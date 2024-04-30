# `com.webotech:state-machine` in depth

Now that [state machine basics](intro.md) are covered, I will explain how the code works. These are
the topics that are covered

* the state machine API
* how to configure a state machine
* what happens at runtime
* examples

## The state machine API

The API for the state machine is in the
[com.webotech.statemachine.api](../src/main/java/com/webotech/statemachine/api) package. Here you
will find the interfaces that define
the [StateMachine](../src/main/java/com/webotech/statemachine/api/StateMachine.java) and related
objects. Everything in the package has comprehensive javadocs to remind you how things work while
you are coding.

## Configuring a state machine

Before the state machine is started, you need to configure it with states, events and transitions.
In [StateMachine](../src/main/java/com/webotech/statemachine/api/StateMachine.java) there are a
number of methods that are clearly documented as being for this purpose. Once configuration is done,
you will need to start the state machine so that it can begin processing events.

The API has been written so that the configuration method names are aligned with natural language.
This is how the [application lifecycle](intro.md#app-lifecycle-example) example would be configured.
Here is the state diagram:

![](media/State_diagram_2.png)

Configuration is done by following these steps (I've omitted defining generic types for
brevity).

```
//define the states
State<> starting = new NamedState<>("STARTING");
State<> running = new NamedState<>("RUNNING");
State<> stopping = new NamedState<>("STOPPING");
State<> stopped = new NamedState<>("STOPPED");

//define the events
StateEvent<> completeEvt = new NamedStateEvent("complete");
StateEvent<> stopEvt = new NamedStateEvent("stop");
StateEvent<> initEvt = new NamedStateEvent("init");
StateEvent<> exitEvt = new NamedStateEvent("exit");

//configure the state machine
StateMachine<> sm = new GenericStateMachine.Builder<>().build();
sm = sm.initialState(starting).receives(completeEvt).itTransitionsTo(running)
    .when(running).receives(stopEvt).itTransitionsTo(stopping)
    .when(stopping).receives(completeEvt).itTransitionsTo(stopped)
    .when(stopped).receives(initEvt).itTransitionsTo(starting)
    .when(stopped).receives(exitEvent).itEnds();
```

At this point the state machine can be started, and it would behave as defined in the diagram but
simple transitions have limited use. Imagine that during the starting state you want the app to
carry out these 3 actions

1. read config from property files
2. use the config to start a connection pool to a database
3. fire a `completeEvt` so that the state machine transitions to `running`

The way this is done is by appending `StateAction`s to a `State`. The API
for [State](../src/main/java/com/webotech/statemachine/api/State.java)
has `appendEntryActions(StateAction...)` for actions you want to execute when the state machine
transitions **to** the state and `appendExitActions(StateAction...)` for actions you want to execute
when the state machine transitions **away** from the state.

In this case you need to add the logic for the actions outlined above in
individual [StateAction](../src/main/java/com/webotech/statemachine/api/StateAction.java)s and then
append them to the `State` as entry actions. When the `starting` state is entered the actions will
execute in the order that they were appended. It is important for them to be executed in a
predictable order, in this case reading the config from property files (action 1) has to happen
before the config is used to connect to a database (action 2).

Finally, the `completeEvt` is fired (action 3) which causes the state machine to transition. Here is
some pseudo code illustrating how the `StateAction`s are added to the `starting` State:

```
//define actions
StateAction<> readConfigAction = sm -> {
 /* Read config from property files and put the config on the StateMachine 
 context using sm.getContext() so it can be accessed by subsequent StateActions */
};
StateAction<> initDbaAction = sm -> {
 /* Get the database connection config using sm.getContext() 
 and start a connection pool to the database */
};
StateAction<> completeAction = sm -> {
 /* Fire a completeEvt */
};
starting.appendEntryActions(readConfigAction, initDbaAction, completeAction);
```

In the above code you will notice the idea of a context for the state machine. It is not necessary
in many cases to use one (in which case define it as `Void`) but in others it is a conventient place
to store data which is for the state machine's use. The context is a free form generic type and is
available to the `StateMachine`, the `StateAction`s and the `StatMachineListener`. It has 2
purposes:

1. allow data to be exchanged between `StateAction`s
2. act as a service locator that provides objects needed by the state machine

## Events in the state machine

### Threading considerations

Typically you configure a `StateMachine` on the app's primary thread, while events originate on I/O
theads like a messaging or RPC thread. `GenericStatMachine` is thread safe so you won't get any
unexpected side effects. Actually the separation that results from using I/O threads helps prevent a
situation where circular execution causes a `StackOverflowError`.

Imagine a 2 state machine where each `State` has a single entry `StateAction` that does nothing
other than fire an event.

![](media/State_diagram_1.png)

In code, the `StateAction`s would look something like this:

```
StateAction<> action1 = sm -> sm.fire(event1);
StateAction<> action2 = sm -> sm.fire(event2);
```

When in `State 1`, the state machine would immediately transition to `State 2` which would cause it
to immediately transition back to `State 1` and so on. Since this is all done on a single thread,
the `StateMachine` enters an unending circular pattern that constantly increases the thread's stack,
ultimately leading to a `StackOverflowError`.

You can avoid this error by using
the [EventManager](../src/main/java/com/webotech/statemachine/EventManager.java) which
has `fireAsync(StateEvent)` and `fireBound(StateEvent)` methods. By using `fireAsync(completeEvt)`
in the `StateAction`s above, the `StateMachine` would continously transition between states without
causing any errors. The `StateAction` code would look something like this:

```
StateAction<> action1 = sm -> {
  EventManager eventManager = sm.getContext().getEventManager();
  eventManager.fireAsync(event1);
};
```

Although this is a contrived example, it does highlight the point that you need to put some thought
into how you fire events in your code. The exact way you do this will be dependant on the app's
threading model. Having a clear understanding of your threading model is important so you can
optimise the app's performance,
the [EventManager](../src/main/java/com/webotech/statemachine/EventManager.java) is one of the tools
that will help you do this.

### Thread safety in `GenericStateMachine`

`GenericStateMachine` is thread safe, so when events are received on multiple threads there will be
no unexpected side effects. The first event that is received will cause a transition to take place
in an atomic way, so that the next event will be processed once the transition is complete (in the
subsequent state). It does this by using `AtomicBoolean`s as a barrier.

In order to minimise object churn at runtime, by default the `AtomicBoolean`s are kept in a simple
object pool and they get recycled during transitions. You can also choose to use your own mechanism
for providing the `AtomicBoolean`s using the `withAtomicBooleanPool()` method on the builder that
takes a supplier and a consumer:

```
//This will use a new AtomicBoolean for every transition which will need to be cleaned up by the GC
StateMachine<> sm = new GenericStateMachine.Builder<>()
    .withAtomicBooleanPool(() -> new::AtomicBoolean, ab -> {}).build();
```

### Duplicate events

If a `StateEvent` is received while the same `StateEvent` is being processed, by
default `GenericStateMachine` will log it as a duplicate and ignore it. In most cases this is
desirable but it could be that your logic relies upon processing _all_ `StateEvent` payloads or you
wish to process the `StateEvent` after the transition has completed. For these cases duplicate
events cannot be ignored and you will need to build the`StateMachine` as follows:

```
//The StateMachine will not drop duplicate events
StateMachine<> sm = new GenericStateMachine.Builder<>().forceProcessDuplicateEvents().build();
```

### Unmapped events

While using the `StateMachine` you may come across situations where a `State` receives
a `StateEvent` that has not been mapped during configuration. By default `GenericStateMachine`
simply logs the details and ignores it, in messaging terms this is equivalent to dropping a message.
However, you may want bespoke behaviour for unmapped events in which case you can build
the `GenericStateMachine` with an unmapped event handler:

```
BiConsumer<StateEvent<>, StateMachine<>> unmappedHandler = ...;
StateMachine<> sm = new GenericStateMachine.Builder<>().setUnmappedEventHander(unmappedHandler);
```

The handler is a `BiConsumer` that calls back with a reference to the `StateEvent` that was received
and the `StateMachine`.

### Event payloads

When events are driven by rich data messages like FIX messages during electronic trading, it
can be convenient for the data in the message to be available to the `StateAction` as you may need
to extract values from it in the processing logic. To facilitate this, the `StateEvent` allows a
generic payload to be set on it which can then be accessed by the `StateAction` when it is being
executed.

Note that in many cases the payload of a `StateEvent` is not needed in which case you can define it
as `Void`.

## Exception handling

When you use a state machine you should have a plan for how you will handle exceptions. Since
the `StateAction`'s will contain your business logic that is where you should focus your efforts.
You may want to simply log all uncaught exceptions and continue with the configured behaviour of the
state machine. Another option would be to configure transitions for error events and fire one when
an uncaught exception occurs.

To help you with your chosen approach, you can use
the [HandleExceptionAction](../src/main/java/com/webotech/statemachine/HandleExceptionAction.java)
which is a `StateAction` designed to separate the concerns for business logic and error code. You
construct it with a `StateAction` that encapsulates the business logic needed for your app, and an
exception handler. The exception handler is called when an exception in the `StateAction` is thrown.
The handler has access to the `StateEvent` that was received when the exception happened,
the `StateMachine` and the `Exception` itself. By wrapping `StateAction`s with
the `HandleExceptionAction`you can handle errors in a single place.

## Tracking the `StateMachine`

If you need to track the `StateMachine` so that you can monitor the current state and any
transitions, you can use a `StateMachineListener`. A single `StateMachineListener` can be set on
the `StateMachine`, at build time here is how to add one:

```
StateMachineListener sml = ...;
StateMachine<> sm = new GenericStateMachine.Builder<>.setStateMachineListener(sml);
```

As you can see from the
API, [StateMachineListener](../src/main/java/com/webotech/statemachine/api/StateMachineListener.java)
is called when the transition to a `State` begins (before any `StateAction`s execute) and when it
ends (after all the `StateActions` complete). There are also 2 implementations
of `StateMachineListener` that are available.

The first implementation
is [LoggingStateMachineListener](../src/main/java/com/webotech/statemachine/LoggingStateMachineListener.java)
which will log at runtime as the `StateMachine` is used. Here is an example of the log output:

```
Starting transition: _UNINITIALISED_ + _immediate_ = STATE-1
Transitioned to STATE-1
Starting transition: STATE-1 + event-1 = STATE-2
Transitioned to STATE-2
Starting transition: STATE-2 + event-1 = _END_
Transitioned to _END_
```

In the output you can see that when the `StateMachine` starts and ends, it transitions with events
and states that you have not explicitly configured. This is because `GenericStateMachine` has
private `State`s and `StateEvent`s which use reserved names that are used for its internal
transitioning logic. You will be able to identify the reserved names in logs as they start and end
with an underscore.

The second implementation
is [MultiConsumerStateMachineListener](../src/main/java/com/webotech/statemachine/MultiConsumerStateMachineListener.java)
which allows you to add/remove other `StateMachineListener`s in a thread safe manner. As an example,
you may want to both log the `StateMachine`s lifecycle and persist it to a database. To do this you
can implement a `StateMachineListener` that is responsible for persisting the lifecycle and combine
it with the `LoggingStateMachineListener` by adding both
to a `MultiConsumerStateMachineListener`. The `MultiConsumerStateMachineListener` is then set on
the `StateMachine`.
