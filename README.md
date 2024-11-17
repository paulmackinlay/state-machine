# state-machine

This repo contains a [state machine API](src/main/java/com/webotech/statemachine/api)
and a [service API](src/main/java/com/webotech/statemachine/service/api) that have generic, thread
safe implementations written in core java. It also has a

- State machine API with comprehensive javadoc
- Generic state machine implementation
- Service API for building apps/microservices with comprehensive javadoc
- Sevice starter implementation that is backed by a state machine
- Core java: no dependencies other than a logging API
- Thread safe
- Extensively tested
- [Extensively documented](docs/01-intro.md)

You can find how core [concepts of a state machine](docs/01-intro.md) are implemented in the
[documentation](docs/02-implementation.md) or you can dive directly into the code.

The integration tests are useful to show various usecases for `state-machine`. Please note that
tests are designed to run on systems with UNIX style line endings.

## Use state-machine with maven or gradle

This project is
in [maven central](https://central.sonatype.com/artifact/com.webotech/state-machine), to start using
it just add this dependency to your POM.

```xml
<dependency>
    <groupId>com.webotech</groupId>
    <artifactId>state-machine</artifactId>
    <version>1.0.2</version>
</dependency>
```

or this dependency in gradle

```groovy
implementation 'com.webotech:state-machine:1.0.2'
```

**Please use the latest version available in maven central - the version in this page may be old.**

## Quick start example

```java
// Define the states
State<Void, Void> firstState = new NamedState<Void, Void>("FIRST-STATE");
State<Void, Void> secondState = new NamedState<Void, Void>("SECOND-STATE");

// Define the events
StateEvent<Void> continueEvt = new NamedStateEvent<>("continue");

// Define entry/exit actions
firstState.appendEntryActions((ev, sm) -> {
  System.out.println("Start in " + sm.getCurrentState());
  sm.fire(continueEvt);
});
firstState.appendExitActions((ev, sm) -> {
  System.out.println(ev + " caused transition away from " + sm.getCurrentState());
});
secondState.appendEntryActions((ev, sm) -> {
  System.out.println(ev + " caused transition to " + sm.getCurrentState());
});

// Build the state machine
StateMachine<Void, Void> stateMachine = new GenericStateMachine.Builder<Void, Void>().build()
    .initialSate(firstState).receives(continueEvt).itTransitionsTo(secondState)
    .when(secondState).itEnds();

// Start it
stateMachine.start();
```

Running the above code will print this to STDOUT:

```
Start in NamedState[FIRST-STATE]
NamedStateEvent[continue] caused transition away from NamedState[FIRST-STATE]
NamedStateEvent[continue] caused transition to NamedState[SECOND-STATE]
```

The state diagram for this example is:

![](docs/media/Quick_start_diagram.png)

## Quick start service/app

The app:

```java
public class ExampleApp extends AbstractAppService<ExampleAppContext> {

  private ExampleApp(ExampleAppContext appContext) {
    super(appContext);
  }

  public static void main(String[] args) {
    ExampleApp app = new ExampleApp(
        new ExampleAppContext().withSubsystems(List.of(new ExampleSubsystem())));
    try {
      app.start();
    } catch (Exception e) {
      app.error(e);
    }
  }
}
```

The app's context:

```java
public class ExampleAppContext extends AbstractAppContext<ExampleAppContext> {

  ExampleAppContext() {
    super("ExampleApp", new String[0]);
  }
}
```

An example Subsystem:

```java
public class ExampleSubsystem implements Subsystem<ExampleAppContext> {

  private String exampleState;

  @Override
  public void start(ExampleAppContext appContext) {
    exampleState = "subsystem is go!";
  }

  @Override
  public void stop(ExampleAppContext appContext) {
    exampleState = null;
  }
}
```