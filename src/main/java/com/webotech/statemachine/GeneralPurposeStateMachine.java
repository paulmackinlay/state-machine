package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;
import com.webotech.statemachine.util.AtomicBooleanPool;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneralPurposeStateMachine<T> implements StateMachine<T> {

  private static final Logger logger = LogManager.getLogger(GeneralPurposeStateMachine.class);
  private static final String LOG_EVENT_NOT_MAPPED = "StateEvent [{}] not mapped for state [{}], ignoring";
  private static final String LOG_EVENT_BEING_PROCESSED = "StateEvent [{}] received in state [{}] already being processed";
  private static final String RESERVED_STATE_NAME_END = "_END_";
  private static final String RESERVED_STATE_NAME_UNINITIALISED = "_UNINITIALISED_";
  private static final List<String> reservedStateNames = List.of(RESERVED_STATE_NAME_UNINITIALISED,
      RESERVED_STATE_NAME_END);
  private static final StateEvent immediateEvent = new NamedStateEvent("_immediate_");
  private final Supplier<AtomicBoolean> atomicBooleanSupplier;
  private final Consumer<AtomicBoolean> atomicBooleanConsumer;
  private final State<T> noState;
  private final State<T> endState;
  private final Map<State<T>, Map<StateEvent, State<T>>> states;
  private final ConcurrentMap<StateEvent, AtomicBoolean> inflightEvents;
  private final T context;
  private StateMachineListener<T> stateMachineListener;
  private State<T> initState;
  private State<T> markedState;
  private StateEvent receiveEvent;
  private State<T> currentState;

  private GeneralPurposeStateMachine(T context, Supplier<AtomicBoolean> atomicBooleanSupplier,
      Consumer<AtomicBoolean> atomicBooleanConsumer) {
    this.states = new HashMap<>();
    this.inflightEvents = new ConcurrentHashMap<>();
    this.endState = new NamedState<>(RESERVED_STATE_NAME_END);
    this.noState = new NamedState<>(RESERVED_STATE_NAME_UNINITIALISED);
    this.context = context;
    this.atomicBooleanSupplier = atomicBooleanSupplier;
    this.atomicBooleanConsumer = atomicBooleanConsumer;
  }

  @SuppressWarnings("hiding")
  @Override
  public StateMachine<T> initialSate(State<T> initState) {
    assertNotReservedState(initState);
    assertInitStateDefined(false);
    this.initState = initState;
    this.states.put(this.initState, new HashMap<>());
    when(initState);
    return this;
  }

  @Override
  public StateMachine<T> when(State<T> state) {
    assertNotReservedState(state);
    assertInitStateDefined(true);
    assertMarkedStateDefined(false);
    this.states.putIfAbsent(state, new HashMap<>());
    this.markedState = state;
    return this;
  }

  @Override
  public StateMachine<T> receives(StateEvent stateEvent) {
    assertNotReservedStateEvent(stateEvent);
    assertInitStateDefined(true);
    assertMarkedStateDefined(true);
    this.receiveEvent = stateEvent;
    this.states.get(this.markedState).put(this.receiveEvent, null);
    return this;
  }

  @Override
  public StateMachine<T> itEnds() {
    assertInitStateDefined(true);
    assertMarkedStateDefined(true);
    if (this.receiveEvent == null) {
      assertNoMappingsExistAtEnd();
      this.states.get(this.markedState).put(immediateEvent, this.endState);
    } else {
      assertEventNotMapped();
      this.states.get(this.markedState).put(this.receiveEvent, this.endState);
    }
    this.markedState = null;
    this.receiveEvent = null;
    return this;
  }

  @Override
  public StateMachine<T> itTransitionsTo(State<T> state) {
    assertNotReservedState(state);
    assertInitStateDefined(true);
    assertMarkedStateDefined(true);
    assertEventNotMapped();
    this.states.get(this.markedState).put(this.receiveEvent, state);
    this.markedState = null;
    this.receiveEvent = null;
    return this;
  }

  private void assertNotReservedState(State<T> state) {
    if (reservedStateNames.stream().anyMatch(r -> r.equals(state.getName()))) {
      throw new IllegalStateException(
          "Invalid state [" + state.getName() + "] is using a reserved name.");
    }
  }

  private void assertNotReservedStateEvent(StateEvent stateEvent) {
    if (immediateEvent.getName().equals(stateEvent.getName())) {
      throw new IllegalStateException(
          "Invalid StateEvent [" + stateEvent.getName() + "] is using a reserved name.");
    }
  }

  private void assertEventNotMapped() {
    State<T> toState = this.states.get(this.markedState).get(this.receiveEvent);
    if (toState != null) {
      throw new IllegalStateException(
          "State [" + this.markedState.getName() + "] already transitions to State [" + toState
              + "] when StateEvent [" + this.receiveEvent.getName() + "] is received.");
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

  private void fireTransitionLogging(boolean isComplete, State<T> fromState, StateEvent stateEvent,
      State<T> toState) {
    if (this.stateMachineListener != null) {
      if (fromState == null) {
        fromState = this.noState;
      }
      if (toState == null) {
        toState = this.noState;
      }
      if (stateEvent == null) {
        stateEvent = immediateEvent;
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
    assertMarkedStateDefined(false);
    if (this.states.isEmpty()) {
      throw new IllegalStateException(
          "State machine cannot be started with no defined transitions.");
    }
    this.initState.onEntry(this);
    this.currentState = this.initState;
  }

  @Override
  public void fire(StateEvent stateEvent) {
    if (this.currentState == null) {
      throw new IllegalStateException(
          "The current state is null, did you start the state machine?");
    }
    State<T> toState = this.states.get(this.currentState).get(stateEvent);
    if (toState == null) {
      logger.info(LOG_EVENT_NOT_MAPPED, stateEvent.getName(), this.currentState.getName());
      return;
    }
    if (this.inflightEvents.computeIfAbsent(stateEvent, k -> this.atomicBooleanSupplier.get())
        .compareAndSet(false, true)) {
      State<T> fromState = this.currentState;
      fireTransitionLogging(false, fromState, stateEvent, toState);
      this.currentState.onExit(this);
      this.currentState = toState;
      this.currentState.onEntry(this);
      this.atomicBooleanConsumer.accept(this.inflightEvents.remove(stateEvent));
      fireTransitionLogging(true, fromState, stateEvent, toState);
    } else {
      logger.info(LOG_EVENT_BEING_PROCESSED, stateEvent.getName(), this.currentState.getName());
    }
  }

  @Override
  public State<T> getCurrentState() {
    return this.currentState;
  }

  @Override
  public T getContext() {
    return this.context;
  }

  @Override
  public void setStateMachineListener(StateMachineListener<T> stateMachineListener) {
    this.stateMachineListener = stateMachineListener;
  }

  public static class Builder<T> {

    private T context;
    private Supplier<AtomicBoolean> atomicBooleanSupplier;
    private Consumer<AtomicBoolean> atomicBooleanConsumer;

    public Builder<T> setContext(T context) {
      this.context = context;
      return this;
    }

    public Builder<T> withAtomicBooleanPool(Supplier<AtomicBoolean> atomicBooleanSupplier,
        Consumer<AtomicBoolean> atomicBooleanConsumer) {
      this.atomicBooleanSupplier = atomicBooleanSupplier;
      this.atomicBooleanConsumer = atomicBooleanConsumer;
      return this;
    }

    public GeneralPurposeStateMachine<T> build() {
      if (atomicBooleanSupplier == null || atomicBooleanConsumer == null) {
        AtomicBooleanPool atomicBooleanPool = new AtomicBooleanPool();
        if (atomicBooleanSupplier == null) {
          atomicBooleanSupplier = atomicBooleanPool;
        }
        if (atomicBooleanConsumer == null) {
          atomicBooleanConsumer = atomicBooleanPool;
        }
      }
      return new GeneralPurposeStateMachine<>(context, atomicBooleanSupplier,
          atomicBooleanConsumer);
    }
  }

}
