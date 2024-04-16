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
public interface StateMachine<T> {

  /**
   * A configuration method: specifies what the initial state is when the {@link StateMachine} is
   * {@link #start()}ed.
   */
  StateMachine<T> initialSate(State<T> initState);

  /**
   * A configuration method: used to specify a {@link State} that is being configured. It is
   * generally followed by {@link #receives(StateEvent)}.
   */
  StateMachine<T> when(State<T> state);

  /**
   * A configuration method: used to define a {@link StateEvent} that will be received by the
   * state that is currently being configured (see {@link #when(State)}). Generally it is followed
   * by {@link #itTransitionsTo(State)} or {@link #itEnds()}.
   */
  StateMachine<T> receives(StateEvent stateEvent);

  //TODO this should be more intuitive like StateMachine<T> itEndsInState(State<T> state)

  /**
   * A configuration method: used to specify when a state machine ends. When it ends it reaches a
   * state that can no longer process {@link StateEvent}s or transition to a new {@link State}.
   */
  StateMachine<T> itEnds();

  /**
   * A configuration method: used to specify the subsequent {@link State} that the
   * {@link StateMachine} will transition to.
   */
  StateMachine<T> itTransitionsTo(State<T> state);

  void start();

  void fire(StateEvent stateEvent);

  State<T> getCurrentState();

  T getContext();

  void setStateMachineListener(StateMachineListener<T> stateMachineListener);
}
