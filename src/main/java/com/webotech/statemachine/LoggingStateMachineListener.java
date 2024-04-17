package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachineListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggingStateMachineListener<T> implements StateMachineListener<T> {

  //TODO review logging - put log statements into it's own class
  private static final String starting_transition = "Starting transition ";
  private static final String transitioned_to = "Transitioned to ";
  private static final Logger logger = LogManager.getLogger(LoggingStateMachineListener.class);
  private static final String equals = " = ";
  private static final String plus = " + ";
  private static final String colon = ": ";
  private final StateMachineListener<T> stateMachineListener;
  private final String name;

  public LoggingStateMachineListener() {
    this(null, null);
  }

  public LoggingStateMachineListener(String name) {
    this(null, name);
  }

  public LoggingStateMachineListener(StateMachineListener<T> stateMachineListener, String name) {
    this.stateMachineListener = stateMachineListener;
    this.name = name;
  }

  @Override
  public void onStateChangeBegin(State<T> fromState, StateEvent event, State<T> toState) {
    logChange(false, fromState, event, toState);
    if (this.stateMachineListener != null) {
      this.stateMachineListener.onStateChangeBegin(fromState, event, toState);
    }
  }

  @Override
  public void onStateChangeEnd(State<T> fromState, StateEvent event, State<T> toState) {
    logChange(true, fromState, event, toState);
    if (this.stateMachineListener != null) {
      this.stateMachineListener.onStateChangeEnd(fromState, event, toState);
    }
  }

  private void logChange(boolean isComplete, State<T> oldState, StateEvent event,
      State<T> newState) {
    if (this.name != null && !this.name.isEmpty()) {
      if (isComplete) {
        logger.info(transitioned_to, newState.getName());
      } else {
        logger.info(starting_transition, this.name, colon, oldState.getName(), plus,
            event.getName(), equals, newState.getName());
      }
    } else {
      if (isComplete) {
        logger.info(transitioned_to, newState.getName());
      } else {
        logger.info(starting_transition, oldState.getName(), plus, event.getName(), equals,
            newState.getName());
      }
    }
  }
}
