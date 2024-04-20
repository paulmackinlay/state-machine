package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;

//TODO review this

/**
 * Useful for a {@link StateMachine} that backs an app.
 */
public final class LifecycleStateMachineFactory {

  public static final StateEvent<Void> startEvt = new NamedStateEvent<>("start");
  public static final StateEvent<Void> completeEvt = new NamedStateEvent<>("complete");
  public static final StateEvent<Void> stopEvt = new NamedStateEvent<>("stop");
  public static final StateEvent<Void> errorEvt = new NamedStateEvent<>("error");

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
  public static <T> State<T, Void> newUnitialisedState(StateAction<T, Void>... entryActions) {
    return newState(UNINITIALISED, entryActions);
  }

  @SafeVarargs
  public static <T> State<T, Void> newStartingState(StateAction<T, Void>... entryActions) {
    return newState(STARTING, entryActions);
  }

  @SafeVarargs
  public static <T> State<T, Void> newStartedState(StateAction<T, Void>... entryActions) {
    return newState(STARTED, entryActions);
  }

  @SafeVarargs
  public static <T> State<T, Void> newStoppingState(StateAction<T, Void>... entryActions) {
    return newState(STOPPING, entryActions);
  }

  @SafeVarargs
  public static <T> State<T, Void> newStoppedState(StateAction<T, Void>... entryActions) {
    return newState(STOPPED, entryActions);
  }

  public static <T> void configureAppStateMachine(StateMachine<T, Void> stateMachine,
      State<T, Void> uninitialised, State<T, Void> starting,
      State<T, Void> started, State<T, Void> stopping, State<T, Void> stopped) {
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
  public static <T, S> StateMachineListener<T, S> stateMachineLogger() {
    if (loggingStateMachineListener == null) {
      loggingStateMachineListener = new LoggingStateMachineListener<>();
    }
    return loggingStateMachineListener;
  }

  @SafeVarargs
  private static <T, S> State<T, S> newState(String name, StateAction<T, S>... entryActions) {
    NamedState<T, S> state = new NamedState<>(name);
    state.appendEntryActions(entryActions);
    return state;
  }
}
