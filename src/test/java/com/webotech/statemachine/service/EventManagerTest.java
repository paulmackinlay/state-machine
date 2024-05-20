/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.webotech.statemachine.NamedStateEvent;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EventManagerTest {

  private EventManager<Void, Void> eventManager;
  private StateMachine<Void, Void> stateMachine;
  private ExecutorService executorService;

  @BeforeEach
  void setup() {
    stateMachine = mock(StateMachine.class);
    executorService = mock(ExecutorService.class);
    this.eventManager = new EventManager<>(stateMachine, executorService);
  }

  @Test
  void shouldFireEventAsync() {
    StateEvent<Void> event = new NamedStateEvent<>("event");
    this.eventManager.fireAsync(event);
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(executorService, times(1)).execute(any(Runnable.class));
    verify(executorService, times(1)).execute(runnableCaptor.capture());
    runnableCaptor.getValue().run();
    verify(stateMachine, times(1)).fire(event);
  }

  @Test
  void shouldFireEventBound() {
    StateEvent<Void> event = new NamedStateEvent<>("event");
    this.eventManager.fireBound(event);
    verify(stateMachine, times(1)).fire(event);
  }
}