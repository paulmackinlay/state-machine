package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoggingStateMachineListenerTest {

  private static final String NAME = LoggingStateMachineListenerTest.class.getSimpleName();
  private static final State<Void, Void> fromState = new NamedState<>("STATE-1");
  private static final State<Void, Void> toState = new NamedState<>("STATE-2");
  private static final StateEvent<Void> event = new NamedStateEvent<>("an-event");
  private LoggingStateMachineListener<Void, Void> loggingListener;
  private LoggingStateMachineListener<Void, Void> namedLoggingListener;

  @BeforeEach
  void setup() {
    this.loggingListener = new LoggingStateMachineListener<>();
    this.namedLoggingListener = new LoggingStateMachineListener<>(NAME);
  }

  @Test
  void shouldLogTransitions() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      loggingListener.onStateChangeBegin(fromState, event, toState);
      loggingListener.onStateChangeEnd(fromState, event, toState);
      String log = logStream.toString();
      assertEquals(
          "Starting transition: " + fromState.getName() + " + " + event.getName() + " = "
              + toState.getName()
              + "\n"
              + "Transitioned to " + toState.getName() + "\n", log);
    }
  }

  @Test
  void shouldLogNamedTransitions() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      namedLoggingListener.onStateChangeBegin(fromState, event, toState);
      namedLoggingListener.onStateChangeEnd(fromState, event, toState);
      String log = logStream.toString();
      assertEquals(
          "Starting " + NAME + " transition: " + fromState.getName() + " + " + event.getName()
              + " = " + toState.getName() + "\n"
              + NAME + " transitioned to " + toState.getName() + "\n", log);
    }
  }
}