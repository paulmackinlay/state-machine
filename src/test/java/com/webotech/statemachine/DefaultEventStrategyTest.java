/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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
      if (!latch.await(1, TimeUnit.SECONDS)) {
        fail("Timed out");
      }
      return state1;
    });
    strategy.processEvent(event1, stateMachine);
    strategy.processEvent(event1, stateMachine);
    latch.countDown();
    TestingUtil.waitForAllEventsToProcess(stateMachine);

    verify(stateMachine, times(2)).setCurrentState(any(State.class));
  }

  @Test
  void shouldHandleUncaughtException() throws IOException, InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    when(stateMachine.getCurrentState()).thenAnswer(i -> {
      try {
        throw new IllegalStateException("test induced");
      } finally {
        latch.countDown();
      }
    });

    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      strategy.processEvent(event1, stateMachine);
      if (!latch.await(1, TimeUnit.SECONDS)) {
        fail("Timed out");
      }
      TestingUtil.waitForAllEventsToProcess(stateMachine);

      String log = logStream.toString();
      assertTrue(log.startsWith("Unhandled exception in thread state-machine-"));
      assertTrue(log.contains("java.lang.IllegalStateException: test induced"));
    }
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