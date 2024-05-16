/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateMachineIntegrationTest {

  private State<Void, Void> state1;
  private State<Void, Void> state2;
  private StateEvent<Void> event1;

  @BeforeEach
  void setup() {
    state1 = new NamedState<>("STATE-1");
    state2 = new NamedState<>("STATE-2");
    event1 = new NamedStateEvent<>("event-1");
  }

  @Test
  void shouldUseTheLoggingStateMachineListener() {
    StateMachineListener<Void, Void> loggingStateMachineListener = new LoggingStateMachineListener<>();
    StateMachine<Void, Void> stateMachine = new GenericStateMachine.Builder<Void, Void>().setStateMachineListener(
        loggingStateMachineListener).build();
    stateMachine.initialSate(state1).receives(event1).itTransitionsTo(state2).when(state2)
        .receives(event1).itEnds();

    OutputStream logStream = TestingUtil.initLogCaptureStream();
    stateMachine.start();
    stateMachine.fire(event1);
    stateMachine.fire(event1);
    TestingUtil.waitForAllEventsToProcess(stateMachine);
    String log = logStream.toString();
    assertEquals("Starting transition: _UNINITIALISED_ + _immediate_ = STATE-1\n"
        + "Transitioned to STATE-1\n"
        + "Starting transition: STATE-1 + event-1 = STATE-2\n"
        + "Transitioned to STATE-2\n"
        + "Starting transition: STATE-2 + event-1 = _END_\n"
        + "Transitioned to _END_\n", log);
  }

  @Test
  void shouldRunQuickstartExample() throws IOException {

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

    try (OutputStream stdOutStream = TestingUtil.initStdOutStream()) {
      // Start it
      stateMachine.start();

      TestingUtil.waitForAllEventsToProcess(stateMachine);
      String stdOut = stdOutStream.toString();
      assertEquals("Start in NamedState[FIRST-STATE]\n"
          + "NamedStateEvent[continue] caused transition away from NamedState[FIRST-STATE]\n"
          + "NamedStateEvent[continue] caused transition to NamedState[SECOND-STATE]\n", stdOut);
    }
  }
}