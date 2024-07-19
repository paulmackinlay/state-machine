/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.strategy;

import com.webotech.statemachine.UnexpectedFlowListener;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DefaultUnexpectedFlowListener<T, S> implements UnexpectedFlowListener<T, S> {

  private static final Logger logger = LogManager.getLogger(DefaultUnexpectedFlowListener.class);

  @Override
  public void onExceptionDuringEventProcessing(StateEvent<S> stateEvent,
      StateMachine<T, S> stateMachine, Thread thread, Exception e) {
    logger.error("Unhandled exception while processing event {} while in state {} on thread [{}]",
        stateEvent, stateMachine.getCurrentState(), thread.getName(), e);
  }

  @Override
  public void onEventAfterMachineEnd(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine) {
    logger.warn("Event {} received after state machine has ended", stateEvent);
  }

  @Override
  public void onEventBeforeMachineStart(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine) {
    logger.warn("Event {} received before state machine has started", stateEvent);
  }
}
