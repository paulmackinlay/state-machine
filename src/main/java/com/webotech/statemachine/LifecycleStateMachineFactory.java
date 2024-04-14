package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;

/**
 * Useful for a {@link StateMachine} that back an app.
 */
public final class LifecycleStateMachineFactory {

  public static final StateEvent startEvt = new ReplicaStateEvent("start");
  public static final StateEvent completeEvt = new ReplicaStateEvent("complete");
  public static final StateEvent stopEvt = new ReplicaStateEvent("stop");
  public static final StateEvent errorEvt = new ReplicaStateEvent("error");

  public static final String UNINITIALISED = "UNINITIALISED";
  public static final String STARTING = "STARTING";
  public static final String STARTED = "STARTED";
  public static final String STOPPING = "STOPPING";
  public static final String STOPPED = "STOPPED";
  @SuppressWarnings("rawtypes")
  private static LoggingStateMachineListener loggingStateMachineListener;

  private LifecycleStateMachineFactory() {
    // Not for instanciation outside this class
  }

  @SafeVarargs
  public static <T> State<T> newUnitialisedState(StateAction<T>... entryActions) {
    return newState(UNINITIALISED, entryActions);
  }

  @SafeVarargs
  public static <T> State<T> newStartingState(StateAction<T>... entryActions) {
    return newState(STARTING, entryActions);
  }

  @SafeVarargs
  public static <T> State<T> newStartedState(StateAction<T>... entryActions) {
    return newState(STARTED, entryActions);
  }

  @SafeVarargs
  public static <T> State<T> newStoppingState(StateAction<T>... entryActions) {
    return newState(STOPPING, entryActions);
  }

  @SafeVarargs
  public static <T> State<T> newStoppedState(StateAction<T>... entryActions) {
    return newState(STOPPED, entryActions);
  }

  public static <T> void configureAppStateMachine(StateMachine<T> stateMachine,
      State<T> uninitialised, State<T> starting,
      State<T> started, State<T> stopping, State<T> stopped) {
    stateMachine.initialSate(uninitialised).receives(startEvt).itTransitionsTo(starting);
    stateMachine.when(starting).receives(completeEvt).itTransitionsTo(started);
    stateMachine.when(starting).receives(errorEvt).itTransitionsTo(stopped);
    stateMachine.when(started).receives(stopEvt).itTransitionsTo(stopping);
    stateMachine.when(started).receives(errorEvt).itTransitionsTo(stopped);
    stateMachine.when(stopping).receives(completeEvt).itTransitionsTo(stopped);
    stateMachine.when(stopping).receives(errorEvt).itTransitionsTo(stopped);
    stateMachine.when(stopped).itEnds();
    stateMachine.start();
  }

  @SuppressWarnings("unchecked")
  public static <T> StateMachineListener<T> stateMachineLogger() {
    if (loggingStateMachineListener == null) {
      loggingStateMachineListener = new LoggingStateMachineListener<>();
    }
    return loggingStateMachineListener;
  }

  @SafeVarargs
  private static <T> State<T> newState(String name, StateAction<T>... entryActions) {
    ReplicaState<T> state = new ReplicaState<>(name);
    state.appendEntryActions(entryActions);
    return state;
  }
}
