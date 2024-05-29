/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultUnexpectedFlowListenerTest {

  private DefaultUnexpectedFlowListener<Void, Void> unexpectedFlowListener;
  private StateEvent<Void> event;
  private StateMachine<Void, Void> stateMachine;

  @BeforeEach
  void setUp() {
    unexpectedFlowListener = new DefaultUnexpectedFlowListener<>();
    event = new NamedStateEvent<>("event");
    stateMachine = mock(StateMachine.class);
  }

  @Test
  void shouldHandleException() throws IOException {
    Exception e = new IllegalStateException("test induced");
    Thread thread = new Thread("test-thread");
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      unexpectedFlowListener.onExceptionDuringEventProcessing(event, stateMachine, thread, e);
      assertTrue(logStream.toString().startsWith(
          "Unhandled exception while processing event NamedStateEvent[event] while in state null on thread [test-thread]\n"
              + "java.lang.IllegalStateException: test induced"));
    }
  }

  @Test
  void shouldHandleEventAfterMachineEnd() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      unexpectedFlowListener.onEventAfterMachineEnd(event, stateMachine);
      assertEquals("Event NamedStateEvent[event] received after state machine has ended\n",
          logStream.toString());
    }
  }

  @Test
  void shouldHandleEventBeforeMachineStart() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      unexpectedFlowListener.onEventBeforeMachineStart(event, stateMachine);
      assertEquals("Event NamedStateEvent[event] received before state machine has started\n",
          logStream.toString());
    }
  }
}