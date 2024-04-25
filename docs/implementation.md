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
you will need to start the state machine so that it can start processing events.

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
StateMachine<> sm = (new GenericStateMachine.Builder<>()).build();
sm = sm.initialState(starting).receives(completeEvt).itTransitionsTo(running)
     when(running).receives(stopEvt).itTransitionsTo(stopping)
     when(stopping).receives(completeEvt).itTransitionsTo(stopped)
     when(stopped).receives(initEvt).itTransitionsTo(starting)
     when(stopped).receives(exitEvent).itEnds();
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
transitions to a state to and `appendExitActions(StateAction...)` for actions you want to execute
when the state machine transitions away from a state.
