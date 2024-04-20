package com.webotech.statemachine.api;

/**
 * <p>This is a representation of an event that will be received by the state machine. Typically, a
 * {@link StateMachine} will transition to a new state once a {@link StateEvent} has been
 * processed.</p>
 * <p>Note that a generic payload &lt;S&gt; can be defined if you wish to attach data to a
 * {@link StateEvent}. If it is not needed just use &lt;VOID&gt;.</p>
 */
public interface StateEvent<S> {

  /**
   * @return the name of this event
   */
  String getName();

  /**
   * @return a payload for this {@link StateEvent}
   */
  S getPayload();

  /**
   * sets a payload for this {@link StateEvent}
   */
  void setPayload(S payload);
}
