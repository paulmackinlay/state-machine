package com.webotech.statemachine;

import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateMachine;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * <p>A {@link StateAction} that routes logic handling to a {@link Consumer} and routes any
 * resultant {@link Exception}s to a separate {@link BiConsumer}, so that business logic and
 * exceptional logic can be implemented separately.</p>
 * <p>This is useful if you want to handle exceptional logic in the same way for all
 * {@link StateAction}s in the {@link StateMachine}</p>
 */
public final class HandleExceptionAction<T> implements StateAction<T> {

  private final Consumer<StateMachine<T>> actionHandler;
  private final BiConsumer<StateMachine<T>, Exception> exceptionHandler;

  public HandleExceptionAction(Consumer<StateMachine<T>> actionHandler,
      BiConsumer<StateMachine<T>, Exception> exceptionHandler) {
    this.actionHandler = actionHandler;
    this.exceptionHandler = exceptionHandler;
  }

  @Override
  public void execute(StateMachine<T> stateMachine) {
    try {
      this.actionHandler.accept(stateMachine);
    } catch (Exception e) {
      this.exceptionHandler.accept(stateMachine, e);
    }
  }

}
