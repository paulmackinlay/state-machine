package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachineListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggingStateMachineListener<T> implements StateMachineListener<T> {

  private static final Logger logger = LogManager.getLogger(LoggingStateMachineListener.class);
  private static final String LOG_STARTING_TRANSITION_NAMED = "Starting {} transition: {} + {} = {}";
  private static final String LOG_STARTING_TRANSITION = "Starting transition: {} + {} = {}";
  private static final String LOG_TRANSITIONED_NAMED = "{} transitioned to {}";
  private static final String LOG_TRANSITIONED = "Transitioned to {}";
  private final String name;

  public LoggingStateMachineListener() {
    this(null);
  }

  /**
   * @param name - is an additional name for logging, it may be the name of the state machine
   */
  public LoggingStateMachineListener(String name) {
    this.name = name;
  }

  @Override
  public void onStateChangeBegin(State<T> fromState, StateEvent event, State<T> toState) {
    logChange(false, fromState, event, toState);
  }

  @Override
  public void onStateChangeEnd(State<T> fromState, StateEvent event, State<T> toState) {
    logChange(true, fromState, event, toState);
  }

  private void logChange(boolean isComplete, State<T> oldState, StateEvent event,
      State<T> newState) {
    if (this.name != null && !this.name.isEmpty()) {
      if (isComplete) {
        logger.info(LOG_TRANSITIONED_NAMED, this.name, newState.getName());
      } else {
        logger.info(LOG_STARTING_TRANSITION_NAMED, this.name, oldState.getName(), event.getName(),
            newState.getName());
      }
    } else {
      if (isComplete) {
        logger.info(LOG_TRANSITIONED, newState.getName());
      } else {
        logger.info(LOG_STARTING_TRANSITION, oldState.getName(), event.getName(),
            newState.getName());
      }
    }
  }
}
