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

public class GenericStateMachine<T> implements StateMachine<T> {

  private static final Logger logger = LogManager.getLogger(GenericStateMachine.class);
  private static final String LOG_EVENT_NOT_MAPPED = "StateEvent [{}] not mapped for state [{}], ignoring";
  private static final String LOG_EVENT_BEING_PROCESSED = "StateEvent [{}] received in state [{}] already being processed";
  private static final String RESERVED_STATE_NAME_END = "_END_";
  private static final String RESERVED_STATE_NAME_UNINITIALISED = "_UNINITIALISED_";
  private static final String RESERVED_STATE_NAME_NOOP = "_NOOP_";
  static final List<String> reservedStateNames = List.of(RESERVED_STATE_NAME_UNINITIALISED,
      RESERVED_STATE_NAME_END, RESERVED_STATE_NAME_NOOP);
  private static final StateEvent immediateEvent = new NamedStateEvent("_immediate_");
  private final Supplier<AtomicBoolean> atomicBooleanSupplier;
  private final Consumer<AtomicBoolean> atomicBooleanConsumer;
  private final BiConsumer<StateEvent, StateMachine<T>> unmappedEventHandler;
  private final State<T> noState;
  private final State<T> endState;
  private final State<T> noopState;
  private final Map<State<T>, Map<StateEvent, State<T>>> states;
  private final ConcurrentMap<StateEvent, AtomicBoolean> inflightEvents;
  private final T context;
  private StateMachineListener<T> stateMachineListener;
  private State<T> initState;
  private State<T> markedState;
  private StateEvent markedEvent;
  private State<T> currentState;

  private GenericStateMachine(T context, Supplier<AtomicBoolean> atomicBooleanSupplier,
      Consumer<AtomicBoolean> atomicBooleanConsumer,
      BiConsumer<StateEvent, StateMachine<T>> unmappedEventHandler) {
    this.states = new HashMap<>();
    this.inflightEvents = new ConcurrentHashMap<>();
    this.endState = new NamedState<>(RESERVED_STATE_NAME_END);
    this.noState = new NamedState<>(RESERVED_STATE_NAME_UNINITIALISED);
    this.noopState = new NamedState<>(RESERVED_STATE_NAME_NOOP);
    this.context = context;
    this.atomicBooleanSupplier = atomicBooleanSupplier;
    this.atomicBooleanConsumer = atomicBooleanConsumer;
    this.unmappedEventHandler = unmappedEventHandler;
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
    this.markedEvent = stateEvent;
    this.states.get(this.markedState).put(this.markedEvent, null);
    return this;
  }

  @Override
  public StateMachine<T> itEnds() {
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
  public StateMachine<T> itTransitionsTo(State<T> state) {
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
  public StateMachine<T> itDoesNotTransition() {
    assertInitStateDefined(true);
    assertMarkedStateDefined(true);
    assertEventNotMapped();
    this.states.get(this.markedState).put(this.markedEvent, this.noopState);
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
    State<T> toState = this.states.get(this.markedState).get(this.markedEvent);
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

  private void notifyStateMachineListener(boolean isComplete, State<T> fromState,
      StateEvent stateEvent, State<T> toState) {
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
    if (this.states.isEmpty()) {
      throw new IllegalStateException(
          "State machine cannot be started with no defined transitions.");
    }
    for (Entry<State<T>, Map<StateEvent, State<T>>> entry : this.states.entrySet()) {
      State<T> key = entry.getKey();
      for (Entry<StateEvent, State<T>> entry1 : entry.getValue().entrySet()) {
        if (entry1.getValue() == null) {
          throw new IllegalStateException(
              "State " + key.getName() + " dose not transition when a " + entry1.getKey().getName()
                  + " event is received");
        }
      }
    }
    this.initState.onEntry(this);
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
  public void fire(StateEvent stateEvent) {
    if (this.currentState == null) {
      throw new IllegalStateException(
          "The current state is null, did you start the state machine?");
    }
    State<T> toState = this.states.get(this.currentState).get(stateEvent);
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
      State<T> fromState = this.currentState;
      notifyStateMachineListener(false, fromState, stateEvent, toState);
      this.currentState.onExit(this);
      this.currentState = toState;
      this.currentState.onEntry(this);
      this.atomicBooleanConsumer.accept(this.inflightEvents.remove(stateEvent));
      notifyStateMachineListener(true, fromState, stateEvent, toState);
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
    private BiConsumer<StateEvent, StateMachine<T>> unmappedEventHandler;

    public Builder<T> setContext(T context) {
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
    public Builder<T> withAtomicBooleanPool(Supplier<AtomicBoolean> atomicBooleanSupplier,
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
    public Builder<T> setUnmappedEventHandler(
        BiConsumer<StateEvent, StateMachine<T>> unmappedEventHandler) {
      this.unmappedEventHandler = unmappedEventHandler;
      return this;
    }

    BiConsumer<StateEvent, StateMachine<T>> getUnmappedEventHandler() {
      return this.unmappedEventHandler;
    }

    Supplier<AtomicBoolean> getAtomicBooleanSupplier() {
      return atomicBooleanSupplier;
    }

    Consumer<AtomicBoolean> getAtomicBooleanConsumer() {
      return atomicBooleanConsumer;
    }

    public GenericStateMachine<T> build() {
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
          unmappedEventHandler);
    }
  }

}
