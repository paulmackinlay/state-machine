/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

//TODO something is wrong here - probably should take two StateActions as arguments

/**
 * <p>A {@link StateAction} that routes logic handling to a {@link Consumer} and routes any
 * resultant {@link Exception}s to a separate {@link BiConsumer}, so that business logic and
 * exceptional logic can be implemented separately.</p>
 * <p>This is useful if you want to handle exceptional logic in the same way for all
 * {@link StateAction}s in the {@link StateMachine}</p>
 */
public final class HandleExceptionAction<T, S> implements StateAction<T, S> {

  private final Consumer<StateMachine<T, S>> actionHandler;
  private final BiConsumer<StateMachine<T, S>, Exception> exceptionHandler;

  public HandleExceptionAction(Consumer<StateMachine<T, S>> actionHandler,
      BiConsumer<StateMachine<T, S>, Exception> exceptionHandler) {
    this.actionHandler = actionHandler;
    this.exceptionHandler = exceptionHandler;
  }

  @Override
  public void execute(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine) {
    try {
      this.actionHandler.accept(stateMachine);
    } catch (Exception e) {
      this.exceptionHandler.accept(stateMachine, e);
    }
  }

}
