/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateMachineIntegrationTest {

  public static final int TIMEOUT_MILLIS = 5000;
  private State<Void, Void> state1;
  private State<Void, Void> state2;
  private StateEvent<Void> event1;
  private StateEvent<Void> event2;

  @BeforeEach
  void setup() {
    state1 = new NamedState<>("STATE-1");
    state2 = new NamedState<>("STATE-2");
    event1 = new NamedStateEvent<>("event-1");
    event2 = new NamedStateEvent<>("event-2");
  }

  @Test
  void shouldUseTheLoggingStateMachineListener() throws IOException {
    StateMachineListener<Void, Void> loggingStateMachineListener = new LoggingStateMachineListener<>();
    StateMachine<Void, Void> stateMachine = new GenericStateMachine.Builder<Void, Void>().setStateMachineListener(
        loggingStateMachineListener).build();
    stateMachine.initialSate(state1).receives(event1).itTransitionsTo(state2).when(state2)
        .receives(event1).itEnds();

    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      assertFalse(stateMachine.isEnded());
      assertFalse(stateMachine.isStarted());
      stateMachine.start();
      assertTrue(stateMachine.isStarted());
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
      assertTrue(stateMachine.isEnded());
      assertTrue(stateMachine.isStarted());
    }
  }

  @Test
  void shouldUseNamedLoggingStateMachineListener() throws IOException {
    String name = "a-test-state-machine";
    StateMachineListener<Void, Void> loggingStateMachineListener = new LoggingStateMachineListener<>(
        name);
    StateMachine<Void, Void> stateMachine = new GenericStateMachine.Builder<Void, Void>().setStateMachineListener(
        loggingStateMachineListener).setName(name).build();
    stateMachine.initialSate(state1).receives(event1).itTransitionsTo(state2).when(state2)
        .receives(event1).itEnds();

    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      assertFalse(stateMachine.isEnded());
      assertFalse(stateMachine.isStarted());
      stateMachine.start();
      assertTrue(stateMachine.isStarted());
      stateMachine.fire(event1);
      stateMachine.fire(event1);
      TestingUtil.waitForAllEventsToProcess(stateMachine);
      String log = logStream.toString();
      assertEquals(
          "Starting a-test-state-machine transition: _UNINITIALISED_ + _immediate_ = STATE-1\n"
              + "a-test-state-machine transitioned to STATE-1\n"
              + "Starting a-test-state-machine transition: STATE-1 + event-1 = STATE-2\n"
              + "a-test-state-machine transitioned to STATE-2\n"
              + "Starting a-test-state-machine transition: STATE-2 + event-1 = _END_\n"
              + "a-test-state-machine transitioned to _END_\n", log);
      assertTrue(stateMachine.isEnded());
      assertTrue(stateMachine.isStarted());
    }
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

  /*
  TODO
  DONE 1. state machine that ends in a state
  DONE 2. state machine that ends on event
  3. state machine that goes in circles and then ends on event
  DONE 4. state machine that logs
  DONE 5. state machine that notifies
  6. state machine that can be used to start an app
  7. context based
  8. events with payload
  9. fire many events concurrently
  10. state machine that starts in a specific state
  11. Unmapped event handlers
  12. no transition configuration
   */
  @Test
  void shouldEndInAState() throws IOException {
    StateMachine<Void, Void> stateMachine = new GenericStateMachine.Builder<Void, Void>().setStateMachineListener(
        new LoggingStateMachineListener<>()).build();
    stateMachine.initialSate(state1).receives(event1).itTransitionsTo(state2).when(state2).itEnds();
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      assertFalse(stateMachine.isEnded());
      assertFalse(stateMachine.isStarted());
      stateMachine.start();
      assertTrue(stateMachine.isStarted());
      stateMachine.fire(event1);
      TestingUtil.waitForMachineToEnd(stateMachine);
      assertEquals("Starting transition: _UNINITIALISED_ + _immediate_ = STATE-1\n"
          + "Transitioned to STATE-1\n"
          + "Starting transition: STATE-1 + event-1 = STATE-2\n"
          + "Transitioned to STATE-2\n", logStream.toString());
      assertTrue(stateMachine.isEnded());
      assertTrue(stateMachine.isStarted());
    }
  }

  @Test
  void shouldNotifyStateChangesAndLogThem() throws IOException {
    List<List<Object>> beginUpdates = new ArrayList<>();
    List<List<Object>> endUpdates = new ArrayList<>();
    StateMachineListener<Void, Void> listener = new StateMachineListener<Void, Void>() {
      @Override
      public void onStateChangeBegin(State<Void, Void> fromState, StateEvent<Void> event,
          State<Void, Void> toState) {
        beginUpdates.add(List.of(fromState, event, toState));
      }

      @Override
      public void onStateChangeEnd(State<Void, Void> fromState, StateEvent<Void> event,
          State<Void, Void> toState) {
        endUpdates.add(List.of(fromState, event, toState));
      }
    };
    MultiConsumerStateMachineListener<Void, Void> multiListener = new MultiConsumerStateMachineListener<>();
    multiListener.add(new LoggingStateMachineListener<>());
    multiListener.add(listener);
    StateMachine<Void, Void> stateMachine = new GenericStateMachine.Builder<Void, Void>().setStateMachineListener(
        multiListener).build();
    stateMachine.initialSate(state1).receives(event1).itTransitionsTo(state2).when(state2).itEnds();
    String log;
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      stateMachine.start();
      stateMachine.fire(event1);
      log = logStream.toString();
    }
    TestingUtil.waitForMachineToEnd(stateMachine);

    assertEquals(2, beginUpdates.size());
    assertNotifiedRow(beginUpdates.get(0), "_UNINITIALISED_", "_immediate_", state1.getName());
    assertNotifiedRow(beginUpdates.get(1), state1.getName(), event1.getName(), state2.getName());

    assertEquals(2, endUpdates.size());
    assertNotifiedRow(endUpdates.get(0), "_UNINITIALISED_", "_immediate_", state1.getName());
    assertNotifiedRow(endUpdates.get(1), state1.getName(), event1.getName(), state2.getName());

    assertEquals("Starting transition: _UNINITIALISED_ + _immediate_ = STATE-1\n"
        + "Transitioned to STATE-1\n", log);
  }

  @Test
  void shouldStopPerpetuallyActiveStateMachine() {
    State<AtomicInteger, Void> aState1 = new NamedState<>("A-STATE-1");
    State<AtomicInteger, Void> aState2 = new NamedState<>("A-STATE-2");

    StateMachine<AtomicInteger, Void> paStateMachine = new GenericStateMachine.Builder<AtomicInteger, Void>().setContext(
        new AtomicInteger()).build();
    paStateMachine.initialSate(aState1).receives(event1).itTransitionsTo(aState2)
        .when(aState2).receives(event1).itTransitionsTo(aState1);
    StateAction<AtomicInteger, Void> incrementCountAction = (ev, sm) -> {
      sm.getContext().incrementAndGet();
      sm.fire(event1);
    };
    aState1.appendEntryActions(incrementCountAction);
    aState2.appendEntryActions(incrementCountAction);
    int minTransitions = 50;

    paStateMachine.start();
    long initMillis = System.currentTimeMillis();
    long maxMillis = 0;
    while (paStateMachine.getContext().get() <= minTransitions && maxMillis < TIMEOUT_MILLIS) {
      maxMillis = System.currentTimeMillis() - initMillis;
    }
    paStateMachine.stop();
    if (maxMillis > TIMEOUT_MILLIS) {
      fail("Timed out");
    }
    TestingUtil.waitForMachineToEnd(paStateMachine);
    assertTrue(paStateMachine.getContext().get() > minTransitions,
        "Not enough transitions took place");
    assertTrue(paStateMachine.isEnded(), "State machine ended");
  }

  @Test
  void shouldStopPerpetuallyActiveStateMachineWithEvent() {
    State<AtomicInteger, Void> aState1 = new NamedState<>("A-STATE-1");
    State<AtomicInteger, Void> aState2 = new NamedState<>("A-STATE-2");

    StateMachine<AtomicInteger, Void> paStateMachine = new GenericStateMachine.Builder<AtomicInteger, Void>().setContext(
        new AtomicInteger()).build();
    paStateMachine.initialSate(aState1).receives(event1).itTransitionsTo(aState2)
        .when(aState1).receives(event2).itEnds()
        .when(aState2).receives(event1).itTransitionsTo(aState1)
        .when(aState2).receives(event2).itEnds();
    StateAction<AtomicInteger, Void> incrementCountAction = (ev, sm) -> {
      sm.getContext().incrementAndGet();
      sm.fire(event1);
    };
    aState1.appendEntryActions(incrementCountAction);
    aState2.appendEntryActions(incrementCountAction);

    int minTransitions = 50;
    paStateMachine.start();
    long initMillis = System.currentTimeMillis();
    long maxMillis = 0;
    while (paStateMachine.getContext().get() <= minTransitions && maxMillis < TIMEOUT_MILLIS) {
      maxMillis = System.currentTimeMillis() - initMillis;
    }
    paStateMachine.fire(event2);
    if (maxMillis > TIMEOUT_MILLIS) {
      fail("Timed out");
    }
    TestingUtil.waitForMachineToEnd(paStateMachine);
    assertTrue(paStateMachine.getContext().get() > minTransitions,
        "Not enough transitions took place");
    assertTrue(paStateMachine.isEnded(), "State machine ended");
  }

  private void assertNotifiedRow(List<Object> row, String fromStateName, String eventName,
      String toStateName) {
    assertEquals(new NamedState<Void, Void>(fromStateName), row.get(0));
    assertEquals(new NamedStateEvent<Void>(eventName), row.get(1));
    assertEquals(new NamedState<Void, Void>(toStateName), row.get(2));
  }
}