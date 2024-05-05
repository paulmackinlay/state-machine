/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.api;

/**
 * <p>This is the API for the state machine. It made up from methods that are used to configure it
 * before it is started and service methods that are used while the state machine is running.</p>
 * <p>The follow pseudo code illustrates how you would configure a {@link StateMachine}</p>
 * <pre>
 * StateMachine sm = (new StateMachine()).initialState(STARTING);
 * sm = sm.when(STARTING).receives(DONE).itTransitionsTo(STARTED);
 * sm = sm.when(STARTED).receives(STOP).itTransitionsTo(STOPPING);
 * sm = sm.when(STOPPING).receives(DONE).itTransitionsTo(STOPPED);
 * when(STOPPED).itEnds();
 * </pre>
 */
public interface StateMachine<T, S> {

  /**
   * A configuration method: specifies what the initial state is when the {@link StateMachine} is
   * {@link #start()}ed.
   */
  StateMachine<T, S> initialSate(State<T, S> initState);

  /**
   * A configuration method: used to mark a {@link State} as the next one to be configured. It is
   * generally followed by {@link #receives(StateEvent)}.
   */
  StateMachine<T, S> when(State<T, S> state);

  /**
   * A configuration method: used to define a {@link StateEvent} that will be received by the
   * state that is currently being configured (see {@link #when(State)}). Generally it is followed
   * by {@link #itTransitionsTo(State)} or {@link #itEnds()}.
   */
  StateMachine<T, S> receives(StateEvent<S> stateEvent);

  /**
   * A configuration method: used to specify when a state machine ends. When it ends it reaches a
   * state that can no longer process {@link StateEvent}s or transition to a new {@link State}.
   */
  StateMachine<T, S> itEnds();

  /**
   * A configuration method: used to specify the subsequent {@link State} that the
   * {@link StateMachine} will transition to. Generally called after {@link #receives(StateEvent)}.
   */
  StateMachine<T, S> itTransitionsTo(State<T, S> state);

  /**
   * A configuration method: used to specify that the {@link StateMachine} does not change state and
   * no {@link StateAction} are executed. Generally called after {@link #receives(StateEvent)}.
   */
  StateMachine<T, S> itDoesNotTransition();

  /**
   * Starts the {@link StateMachine}
   */
  void start();

  //TODO add  startInState(State)

  /**
   * Fires an event
   */
  void fire(StateEvent<S> stateEvent);

  /**
   * Retrieves the current state
   */
  State<T, S> getCurrentState();

  /**
   * Retrieves the context of the {@link StateMachine}
   */
  T getContext();

  /**
   * Sets a {@link StateMachineListener} for the state machine. The {@link StateMachine} only
   * accepts a single {@link StateMachineListener} so if multiple subsystems need to be
   * independently notified of {@link State} transitions, the supplied {@link StateMachineListener}
   * is responsible for fanning out notifications.
   */
  void setStateMachineListener(StateMachineListener<T, S> stateMachineListener);

  /**
   * @return true if the {@link StateMachine} has been started.
   */
  boolean isStarted();

  /**
   * @return true when the {@link StateMachine} has reached an ended state.
   */
  boolean isEnded();

  /**
   * @return the number of {@link StateEvent}s that haven't completed processing.
   */
  int getEventQueueSize();
}
