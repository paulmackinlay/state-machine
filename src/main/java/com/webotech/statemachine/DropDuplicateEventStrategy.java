/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.util.AtomicBooleanPool;
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
  private DropDuplicateEventStrategy(Supplier<AtomicBoolean> atomicBooleanSupplier,
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

  static class Builder<T, S> {

    private Supplier<AtomicBoolean> atomicBooleanSupplier;
    private Consumer<AtomicBoolean> atomicBooleanConsumer;

    /**
     * Allows an object pool of {@link AtomicBoolean}s to be set. The pool implementation must be
     * comprised of a {@link Supplier}  and a {@link Consumer}. It is expected that the
     * implementation of these provide the logic where objects are taken from and given to the pool.
     *
     * @param atomicBooleanSupplier supplies {@link AtomicBoolean}s (take from pool)
     * @param atomicBooleanConsumer consumes {@link AtomicBoolean}s (give to pool)
     */
    public DropDuplicateEventStrategy.Builder<T, S> withAtomicBooleanPool(
        Supplier<AtomicBoolean> atomicBooleanSupplier,
        Consumer<AtomicBoolean> atomicBooleanConsumer) {
      this.atomicBooleanSupplier = atomicBooleanSupplier;
      this.atomicBooleanConsumer = atomicBooleanConsumer;
      return this;
    }

    Supplier<AtomicBoolean> getAtomicBooleanSupplier() {
      return atomicBooleanSupplier;
    }

    Consumer<AtomicBoolean> getAtomicBooleanConsumer() {
      return atomicBooleanConsumer;
    }

    public DropDuplicateEventStrategy<T, S> build() {
      if (atomicBooleanSupplier == null || atomicBooleanConsumer == null) {
        AtomicBooleanPool atomicBooleanPool = new AtomicBooleanPool();
        if (atomicBooleanSupplier == null) {
          atomicBooleanSupplier = atomicBooleanPool;
        }
        if (atomicBooleanConsumer == null) {
          atomicBooleanConsumer = atomicBooleanPool;
        }
      }
      return new DropDuplicateEventStrategy<>(atomicBooleanSupplier, atomicBooleanConsumer);
    }
  }
}
