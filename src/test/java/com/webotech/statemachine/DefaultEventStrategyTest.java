/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.webotech.statemachine.DefaultEventStrategy.Builder;
import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefaultEventStrategyTest {

  private static final StateEvent<Void> event1 = new NamedStateEvent<>("event1");
  private static final StateEvent<Void> event2 = new NamedStateEvent<>("event2");
  private static final State<Void, Void> state1 = new NamedState("STATE-1");
  private static final State<Void, Void> state2 = new NamedState("STATE-2");
  private static final State<Void, Void> noopState = new NamedState<>(
      GenericStateMachine.RESERVED_STATE_NAME_NOOP);
  private GenericStateMachine<Void, Void> stateMachine;
  private DefaultEventStrategy<Void, Void> strategy;
  private BiConsumer<StateEvent<Void>, StateMachine<Void, Void>> unmappedEventHandler;

  @BeforeEach
  void setup() {
    stateMachine = mock(GenericStateMachine.class, Mockito.RETURNS_DEEP_STUBS);
    unmappedEventHandler = mock(BiConsumer.class);
    Map<State<Void, Void>, Map<StateEvent<Void>, State<Void, Void>>> states = Map.of(state1,
        Map.of(event1, state2), state2, Map.of(event1, noopState));
    strategy = new DefaultEventStrategy.Builder<Void, Void>("state-machine",
        states).setUnmappedEventHandler(unmappedEventHandler).build();

    when(stateMachine.getNoopState()).thenReturn(noopState);
    when(stateMachine.getCurrentState()).thenReturn(state1);
    when(stateMachine.getEventQueueSize()).thenAnswer(i -> strategy.getEventQueueSize());
  }

  @Test
  void shouldProcessAllQueuedEvents() {
    CountDownLatch latch = new CountDownLatch(1);
    when(stateMachine.getCurrentState()).thenAnswer(i -> {
      boolean success = latch.await(1, TimeUnit.SECONDS);
      return state1;
    });
    strategy.processEvent(event1, stateMachine);
    strategy.processEvent(event1, stateMachine);
    latch.countDown();
    TestingUtil.waitForAllEventsToProcess(stateMachine);

    verify(stateMachine, times(2)).setCurrentState(any(State.class));
  }

  @Test
  void shouldHandleUncaughtException() throws IOException {
    when(stateMachine.getCurrentState()).thenThrow(new IllegalStateException("test induced"));

    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      strategy.processEvent(event1, stateMachine);
      TestingUtil.waitForAllEventsToProcess(stateMachine);

      String log = logStream.toString();
      assertTrue(log.startsWith("Unhandled exception in thread state-machine-"));
      assertTrue(log.contains("java.lang.IllegalStateException: test induced"));
    }
  }

  @Test
  void shouldHandleUnmappedEvent() {
    strategy.processEvent(event2, stateMachine);
    TestingUtil.waitForAllEventsToProcess(stateMachine);

    verify(unmappedEventHandler, times(1)).accept(event2, stateMachine);
  }

  @Test
  void shouldHandleNoopState() {
    when(stateMachine.getCurrentState()).thenReturn(state2);

    strategy.processEvent(event1, stateMachine);
    TestingUtil.waitForAllEventsToProcess(stateMachine);

    // notify to NOOP state before/after
    verify(stateMachine, times(1)).notifyStateMachineListener(false, state2,
        event1, noopState);
    verify(stateMachine, times(1)).notifyStateMachineListener(true, state2,
        event1, noopState);

    // ensure no state change
    verify(stateMachine, times(0)).setCurrentState(any());
  }

  @Test
  void shouldTransition() {
    State<Void, Void> mockState = mock(State.class);
    when(stateMachine.getCurrentState()).thenReturn(state1, state1, mockState);

    strategy.processEvent(event1, stateMachine);
    TestingUtil.waitForAllEventsToProcess(stateMachine);

    // notification before/after state transition
    verify(stateMachine, times(1)).notifyStateMachineListener(false, state1, event1, state2);
    verify(stateMachine, times(1)).notifyStateMachineListener(false, state1, event1, state2);

    // transition
    verify(stateMachine, times(1)).setCurrentState(state2);

    // entry/exit actions
    verify(mockState, times(1)).onExit(event1, stateMachine);
    verify(mockState, times(1)).onEntry(event1, stateMachine);
  }

  @Test
  void shouldBuildWithUnmappedEventHandler() {
    BiConsumer<StateEvent<Void>, StateMachine<Void, Void>> unmappedEventHander = (se, sm) -> {
    };
    Builder<Void, Void> builder = new DefaultEventStrategy.Builder<Void, Void>("state-machine",
        new HashMap<>()).setUnmappedEventHandler(unmappedEventHander);
    assertSame(unmappedEventHander, builder.getUnmappedEventHandler());
  }

  @Test
  void shouldBuildWithExecutor() {
    ExecutorService executor = mock(ExecutorService.class);
    Builder<Void, Void> builder = new DefaultEventStrategy.Builder<Void, Void>("state-machine",
        new HashMap<>()).setExecutor(executor);
    assertSame(executor, builder.getExecutor());
  }
}