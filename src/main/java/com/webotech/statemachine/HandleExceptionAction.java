package com.webotech.statemachine;

import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateMachine;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HandleExceptionAction<T> implements StateAction<T> {

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
