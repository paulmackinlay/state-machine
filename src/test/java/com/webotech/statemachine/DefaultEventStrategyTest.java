/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefaultEventStrategyTest {

  private final static Logger logger = LogManager.getLogger(DefaultEventStrategyTest.class);
  private final static ExecutorService executor = Executors.newSingleThreadExecutor();
  private static final StateEvent<Void> event1 = new NamedStateEvent<>("event1");
  private static final State<Void, Void> state1 = new NamedState("STATE-1");
  private static final State<Void, Void> state2 = new NamedState("STATE-2");
  private static final State<Void, Void> noopState = new NamedState<>(
      GenericStateMachine.RESERVED_STATE_NAME_NOOP);
  private GenericStateMachine<Void, Void> stateMachine;
  private DefaultEventStrategy<Void, Void> strategy;
  private UnexpectedFlowListener unexpectedFlowListener;

  @BeforeEach
  void setup() {
    stateMachine = mock(GenericStateMachine.class, Mockito.RETURNS_DEEP_STUBS);
    BiConsumer<StateEvent<Void>, StateMachine<Void, Void>> unmappedEventHandler = mock(
        BiConsumer.class);
    Map<State<Void, Void>, Map<StateEvent<Void>, State<Void, Void>>> states = Map.of(state1,
        Map.of(event1, state2), state2, Map.of(event1, noopState));
    unexpectedFlowListener = mock(UnexpectedFlowListener.class);
    strategy = new DefaultEventStrategy<>(unmappedEventHandler, executor,
        unexpectedFlowListener);
    strategy.setStates(states);

    when(stateMachine.getNoopState()).thenReturn(noopState);
    when(stateMachine.getCurrentState()).thenReturn(state1);
  }

  @Test
  void shouldProcessAllQueuedEvents() {
    CountDownLatch latch = new CountDownLatch(1);
    when(stateMachine.getCurrentState()).thenAnswer(i -> {
      if (!latch.await(1, TimeUnit.SECONDS)) {
        fail("Timed out");
      }
      return state1;
    });
    strategy.processEvent(event1, stateMachine);
    strategy.processEvent(event1, stateMachine);
    latch.countDown();
    waitForEventsToProcess();
    verify(stateMachine, times(2)).setCurrentState(any(State.class));
  }

  private void waitForEventsToProcess() {
    try {
      int timeoutMillis = 5000;
      long startMillis = System.currentTimeMillis();
      long durationMillis = 0;
      while (strategy.getEventQueueSize() > 0 && durationMillis < timeoutMillis) {
        TimeUnit.MILLISECONDS.sleep(50);
        durationMillis = System.currentTimeMillis() - startMillis;
      }
      if (durationMillis > timeoutMillis) {
        fail("Timeout out");
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void shouldHandleUncaughtException() {
    IllegalStateException testInduced = new IllegalStateException("test induced");
    when(stateMachine.getCurrentState()).thenThrow(testInduced);
    verifyNoInteractions(unexpectedFlowListener);
    strategy.processEvent(event1, stateMachine);
    waitForEventsToProcess();
    verify(unexpectedFlowListener, times(1)).onExceptionDuringEventProcessing(eq(event1),
        eq(stateMachine), any(Thread.class), eq(testInduced));
  }

}