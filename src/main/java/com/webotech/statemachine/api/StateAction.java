package com.webotech.statemachine.api;

/**
 * Represents an action that will be fired when a states transition from one to another.
 */
public interface StateAction<T> {

  /**
   * This is called when this {@link StateAction} fires.
   */
  void execute(StateMachine<T> stateMachine);

}
