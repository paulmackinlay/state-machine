/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.webotech.statemachine.HandleExceptionAction.ExceptionHandler;
import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HandleExceptionActionTest {

  private HandleExceptionAction<Void, Void> action;
  private StateAction<Void, Void> stateAction;
  private ExceptionHandler<Void, Void> exceptionHandler;
  private StateEvent<Void> event;

  @BeforeEach
  void setup() {
    stateAction = mock(StateAction.class);
    exceptionHandler = mock(ExceptionHandler.class);
    event = mock(StateEvent.class);
    action = new HandleExceptionAction<>(stateAction, exceptionHandler);
  }

  @Test
  void shouldHandleBizLogic() {
    StateMachine<Void, Void> stateMachine = mock(StateMachine.class);
    action.execute(event, stateMachine);
    verify(stateAction, times(1)).execute(event, stateMachine);
    verifyNoInteractions(exceptionHandler);
  }

  @Test
  void shouldHandleExceptionLogic() {
    StateMachine<Void, Void> stateMachine = mock(StateMachine.class);
    IllegalStateException excp = new IllegalStateException("test induced");
    doThrow(excp).when(stateAction).execute(event, stateMachine);
    action.execute(event, stateMachine);
    verify(stateAction, times(1)).execute(event, stateMachine);
    verify(exceptionHandler, times(1)).onException(event, stateMachine, excp);
  }
}