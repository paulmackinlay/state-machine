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
the `StateMachine` and the `Exception` itself. By wrapping all `StateAction`s with
the `HandleExceptionAction`you can handle errors in a single place.

[previous page](05-events.md) --- [next page](07-tracking.md)
