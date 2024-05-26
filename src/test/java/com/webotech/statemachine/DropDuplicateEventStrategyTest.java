/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DropDuplicateEventStrategyTest {

  private final static ExecutorService executor = Executors.newFixedThreadPool(1);
  private static final StateEvent<Void> event1 = new NamedStateEvent<>("event1");
  private static final State<Void, Void> state1 = new NamedState("STATE-1");
  private static final State<Void, Void> state2 = new NamedState("STATE-2");
  private static final State<Void, Void> noopState = new NamedState<>(
      GenericStateMachine.RESERVED_STATE_NAME_NOOP);
  private static final State<Void, Void> endState = new NamedState<>(
      GenericStateMachine.RESERVED_STATE_NAME_END);
  private static final StateEvent<Void> immediateEvent = new NamedStateEvent<>(
      GenericStateMachine.RESERVED_STATE_EVENT_NAME_IMMEDIATE);
  private final static int timeoutSecs = 5;
  private GenericStateMachine<Void, Void> stateMachine;
  private DropDuplicateEventStrategy<Void, Void> strategy;
  private BiConsumer<StateEvent<Void>, StateMachine<Void, Void>> unmappedEventHandler;

  @BeforeEach
  void setup() {
    stateMachine = mock(GenericStateMachine.class, Mockito.RETURNS_DEEP_STUBS);
    unmappedEventHandler = mock(BiConsumer.class);
    Map<State<Void, Void>, Map<StateEvent<Void>, State<Void, Void>>> states = Map.of(state1,
        Map.of(event1, state2), state2, Map.of(event1, noopState));
    strategy = new DropDuplicateEventStrategy.Builder<Void, Void>(executor).setUnmappedEventHandler(
        unmappedEventHandler).build();
    strategy.setStates(states);

    when(stateMachine.getNoopState()).thenReturn(noopState);
    when(stateMachine.getCurrentState()).thenReturn(state1);
    when(stateMachine.getEndState()).thenReturn(endState);
    when(stateMachine.getImmediateEvent()).thenReturn(immediateEvent);
    when(stateMachine.getEventQueueSize()).thenAnswer(i -> strategy.getEventQueueSize());
  }

  @Test
  void shouldDropDuplicateEvent() {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean isFirstProcess = new AtomicBoolean();
    when(stateMachine.getCurrentState()).thenAnswer(i -> {
      if (isFirstProcess.compareAndSet(false, true)) {
        boolean success = latch.await(timeoutSecs, TimeUnit.SECONDS);
        if (!success) {
          fail("Latch timed out");
        }
      }
      return state1;
    });
    executor.execute(
        () -> this.strategy.processEvent(event1, stateMachine));
    long startMillis = System.currentTimeMillis();
    while (!isFirstProcess.get() && System.currentTimeMillis() - startMillis < (timeoutSecs
        * 1000)) {
      // niente
    }
    OutputStream logStream = TestingUtil.initLogCaptureStream();
    this.strategy.processEvent(event1, stateMachine);
    latch.countDown();
    TestingUtil.waitForAllEventsToProcess(stateMachine);
    assertEquals("Event [NamedStateEvent[event1]] already in queue, will drop it\n",
        logStream.toString());
    verify(stateMachine, times(1)).setCurrentState(state2);
  }
}
