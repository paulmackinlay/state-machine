## Tracking the state machine

If you need to track the `StateMachine` so that you can monitor the current state and any
transitions, you can use a `StateMachineListener`. A single `StateMachineListener` can be set on
the `StateMachine` at build time, here is how to add one:

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
which allows you to add/remove multiple other `StateMachineListener`s in a thread safe manner. As an
example, you may want to both log the `StateMachine`s lifecycle and persist it to a database. To do
this you can implement a `StateMachineListener` that is responsible for persisting the lifecycle and
combine it with the `LoggingStateMachineListener` by adding both to
a `MultiConsumerStateMachineListener`. The `MultiConsumerStateMachineListener` is then set on
the `StateMachine`.

[previous page](06-exceptions.md)
