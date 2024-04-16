package com.webotech.statemachine.api;

/**
 * This is a representation of an event that will be received by the state machine. Typically, a
 * state machine will transition to a new state once a {@link StateEvent} has been processed.
 */
public interface StateEvent {

  /**
   * @return the name of this event
   */
  String getName();
}
