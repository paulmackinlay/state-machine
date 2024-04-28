/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.function.BiConsumer;

/**
 * <p>A {@link StateAction} that passes though to an internal {@link StateAction} at execution time
 * while catching any unhandled {@link Exception} and routing them to a {@link BiConsumer}. This
 * business logic and exceptional logic can be implemented separately.</p>
 * <p>This is useful if you want to handle exceptional logic in the same way for all
 * {@link StateAction}s in the {@link StateMachine}</p>
 */
public final class HandleExceptionAction<T, S> implements StateAction<T, S> {

  private final StateAction<T, S> stateAction;
  private final BiConsumer<StateMachine<T, S>, Exception> exceptionHandler;

  public HandleExceptionAction(StateAction<T, S> stateAction,
      BiConsumer<StateMachine<T, S>, Exception> exceptionHandler) {
    this.stateAction = stateAction;
    this.exceptionHandler = exceptionHandler;
  }

  @Override
  public void execute(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine) {
    try {
      this.stateAction.execute(stateEvent, stateMachine);
    } catch (Exception e) {
      this.exceptionHandler.accept(stateMachine, e);
    }
  }

}
