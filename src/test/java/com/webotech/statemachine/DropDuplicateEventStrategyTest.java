/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DropDuplicateEventStrategyTest {

  private final static ExecutorService executor = Executors.newFixedThreadPool(1);
  private final static StateEvent<Void> event1 = new NamedStateEvent<>("event1");
  private final static State<Void, Void> state1 = new NamedState<>("STATE-1");

  private DropDuplicateEventStrategy<Void, Void> dropDuplicateEventStrategy;
  private GenericStateMachine<Void, Void> stateMachine;
  private State<Void, Void> currentState;

  @BeforeEach
  void setup() {
    stateMachine = mock(GenericStateMachine.class, Mockito.RETURNS_DEEP_STUBS);
    currentState = mock(State.class, Mockito.RETURNS_DEEP_STUBS);
    when(stateMachine.getCurrentState()).thenReturn(currentState);

    dropDuplicateEventStrategy = new DropDuplicateEventStrategy<>(AtomicBoolean::new,
        ab -> {
        });
  }

  @Test
  void shouldTransition() {
    this.dropDuplicateEventStrategy.processEvent(event1, stateMachine, state1);

    //Verify exit/entry actions are called
    verify(currentState, times(1)).onExit(event1, stateMachine);
    verify(currentState, times(1)).onEntry(event1, stateMachine);

    //Verify transition to toState
    verify(stateMachine, times(1)).setCurrentState(state1);

    //Verify notify before/after transition
    verify(stateMachine, times(1)).notifyStateMachineListener(eq(false), any(State.class),
        eq(event1), eq(state1));
    verify(stateMachine, times(1)).notifyStateMachineListener(eq(true), any(State.class),
        eq(event1), eq(state1));
  }

  @Test
  void shouldDropDuplicateEvent() {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicInteger noInvocations = new AtomicInteger(0);
    when(stateMachine.getCurrentState()).thenAnswer(i -> {
      if (noInvocations.getAndIncrement() == 0) {
        boolean success = latch.await(5, TimeUnit.SECONDS);
        if (!success) {
          fail("Latch timed out");
        }
      }
      return currentState;
    });
    executor.execute(
        () -> this.dropDuplicateEventStrategy.processEvent(event1, stateMachine, state1));
    long startMillis = System.currentTimeMillis();
    while (noInvocations.get() == 0 && System.currentTimeMillis() - startMillis < 5000) {
      // niente
    }
    OutputStream logStream = TestingUtil.initLogCaptureStream();
    this.dropDuplicateEventStrategy.processEvent(event1, stateMachine, state1);
    latch.countDown();
    assertEquals("StateEvent [event1] received in state [null] already being processed\n",
        logStream.toString());
    verify(stateMachine, times(1)).setCurrentState(state1);
  }
}
