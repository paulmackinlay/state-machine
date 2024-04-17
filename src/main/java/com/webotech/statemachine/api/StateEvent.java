package com.webotech.statemachine.api;

/*
TODO maybe a StateEvent should have a generic payload that is available to StateActions so they can
affect the Context with it
 */

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
