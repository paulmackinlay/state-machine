/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;
import com.webotech.statemachine.util.AtomicBooleanPool;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GenericStateMachine<T, S> implements StateMachine<T, S> {

  private static final Logger logger = LogManager.getLogger(GenericStateMachine.class);
  private static final String LOG_EVENT_NOT_MAPPED = "StateEvent [{}] not mapped for state [{}], ignoring";
  private static final String LOG_EVENT_BEING_PROCESSED = "StateEvent [{}] received in state [{}] already being processed";
  private static final String RESERVED_STATE_NAME_END = "_END_";
  private static final String RESERVED_STATE_NAME_UNINITIALISED = "_UNINITIALISED_";
  private static final String RESERVED_STATE_NAME_NOOP = "_NOOP_";
  private static final String RESERVED_STATE_EVENT_NAME_IMMEDIATE = "_immediate_";
  static final List<String> reservedStateNames = List.of(RESERVED_STATE_NAME_UNINITIALISED,
      RESERVED_STATE_NAME_END, RESERVED_STATE_NAME_NOOP);
  private final StateEvent<S> immediateEvent;
  private final Supplier<AtomicBoolean> atomicBooleanSupplier;
  private final Consumer<AtomicBoolean> atomicBooleanConsumer;
  private final State<T, S> noState;
  private final State<T, S> endState;
  private final State<T, S> noopState;
  private final Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states;
  private final ConcurrentMap<StateEvent<S>, AtomicBoolean> inflightEvents;
  private final T context;
  private StateMachineListener<T, S> stateMachineListener;
  private BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;
  private State<T, S> initState;
  private State<T, S> markedState;
  private StateEvent<S> markedEvent;
  private State<T, S> currentState;

  private GenericStateMachine(T context, Supplier<AtomicBoolean> atomicBooleanSupplier,
      Consumer<AtomicBoolean> atomicBooleanConsumer,
      StateMachineListener<T, S> stateMachineListener,
      BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
    this.states = new HashMap<>();
    this.inflightEvents = new ConcurrentHashMap<>();
    this.immediateEvent = new NamedStateEvent<>(RESERVED_STATE_EVENT_NAME_IMMEDIATE);
    this.endState = new NamedState<>(RESERVED_STATE_NAME_END);
    this.noState = new NamedState<>(RESERVED_STATE_NAME_UNINITIALISED);
    this.noopState = new NamedState<>(RESERVED_STATE_NAME_NOOP);
    this.context = context;
    this.atomicBooleanSupplier = atomicBooleanSupplier;
    this.atomicBooleanConsumer = atomicBooleanConsumer;
    this.stateMachineListener = stateMachineListener;
    this.unmappedEventHandler = unmappedEventHandler;
  }

  @SuppressWarnings("hiding")
  @Override
  public StateMachine<T, S> initialSate(State<T, S> initState) {
    assertNotReservedState(initState);
    assertInitStateDefined(false);
    this.initState = initState;
    this.states.put(this.initState, new HashMap<>());
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

  private void notifyStateMachineListener(boolean isComplete, State<T, S> fromState,
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
    this.initState.onEntry(immediateEvent, this);
    this.currentState = this.initState;
  }

  @Override
  public boolean isStarted() {
    return this.currentState != null && !this.currentState.equals(noState);
  }

  @Override
  public boolean isEnded() {
    return this.currentState != null && this.currentState.equals(endState);
  }

  @Override
  public void fire(StateEvent<S> stateEvent) {
    if (this.currentState == null) {
      throw new IllegalStateException(
          "The current state is null, did you start the state machine?");
    }
    State<T, S> toState = this.states.get(this.currentState).get(stateEvent);
    if (toState == null) {
      this.unmappedEventHandler.accept(stateEvent, this);
      return;
    }
    if (this.noopState.equals(toState)) {
      // No transition but notify the listener so can tell a StateEvent was processed
      notifyStateMachineListener(false, this.currentState, stateEvent, toState);
      notifyStateMachineListener(true, this.currentState, stateEvent, toState);
      return;
    }
    if (this.inflightEvents.computeIfAbsent(stateEvent, k -> this.atomicBooleanSupplier.get())
        .compareAndSet(false, true)) {
      State<T, S> fromState = this.currentState;
      notifyStateMachineListener(false, fromState, stateEvent, toState);
      this.currentState.onExit(stateEvent, this);
      this.currentState = toState;
      this.currentState.onEntry(stateEvent, this);
      this.atomicBooleanConsumer.accept(this.inflightEvents.remove(stateEvent));
      notifyStateMachineListener(true, fromState, stateEvent, toState);
    } else {
      logger.info(LOG_EVENT_BEING_PROCESSED, stateEvent.getName(), this.currentState.getName());
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

  @Override
  public void setUnmappedEventHandler(
      BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
    this.unmappedEventHandler = unmappedEventHandler;
  }

  public static class Builder<T, S> {

    private T context;
    private Supplier<AtomicBoolean> atomicBooleanSupplier;
    private Consumer<AtomicBoolean> atomicBooleanConsumer;
    private StateMachineListener<T, S> stateMachineListener;
    private BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;

    public Builder<T, S> setContext(T context) {
      this.context = context;
      return this;
    }

    /**
     * Allows an object pool of {@link AtomicBoolean}s to be set. The pool implementation must be
     * comprised of a {@link Supplier}  and a {@link Consumer}. It is expected that the
     * implementation of these provide the logic where objects are taken from and given to the pool.
     *
     * @param atomicBooleanSupplier supplies {@link AtomicBoolean}s (take from pool)
     * @param atomicBooleanConsumer consumes {@link AtomicBoolean}s (give to pool)
     */
    public Builder<T, S> withAtomicBooleanPool(Supplier<AtomicBoolean> atomicBooleanSupplier,
        Consumer<AtomicBoolean> atomicBooleanConsumer) {
      this.atomicBooleanSupplier = atomicBooleanSupplier;
      this.atomicBooleanConsumer = atomicBooleanConsumer;
      return this;
    }

    /**
     * When the {@link StateMachine} receives a {@link StateEvent} that is not mapped for the
     * current state, by default this is logged but setting the handler here allows you to override
     * the behaviour. This handler is called with the {@link StateEvent} that was received and a
     * reference to the {@link StateMachine}.
     */
    public Builder<T, S> setUnmappedEventHandler(
        BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
      this.unmappedEventHandler = unmappedEventHandler;
      return this;
    }

    Builder<T, S> setStateMachineListener(StateMachineListener<T, S> stateMachineListener) {
      this.stateMachineListener = stateMachineListener;
      return this;
    }

    StateMachineListener<T, S> getStateMachineListener() {
      return stateMachineListener;
    }

    BiConsumer<StateEvent<S>, StateMachine<T, S>> getUnmappedEventHandler() {
      return this.unmappedEventHandler;
    }

    Supplier<AtomicBoolean> getAtomicBooleanSupplier() {
      return atomicBooleanSupplier;
    }

    Consumer<AtomicBoolean> getAtomicBooleanConsumer() {
      return atomicBooleanConsumer;
    }

    public GenericStateMachine<T, S> build() {
      if (atomicBooleanSupplier == null || atomicBooleanConsumer == null) {
        AtomicBooleanPool atomicBooleanPool = new AtomicBooleanPool();
        if (atomicBooleanSupplier == null) {
          atomicBooleanSupplier = atomicBooleanPool;
        }
        if (atomicBooleanConsumer == null) {
          atomicBooleanConsumer = atomicBooleanPool;
        }
      }
      if (unmappedEventHandler == null) {
        unmappedEventHandler = (ev, sm) -> logger.info(LOG_EVENT_NOT_MAPPED, ev.getName(),
            sm.getCurrentState().getName());
      }
      return new GenericStateMachine<>(context, atomicBooleanSupplier, atomicBooleanConsumer,
          stateMachineListener, unmappedEventHandler);
    }
  }

}
