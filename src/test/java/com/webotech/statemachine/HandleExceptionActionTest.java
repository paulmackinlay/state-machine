/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HandleExceptionActionTest {

  private HandleExceptionAction<Void, Void> action;
  private Consumer<StateMachine<Void, Void>> actionHandler;
  private BiConsumer<StateMachine<Void, Void>, Exception> exceptionHandler;
  private StateEvent<Void> event;

  @BeforeEach
  void setup() {
    actionHandler = mock(Consumer.class);
    exceptionHandler = mock(BiConsumer.class);
    event = mock(StateEvent.class);
    action = new HandleExceptionAction<>(actionHandler, exceptionHandler);
  }

  @Test
  void shouldHandleBizLogic() {
    StateMachine<Void, Void> stateMachine = mock(StateMachine.class);
    action.execute(event, stateMachine);
    verify(actionHandler, times(1)).accept(stateMachine);
    verifyNoInteractions(exceptionHandler);
  }

  @Test
  void shouldHandleExceptionLogic() {
    StateMachine<Void, Void> stateMachine = mock(StateMachine.class);
    IllegalStateException excp = new IllegalStateException("test induced");
    doThrow(excp).when(actionHandler).accept(stateMachine);
    action.execute(event, stateMachine);
    verify(actionHandler, times(1)).accept(stateMachine);
    verify(exceptionHandler, times(1)).accept(stateMachine, excp);
  }
}