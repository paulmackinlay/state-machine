/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import com.webotech.statemachine.NamedState;
import com.webotech.statemachine.NamedStateEvent;
import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;

/**
 * Useful for a {@link StateMachine} that backs an app.
 */
//TODO review the name of this
public final class LifecycleStateMachineFactory {

  public static final StateEvent<Void> evtStart = new NamedStateEvent<>("start");
  public static final StateEvent<Void> evtComplete = new NamedStateEvent<>("complete");
  public static final StateEvent<Void> evtStop = new NamedStateEvent<>("stop");
  public static final StateEvent<Void> evtError = new NamedStateEvent<>("error");
  public static final String STATE_UNINITIALISED = "UNINITIALISED";
  public static final String STATE_STARTING = "STARTING";
  public static final String STATE_STARTED = "STARTED";
  public static final String STATE_STOPPING = "STOPPING";
  public static final String STATE_STOPPED = "STOPPED";

  private LifecycleStateMachineFactory() {
    // Not for instanciation outside this class
  }

  @SafeVarargs
  public static <T> State<T, Void> newUnitialisedState(StateAction<T, Void>... entryActions) {
    return newState(STATE_UNINITIALISED, entryActions);
  }

  @SafeVarargs
  public static <T> State<T, Void> newStartingState(StateAction<T, Void>... entryActions) {
    return newState(STATE_STARTING, entryActions);
  }

  @SafeVarargs
  public static <T> State<T, Void> newStartedState(StateAction<T, Void>... entryActions) {
    return newState(STATE_STARTED, entryActions);
  }

  @SafeVarargs
  public static <T> State<T, Void> newStoppingState(StateAction<T, Void>... entryActions) {
    return newState(STATE_STOPPING, entryActions);
  }

  @SafeVarargs
  public static <T> State<T, Void> newStoppedState(StateAction<T, Void>... entryActions) {
    return newState(STATE_STOPPED, entryActions);
  }

  public static <T> void configureAppStateMachine(StateMachine<T, Void> stateMachine,
      State<T, Void> uninitialised, State<T, Void> starting,
      State<T, Void> started, State<T, Void> stopping, State<T, Void> stopped) {
    stateMachine.initialSate(uninitialised).receives(evtStart).itTransitionsTo(starting);
    stateMachine.when(starting).receives(evtComplete).itTransitionsTo(started);
    stateMachine.when(starting).receives(evtError).itTransitionsTo(stopped);
    stateMachine.when(started).receives(evtStop).itTransitionsTo(stopping);
    stateMachine.when(started).receives(evtError).itTransitionsTo(stopped);
    stateMachine.when(stopping).receives(evtComplete).itTransitionsTo(stopped);
    stateMachine.when(stopping).receives(evtError).itTransitionsTo(stopped);
    stateMachine.when(stopped).itEnds();
  }

  @SafeVarargs
  private static <T, S> State<T, S> newState(String name, StateAction<T, S>... entryActions) {
    NamedState<T, S> state = new NamedState<>(name);
    state.appendEntryActions(entryActions);
    return state;
  }
}
