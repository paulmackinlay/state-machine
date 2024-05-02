# state-machine

This contains a [state machine API](src/main/java/com/webotech/statemachine/api) with a generic
[implementation](src/main/java/com/webotech/statemachine/GenericStateMachine.java) written in core
java that is thread safe.

- State machine API with comprehensive javadoc
- Generic implementation
- Core java: no dependencies (other than a logging API)
- Thread safe

You can find how core [concepts of a state machine](docs/01-intro.md) are implemented in the
[documentation](docs/02-implementation.md) or you can dive directly into the code.

**Note** although the current version is being used in production on a number of projects, the logic
is still undergoing significant changes. You may want to wait for a major release version (1.0.0)
when it will become more stable.
