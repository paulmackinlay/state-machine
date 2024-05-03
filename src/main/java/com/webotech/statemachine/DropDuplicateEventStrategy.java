/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DropDuplicateEventStrategy<T, S> implements EventProcessingStrategy<T, S> {

  private static final Logger logger = LogManager.getLogger(DropDuplicateEventStrategy.class);
  private static final String LOG_EVENT_BEING_PROCESSED = "StateEvent [{}] received in state [{}] already being processed";
  private final ConcurrentMap<StateEvent<S>, AtomicBoolean> eventQueue;
  private final Supplier<AtomicBoolean> atomicBooleanSupplier;
  private final Consumer<AtomicBoolean> atomicBooleanConsumer;

  /**
   * The default {@link EventProcessingStrategy}, it transitions state atomically, duplicate
   * {@link StateEvent}s received by a single {@link State} are logged but not processed.
   */
  DropDuplicateEventStrategy(Supplier<AtomicBoolean> atomicBooleanSupplier,
      Consumer<AtomicBoolean> atomicBooleanConsumer) {
    this.eventQueue = new ConcurrentHashMap<>();
    this.atomicBooleanSupplier = atomicBooleanSupplier;
    this.atomicBooleanConsumer = atomicBooleanConsumer;
  }

  @Override
  public void processEvent(StateEvent<S> stateEvent, GenericStateMachine<T, S> stateMachine,
      State<T, S> toState) {
    if (this.eventQueue.computeIfAbsent(stateEvent, k -> this.atomicBooleanSupplier.get())
        .compareAndSet(false, true)) {
      State<T, S> fromState = stateMachine.getCurrentState();
      stateMachine.notifyStateMachineListener(false, fromState, stateEvent, toState);
      stateMachine.getCurrentState().onExit(stateEvent, stateMachine);
      stateMachine.setCurrentState(toState);
      stateMachine.getCurrentState().onEntry(stateEvent, stateMachine);
      this.atomicBooleanConsumer.accept(this.eventQueue.remove(stateEvent));
      stateMachine.notifyStateMachineListener(true, fromState, stateEvent, toState);
    } else {
      logger.info(LOG_EVENT_BEING_PROCESSED, stateEvent.getName(),
          stateMachine.getCurrentState().getName());
    }
  }

}
