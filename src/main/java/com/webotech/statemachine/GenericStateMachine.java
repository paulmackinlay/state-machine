/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;
import com.webotech.statemachine.strategy.EventProcessingStrategy;
import com.webotech.statemachine.strategy.EventProcessingStrategyFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

public class GenericStateMachine<T, S> implements StateMachine<T, S> {

  public static final String RESERVED_STATE_NAME_END = "_END_";
  private static final String RESERVED_STATE_NAME_UNINITIALISED = "_UNINITIALISED_";
  public static final String RESERVED_STATE_NAME_NOOP = "_NOOP_";
  public static final String RESERVED_STATE_EVENT_NAME_IMMEDIATE = "_immediate_";
  static final List<String> reservedStateNames = List.of(RESERVED_STATE_NAME_UNINITIALISED,
      RESERVED_STATE_NAME_END, RESERVED_STATE_NAME_NOOP);
  private final AtomicBoolean isStarted;
  private final AtomicBoolean isEnded;
  private final Map<StateEvent<S>, State<T, S>> noTransitionMap;
  private final StateEvent<S> immediateEvent;
  private final State<T, S> noState;
  private final State<T, S> endState;
  private final State<T, S> noopState;
  private final Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states;
  private final T context;
  private final EventProcessingStrategy<T, S> eventProcessingStrategy;
  private final UnexpectedFlowListener<T, S> unexpectedFlowListener;
  private StateMachineListener<T, S> stateMachineListener;
  private State<T, S> initState;
  private State<T, S> markedState;
  private StateEvent<S> markedEvent;
  private State<T, S> currentState;

  private GenericStateMachine(T context, Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states,
      StateMachineListener<T, S> stateMachineListener,
      EventProcessingStrategy<T, S> eventProcessingStrategy) {
    this.states = states;
    this.immediateEvent = new NamedStateEvent<>(RESERVED_STATE_EVENT_NAME_IMMEDIATE);
    this.endState = new NamedState<>(RESERVED_STATE_NAME_END);
    this.noState = new NamedState<>(RESERVED_STATE_NAME_UNINITIALISED);
    this.noopState = new NamedState<>(RESERVED_STATE_NAME_NOOP);
    this.noTransitionMap = Collections.emptyMap();
    this.context = context;
    this.stateMachineListener = stateMachineListener;
    this.eventProcessingStrategy = eventProcessingStrategy;
    this.unexpectedFlowListener = eventProcessingStrategy.getUnexpectedFlowListener();
    this.eventProcessingStrategy.setStates(this.states);
    isStarted = new AtomicBoolean(false);
    isEnded = new AtomicBoolean(false);
  }

  @SuppressWarnings("hiding")
  @Override
  public StateMachine<T, S> initialSate(State<T, S> initState) {
    assertNotReservedState(initState);
    assertInitStateDefined(false);
    this.initState = initState;
    states.put(initState, new HashMap<>());
    states.put(endState, noTransitionMap);
    states.put(noState, noTransitionMap);
    states.put(noopState, noTransitionMap);
    when(initState);
    return this;
  }

  @Override
  public StateMachine<T, S> when(State<T, S> state) {
    assertNotReservedState(state);
    assertInitStateDefined(true);
    assertMarkedStateDefined(false);
    this.states.putIfAbsent(state, new HashMap<>());
    this.markedState = state;
    return this;
  }

  @Override
  public StateMachine<T, S> receives(StateEvent<S> stateEvent) {
    assertNotReservedStateEvent(stateEvent);
    assertInitStateDefined(true);
    assertMarkedStateDefined(true);
    this.markedEvent = stateEvent;
    this.states.get(this.markedState).put(this.markedEvent, null);
    return this;
  }

  @Override
  public StateMachine<T, S> itEnds() {
    assertInitStateDefined(true);
    assertMarkedStateDefined(true);
    if (this.markedEvent == null) {
      assertNoMappingsExistAtEnd();
      this.states.get(this.markedState).put(immediateEvent, this.endState);
    } else {
      assertEventNotMapped();
      this.states.get(this.markedState).put(this.markedEvent, this.endState);
    }
    this.markedState = null;
    this.markedEvent = null;
    return this;
  }

  @Override
  public StateMachine<T, S> itTransitionsTo(State<T, S> state) {
    assertNotReservedState(state);
    assertInitStateDefined(true);
    assertMarkedStateDefined(true);
    assertEventNotMapped();
    this.states.get(this.markedState).put(this.markedEvent, state);
    this.markedState = null;
    this.markedEvent = null;
    return this;
  }

  @Override
  public StateMachine<T, S> itDoesNotTransition() {
    assertInitStateDefined(true);
    assertMarkedStateDefined(true);
    assertEventNotMapped();
    this.states.get(this.markedState).put(this.markedEvent, this.noopState);
    this.markedState = null;
    this.markedEvent = null;
    return this;
  }

  private void assertNotReservedState(State<T, S> state) {
    if (reservedStateNames.stream().anyMatch(r -> r.equals(state.getName()))) {
      throw new IllegalStateException(
          "Invalid state [" + state.getName() + "] is using a reserved name.");
    }
  }

  private void assertNotReservedStateEvent(StateEvent<S> stateEvent) {
    if (immediateEvent.getName().equals(stateEvent.getName())) {
      throw new IllegalStateException(
          "Invalid StateEvent [" + stateEvent.getName() + "] is using a reserved name.");
    }
  }

  private void assertEventNotMapped() {
    State<T, S> toState = this.states.get(this.markedState).get(this.markedEvent);
    if (toState != null) {
      throw new IllegalStateException(
          "State [" + this.markedState.getName() + "] already transitions to State [" + toState
              + "] when StateEvent [" + this.markedEvent.getName() + "] is received.");
    }
  }

  private void assertNoMappingsExistAtEnd() {
    if (!this.states.get(this.markedState).isEmpty()) {
      throw new IllegalStateException(
          "It is not possible to immediately end in State [" + this.markedState.getName()
              + "] since transitions for the State exist: " + this.states.get(this.markedState)
              + ".");
    }
  }

  private void assertMarkedStateDefined(boolean exists) {
    if (exists && this.markedState == null) {
      throw new IllegalStateException("A state has to be marked to be configured using when().");
    } else if (!exists && this.markedState != null) {
      throw new IllegalStateException(
          "A StateEvent for State [" + this.markedState.getName() + "] has to be defined first.");
    }
  }

  private void assertInitStateDefined(boolean exists) {
    if (exists && this.initState == null) {
      throw new IllegalStateException("An initial State has to be defined first.");
    } else if (!exists && this.initState != null) {
      throw new IllegalStateException(
          "An initial State [" + this.initState.getName() + "] already exists.");
    }
  }

  public void notifyStateMachineListener(boolean isComplete, State<T, S> fromState,
      StateEvent<S> stateEvent, State<T, S> toState) {
    if (this.stateMachineListener != null) {
      if (fromState == null) {
        fromState = this.noState;
      }
      if (toState == null) {
        toState = this.noState;
      }
      if (stateEvent == null) {
        stateEvent = this.immediateEvent;
      }
      if (isComplete) {
        this.stateMachineListener.onStateChangeEnd(fromState, stateEvent, toState);
      } else {
        this.stateMachineListener.onStateChangeBegin(fromState, stateEvent, toState);
      }
    }
  }

  public StateEvent<S> getImmediateEvent() {
    return this.immediateEvent;
  }

  public State<T, S> getEndState() {
    return this.endState;
  }

  @Override
  public void start() {
    assertInitStateDefined(true);
    if (this.states.isEmpty()) {
      throw new IllegalStateException(
          "State machine cannot be started with no defined transitions.");
    }
    for (Entry<State<T, S>, Map<StateEvent<S>, State<T, S>>> entry : this.states.entrySet()) {
      State<T, S> key = entry.getKey();
      for (Entry<StateEvent<S>, State<T, S>> entry1 : entry.getValue().entrySet()) {
        if (entry1.getValue() == null) {
          throw new IllegalStateException(
              "State " + key.getName() + " dose not transition when a " + entry1.getKey().getName()
                  + " event is received");
        }
      }
    }
    if (isStarted.compareAndSet(false, true)) {
      currentState = initState;
      notifyStateMachineListener(false, noState, immediateEvent, initState);
      initState.onEntry(immediateEvent, this);
      notifyStateMachineListener(true, noState, immediateEvent, initState);
      isEnded.set(false);
    } else {
      throw new IllegalStateException("The state machine has already been started");
    }
  }

  /**
   * This causes an immediate stop of the {@link StateMachine}. Any queued {@link StateEvent}s will
   * not process as expected and any {@link StateEvent} that is being processed may not complete as
   * expected (a runtime exception is likely).
   * <p>
   * To gracefully stop a {@link StateMachine} a {@link StateEvent} should be configured to cause it
   * to stop and fired.
   */
  @Override
  public void stop() {
    updateCurrentState(this.endState);
  }

  @Override
  public void startInState(State<T, S> state) {
    if (!states.containsKey(state)) {
      throw new IllegalStateException("State [" + state + "] has not been configured");
    }
    if (isStarted.compareAndSet(false, true)) {
      this.currentState = state;
      isEnded.set(false);
    } else {
      throw new IllegalStateException("The state machine has already been started");
    }
  }

  @Override
  public boolean isStarted() {
    return isStarted.get();
  }

  @Override
  public boolean isEnded() {
    return isEnded.get();
  }

  @Override
  public int getEventQueueSize() {
    return eventProcessingStrategy.getEventQueueSize();
  }

  @Override
  public void fire(StateEvent<S> stateEvent) {
    if (!isStarted()) {
      unexpectedFlowListener.onEventBeforeMachineStart(stateEvent, this);
    } else if (isStarted() && isEnded()) {
      unexpectedFlowListener.onEventAfterMachineEnd(stateEvent, this);
    } else {
      eventProcessingStrategy.processEvent(stateEvent, this);
    }
  }

  @Override
  public State<T, S> getCurrentState() {
    return this.currentState;
  }

  @Override
  public T getContext() {
    return this.context;
  }

  /**
   * Note for this implementation when a {@link State} has been configured with
   * {@link #itDoesNotTransition()}, the 'to state' name in
   * {@link StateMachineListener#onStateChangeBegin(State, StateEvent, State)} and
   * {@link StateMachineListener#onStateChangeEnd(State, StateEvent, State)} will be _NOOP_. This
   * indicates that no operation occurred and so no state transition took place. The
   * {@link StateMachine} current state will actually be the same as the 'from state'.
   */
  @Override
  public void setStateMachineListener(StateMachineListener<T, S> stateMachineListener) {
    this.stateMachineListener = stateMachineListener;
  }

  public void updateCurrentState(State<T, S> state) {
    if (isEnded.compareAndSet(false, state.equals(endState))) {
      currentState = state;
    }
  }

  public State<T, S> getNoopState() {
    return this.noopState;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", GenericStateMachine.class.getSimpleName() + "[", "]")
        .add("currentState=" + currentState)
        .add("context=" + context)
        .toString();
  }

  public static class Builder<T, S> {

    private T context;
    private StateMachineListener<T, S> stateMachineListener;
    private EventProcessingStrategy<T, S> eventProcessingStrategy;

    public Builder<T, S> setContext(T context) {
      this.context = context;
      return this;
    }

    public Builder<T, S> setStateMachineListener(StateMachineListener<T, S> stateMachineListener) {
      this.stateMachineListener = stateMachineListener;
      return this;
    }

    public Builder<T, S> setEventProcessingStrategy(
        EventProcessingStrategy<T, S> eventProcessingStrategy) {
      this.eventProcessingStrategy = eventProcessingStrategy;
      return this;
    }

    StateMachineListener<T, S> getStateMachineListener() {
      return stateMachineListener;
    }

    EventProcessingStrategy<T, S> getEventProcessingStrategy() {
      return eventProcessingStrategy;
    }

    public GenericStateMachine<T, S> build() {
      if (eventProcessingStrategy == null) {
        eventProcessingStrategy = EventProcessingStrategyFactory.createDefaultStrategy();
      }
      return new GenericStateMachine<>(context, new HashMap<>(), stateMachineListener,
          eventProcessingStrategy);
    }
  }
}
