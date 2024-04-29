/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;

/**
 * <p>A {@link StateAction} that passes though to an internal {@link StateAction} at execution time
 * while catching any uncaught {@link Exception}s and routing them to an {@link ExceptionHandler}.
 * This allows business logic and exceptional logic can be implemented separately.</p>
 * <p>It is useful if you want to handle exceptional logic in the same way for all
 * {@link StateAction}s in the {@link StateMachine}</p>
 */
public final class HandleExceptionAction<T, S> implements StateAction<T, S> {

  private final StateAction<T, S> stateAction;
  private final ExceptionHandler<T, S> exceptionHandler;

  public HandleExceptionAction(StateAction<T, S> stateAction,
      ExceptionHandler<T, S> exceptionHandler) {
    this.stateAction = stateAction;
    this.exceptionHandler = exceptionHandler;
  }

  @Override
  public void execute(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine) {
    try {
      stateAction.execute(stateEvent, stateMachine);
    } catch (Exception e) {
      exceptionHandler.onException(stateEvent, stateMachine, e);
    }
  }

  /**
   * Allows uncaught exceptions in {@link StateAction}s to be handled.
   */
  public interface ExceptionHandler<T, S> {

    /**
     * Called when an uncaught {@link Exception} happens in the {@link StateAction}
     */
    void onException(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine, Exception e);
  }

}
