/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;
import java.io.OutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateMachineIntegrationTest {

  private State<Void, Void> state1;
  private State<Void, Void> state2;
  private StateEvent<Void> event1;

  @BeforeEach
  void setuo() {
    state1 = new NamedState<>("STATE-1");
    state2 = new NamedState<>("STATE-2");
    event1 = new NamedStateEvent<>("event-1");
  }


  //TODO create integration tests
  @Test
  void shouldUseTheLoggingStateMachineListener() {
    StateMachineListener<Void, Void> loggingStateMachineListener = new LoggingStateMachineListener<>();
    StateMachine<Void, Void> sm = new GenericStateMachine.Builder<Void, Void>().setStateMachineListener(
            loggingStateMachineListener)
        .build();
    sm.initialSate(state1).receives(event1).itTransitionsTo(state2).when(state2).receives(event1)
        .itEnds();

    OutputStream logStream = TestingUtil.initLogCaptureStream();
    sm.start();
    sm.fire(event1);
    sm.fire(event1);
    String log = logStream.toString();
    //TODO how about logging when it starts?
    assertEquals("Starting transition: STATE-1 + event-1 = STATE-2\n"
        + "Transitioned to STATE-2\n"
        + "Starting transition: STATE-2 + event-1 = _END_\n"
        + "Transitioned to _END_\n", log);
  }
}