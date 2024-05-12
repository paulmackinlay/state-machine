## Events in the state machine

### Event processing

In `GenericStateMachine` events are processed by
an [EventProcessingStrategy](../src/main/java/com/webotech/statemachine/EventProcessingStrategy.java).
There are a couple of implementations and the default implementation is discussed here.

When it comes to processing events, understanding the thread interaction in a `StateMachine` is
important. It is typical for events to originate on I/O threads (like a messaging or RPC thread),
however when you need to fire an event from a `StateAction` it will originate in the current thread.
In order handle programatic flow originating from different threads in a consistent way, by
default `GenericStateMachine` hands off processing to a single thread executor. This has the effect
of

- quickly freeing I/O threads thereby avoid blocking
- avoid thread contention when events originate on the current thread
- guarantees events to be processed in the same order they were received
- being thread-safe

`GenericStateMachine` is thread safe, so when events are received on multiple threads there will be
no unexpected side effects. The first event that is received will cause a transition to take place
in an atomic way, so that the next event will be processed once the transition is complete (in the
subsequent state).

When constructing a `GenericStateMachine`, flexibility has been built in so if you need finer
control of how events are processed there are these options:

- use the builder that constructs the `EventProcessingStrategy` to pass in your
  own `ExecutorService`
- choose a different implementation of `EventProcessingStrategy`
- implement your own `EventProcessingStrategy`

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

### Duplicate events

If a `StateEvent` is received while the same `StateEvent` is being processed, by
default `GenericStateMachine` will log it as a duplicate and ignore it. In most cases this is
desirable but it could be that your logic relies upon processing _all_ `StateEvent` payloads or you
wish to process the `StateEvent` after the transition has completed. For these cases duplicate
events cannot be ignored and you will need to build the`StateMachine` as follows:

```
//The StateMachine will not drop duplicate events
StateMachine<> sm = new GenericStateMachine.Builder<>().processDuplicateEvents().build();
```

[previous page](04-configure.md) --- [next page](06-exceptions.md)
