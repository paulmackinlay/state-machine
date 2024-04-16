package com.webotech.statemachine.api;

/**
 * Representation of a state of the state machine
 */
public interface State<T> {

  /**
   * This is called when the state machine transitions to this {@link State}. Any
   * {@link StateAction}s that has been assigned to state entry will fire.
   */
  void onEntry(StateMachine<T> stateMachine);

  /**
   * This is called when the state machine transitions away from this {@link State}.  Any
   * {@link StateAction}s that has been assigned to state exit will fire.
   */
  void onExit(StateMachine<T> stateMachine);

  /**
   * This will assign the {@link StateAction} to fire when the state is entered.
   */
  @SuppressWarnings("unchecked")
  void appendEntryActions(StateAction<T>... actions);

  /**
   * This will assign the {@link StateAction} to fire when the state is exited.
   */
  @SuppressWarnings("unchecked")
  void appendExitActions(StateAction<T>... actions);

  /**
   * @return the name of this {@link State}
   */
  String getName();
}
