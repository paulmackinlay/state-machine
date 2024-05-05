/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.webotech.statemachine.DropDuplicateEventStrategy.Builder;
import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DropDuplicateEventStrategyTest {

  private final static ExecutorService executor = Executors.newFixedThreadPool(1);
  private final static StateEvent<Void> event1 = new NamedStateEvent<>("event1");
  private final static StateEvent<Void> event2 = new NamedStateEvent<>("event2");
  private final static State<Void, Void> state1 = new NamedState<>("STATE-1");
  private final static State<Void, Void> state2 = new NamedState<>("STATE-2");
  private final static int timeoutSecs = 5;

  private DropDuplicateEventStrategy<Void, Void> dropDuplicateEventStrategy;
  private GenericStateMachine<Void, Void> stateMachine;
  private State<Void, Void> currentState;
  private BiConsumer<StateEvent<Void>, StateMachine<Void, Void>> unmappedEventHandler;

  @BeforeEach
  void setup() throws InterruptedException {
    stateMachine = mock(GenericStateMachine.class);
    currentState = mock(State.class, Mockito.RETURNS_DEEP_STUBS);
    unmappedEventHandler = mock(BiConsumer.class);
    when(stateMachine.getNoopState()).thenReturn(new NamedState<>("_NNOP_"));
    when(stateMachine.getCurrentState()).thenReturn(currentState);
    Map<State<Void, Void>, Map<StateEvent<Void>, State<Void, Void>>> states = new HashMap<>();
    states.put(currentState, Map.of(event1, currentState));
    dropDuplicateEventStrategy = new DropDuplicateEventStrategy.Builder<>(states,
        unmappedEventHandler).build();
    when(stateMachine.getEventQueueSize()).thenReturn(
        1, dropDuplicateEventStrategy.getEventQueueSize());
  }

  @Test
  void shouldTransition() {
    this.dropDuplicateEventStrategy.processEvent(event1, stateMachine);
    TestingUtil.waitForAllEventsToProcess(stateMachine);

    //Verify exit/entry actions are called
    verify(currentState, times(1)).onExit(event1, stateMachine);
    verify(currentState, times(1)).onEntry(event1, stateMachine);

    //Verify transition to toState
    verify(stateMachine, times(1)).setCurrentState(currentState);

    //Verify notify before/after transition
    verify(stateMachine, times(1)).notifyStateMachineListener(eq(false), any(State.class),
        eq(event1), any(State.class));
    verify(stateMachine, times(1)).notifyStateMachineListener(eq(true), any(State.class),
        eq(event1), any(State.class));
  }

  @Test
  @Disabled
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
      return currentState;
    });
    executor.execute(
        () -> this.dropDuplicateEventStrategy.processEvent(event1, stateMachine));
    long startMillis = System.currentTimeMillis();
    while (!isFirstProcess.get() && System.currentTimeMillis() - startMillis < (timeoutSecs
        * 1000)) {
      // niente
    }
    OutputStream logStream = TestingUtil.initLogCaptureStream();
    this.dropDuplicateEventStrategy.processEvent(event1, stateMachine);
    TestingUtil.waitForAllEventsToProcess(stateMachine);
    latch.countDown();
    assertEquals("StateEvent [event1] received in state [null] already being processed\n",
        logStream.toString());
    verify(stateMachine, times(1)).setCurrentState(state1);
  }

  @Test
  @Disabled
  void shouldHandleInterleavedEvents() {
    /**
     * 1st event is slow to complete
     * 2nd event finishes before 1st event
     */
    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean isFirstProcess = new AtomicBoolean();
    when(stateMachine.getCurrentState()).thenAnswer(i -> {
      if (isFirstProcess.compareAndSet(false, true)) {
        boolean success = latch.await(timeoutSecs, TimeUnit.SECONDS);
        if (!success) {
          fail("Latch timed out");
        }
      }
      return currentState;
    });
    executor.execute(
        () -> this.dropDuplicateEventStrategy.processEvent(event1, stateMachine));
    long startMillis = System.currentTimeMillis();
    while (!isFirstProcess.get() && System.currentTimeMillis() - startMillis < (timeoutSecs
        * 1000)) {
      // niente
    }
    OutputStream logStream = TestingUtil.initLogCaptureStream();
    this.dropDuplicateEventStrategy.processEvent(event2, stateMachine);
    TestingUtil.waitForAllEventsToProcess(stateMachine);
    latch.countDown();

    assertEquals("StateEvent [event1] received in state [null] already being processed\n",
        logStream.toString());
    verify(stateMachine, times(1)).setCurrentState(state1);
  }

  @Test
  void shouldBuildWithPool() {
    Supplier<AtomicBoolean> poolSupplier = AtomicBoolean::new;
    Consumer<AtomicBoolean> poolConsumer = a -> {
    };
    Builder<Void, Void> builder = new DropDuplicateEventStrategy.Builder<Void, Void>(new HashMap(),
        null).withAtomicBooleanPool(poolSupplier, poolConsumer);
    assertSame(poolSupplier, builder.getAtomicBooleanSupplier());
    assertSame(poolConsumer, builder.getAtomicBooleanConsumer());
  }

}
