package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;
import com.webotech.statemachine.util.AtomicBooleanPool;
import java.util.HashMap;
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
  private static final String LOG_alreadyBeingProcessed = "] ignored due to event already being processed.";
  private static final String LOG_ignoring = "]. Ignoring.";
  private static final String LOG_receivedInState = "] recieved in state [";
  private static final String LOG_notMappedFor = "] not mapped for [";
  private static final String LOG_stateEvent = "StateEvent [";
  private static final String end = "END";
  private static final String uninitialised = "UNINITIALISED";
  private static final String immediate = "immediate";
  private static final StateEvent immediateEvent = new ReplicaStateEvent(immediate);
  private final Supplier<AtomicBoolean> atomicBooleanSupplier;
  private final Consumer<AtomicBoolean> atomicBooleanConsumer;
  private final State<T> noState;
  private final State<T> endState;
  private final Map<State<T>, Map<StateEvent, State<T>>> states;
  private final ConcurrentMap<StateEvent, AtomicBoolean> inflightEvents;
  private final T context;
  private StateMachineListener<T> stateMachineListener;
  private State<T> initState;
  private State<T> whenState;
  private StateEvent receiveAction;
  private State<T> currentState;

  private GeneralPurposeStateMachine(T context, Supplier<AtomicBoolean> atomicBooleanSupplier,
      Consumer<AtomicBoolean> atomicBooleanConsumer) {
    this.states = new HashMap<>();
    this.inflightEvents = new ConcurrentHashMap<>();
    this.endState = new ReplicaState<>(end) {
    };
    this.noState = new ReplicaState<>(uninitialised) {
    };
    this.context = context;
    this.atomicBooleanSupplier = atomicBooleanSupplier;
    this.atomicBooleanConsumer = atomicBooleanConsumer;
  }

  @SuppressWarnings("hiding")
  @Override
  public StateMachine<T> initialSate(State<T> initState) {
    initStateDefined(false);
    this.initState = initState;
    this.states.put(this.initState, new HashMap<>());
    when(initState);
    return this;
  }

  @Override
  public StateMachine<T> when(State<T> state) {
    initStateDefined(true);
    whenStateDefined(false);
    this.states.putIfAbsent(state, new HashMap<>());
    this.whenState = state;
    return this;
  }

  @Override
  public StateMachine<T> receives(StateEvent stateEvent) {
    initStateDefined(true);
    whenStateDefined(true);
    this.receiveAction = stateEvent;
    this.states.get(this.whenState).put(this.receiveAction, null);
    return this;
  }

  @Override
  public StateMachine<T> itEnds() {
    initStateDefined(true);
    whenStateDefined(true);
    if (this.receiveAction == null) {
      noMappingsExistAtEnd();
      this.states.get(this.whenState).put(immediateEvent, this.endState);
    } else {
      eventNotMapped();
      this.states.get(this.whenState).put(this.receiveAction, this.endState);
    }
    this.whenState = null;
    this.receiveAction = null;
    return this;
  }

  @Override
  public StateMachine<T> itTransitionsTo(State<T> state) {
    initStateDefined(true);
    whenStateDefined(true);
    eventNotMapped();
    this.states.get(this.whenState).put(this.receiveAction, state);
    this.whenState = null;
    this.receiveAction = null;
    return this;
  }

  private void eventNotMapped() {
    State<T> toState = this.states.get(this.whenState).get(this.receiveAction);
    if (toState != null) {
      throw new IllegalStateException(
          "State " + this.whenState.getName() + " already has a transition when event "
              + this.receiveAction.getName() + "happens, it transitions to " + toState);
    }
  }

  private void noMappingsExistAtEnd() {
    if (!this.states.get(this.whenState).isEmpty()) {
      throw new IllegalStateException(
          "It is not possible to immediately end in state " + this.whenState.getName()
              + " when transitions already exist: " + this.states.get(this.whenState));
    }
  }

  private void whenStateDefined(boolean exists) {
    if (exists && this.whenState == null) {
      throw new IllegalStateException("A 'when' state has to be defined first");
    } else if (!exists && this.whenState != null) {
      throw new IllegalStateException(
          "An action on [" + this.whenState.getName() + "] has to be defined first");
    }
  }

  private void initStateDefined(boolean exists) {
    if (exists && this.initState == null) {
      throw new IllegalStateException("Initial state has to be defined first");
    } else if (!exists && this.initState != null) {
      throw new IllegalStateException("Initial state already exists: " + this.initState.getName());
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
    initStateDefined(true);
    whenStateDefined(false);
    if (this.states.isEmpty()) {
      throw new IllegalStateException(
          "State machine cannot be started as there are no transitions defined.");
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
      logger.info(LOG_stateEvent, stateEvent.getName(), LOG_notMappedFor,
          this.currentState.getName(), LOG_ignoring);
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
      logger.info(LOG_stateEvent, stateEvent.getName(), LOG_receivedInState,
          this.currentState.getName(), LOG_alreadyBeingProcessed);
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
      return new GeneralPurposeStateMachine<T>(context, atomicBooleanSupplier,
          atomicBooleanConsumer);
    }
  }

}
