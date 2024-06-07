/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.webotech.statemachine.api.StateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoundQueueEventStrategyTest {

  private static final int MAX_SIZE = 2;
  private BoundQueueEventStrategy<Void, Void> strategy;
  private DefaultEventStrategy<Void, Void> defaultEventStrategy;
  private UnexpectedFlowListener<Void, Void> unexpectedFlowListener;

  @BeforeEach
  void setup() {
    defaultEventStrategy = mock(DefaultEventStrategy.class);
    unexpectedFlowListener = mock(UnexpectedFlowListener.class);
    when(defaultEventStrategy.getUnexpectedFlowListener()).thenReturn(unexpectedFlowListener);
    strategy = new BoundQueueEventStrategy<>(defaultEventStrategy, MAX_SIZE);
  }

  @Test
  void shouldDropMaxedOutEvents() {
    StateEvent<Void> event1 = mock(StateEvent.class);
    GenericStateMachine<Void, Void> stateMachine = mock(GenericStateMachine.class);
    when(defaultEventStrategy.getEventQueueSize()).thenReturn(MAX_SIZE);
    verifyNoInteractions(unexpectedFlowListener);
    strategy.processEvent(event1, stateMachine);
    verify(unexpectedFlowListener, times(1)).onExceptionDuringEventProcessing(eq(event1),
        eq(stateMachine), eq(Thread.currentThread()), any(IllegalStateException.class));
    verify(defaultEventStrategy, times(0)).processEvent(any(StateEvent.class),
        any(GenericStateMachine.class));
  }

  @Test
  void shouldProcessEvent() {
    StateEvent<Void> event1 = mock(StateEvent.class);
    GenericStateMachine<Void, Void> stateMachine = mock(GenericStateMachine.class);
    when(defaultEventStrategy.getEventQueueSize()).thenReturn(MAX_SIZE - 1);
    strategy.processEvent(event1, stateMachine);
    verifyNoInteractions(unexpectedFlowListener);
    verify(defaultEventStrategy, times(1)).processEvent(event1, stateMachine);
  }
}