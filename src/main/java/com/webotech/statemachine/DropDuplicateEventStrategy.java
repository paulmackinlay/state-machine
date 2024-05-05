/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.util.AtomicBooleanPool;
import com.webotech.statemachine.util.Threads;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DropDuplicateEventStrategy<T, S> implements EventProcessingStrategy<T, S> {

  private static final Logger logger = LogManager.getLogger(DropDuplicateEventStrategy.class);
  private static final String LOG_EVENT_BEING_PROCESSED = "StateEvent [{}] received in state [{}] already being processed";
  private final ConcurrentMap<StateEvent<S>, AtomicBoolean> inFlightEvents;
  private final ConcurrentLinkedQueue<Entry<StateEvent<S>, GenericStateMachine<T, S>>> eventQueue;
  private final Supplier<AtomicBoolean> atomicBooleanSupplier;
  private final Consumer<AtomicBoolean> atomicBooleanConsumer;
  private final Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states;
  private final ExecutorService executor;
  private final BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;

  /**
   * The default {@link EventProcessingStrategy}, it transitions state atomically, duplicate
   * {@link StateEvent}s received by a single {@link State} are logged but not processed.
   */
  private DropDuplicateEventStrategy(Supplier<AtomicBoolean> atomicBooleanSupplier,
      Consumer<AtomicBoolean> atomicBooleanConsumer,
      Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states,
      BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
    this.inFlightEvents = new ConcurrentHashMap<>();
    this.atomicBooleanSupplier = atomicBooleanSupplier;
    this.atomicBooleanConsumer = atomicBooleanConsumer;
    this.eventQueue = new ConcurrentLinkedQueue<>();
    this.states = states;
    this.unmappedEventHandler = unmappedEventHandler;
    //TODO improve this
    this.executor = Executors.newSingleThreadExecutor(
        Threads.newNamedDaemonThreadFactory("state-machine", (t, e) -> {
          //TODO
          logger.error("Exception on thread " + t.getName(), e);
        }));
  }

  @Override
  public int getEventQueueSize() {
    return eventQueue.size();
  }

  @Override
  public void processEvent(StateEvent<S> stateEvent2, GenericStateMachine<T, S> stateMachine2) {
    eventQueue.offer(new AbstractMap.SimpleEntry<>(stateEvent2, stateMachine2));

    executor.execute(() -> {
      while (!eventQueue.isEmpty()) {
        Entry<StateEvent<S>, GenericStateMachine<T, S>> eventPair = eventQueue.peek();
        StateEvent<S> stateEvent = eventPair.getKey();
        GenericStateMachine<T, S> stateMachine = eventPair.getValue();
        try {
          State<T, S> toState = this.states.get(stateMachine.getCurrentState()).get(stateEvent);
          if (toState == null) {
            unmappedEventHandler.accept(stateEvent, stateMachine);
            return;
          }
          if (stateMachine.getNoopState().equals(toState)) {
            // No transition but notify the listener so can tell a StateEvent was processed
            stateMachine.notifyStateMachineListener(false, stateMachine.getCurrentState(),
                stateEvent, toState);
            stateMachine.notifyStateMachineListener(true, stateMachine.getCurrentState(),
                stateEvent, toState);
            return;
          }

          if (this.inFlightEvents.computeIfAbsent(stateEvent, k -> this.atomicBooleanSupplier.get())
              .compareAndSet(false, true)) {
            State<T, S> fromState = stateMachine.getCurrentState();
            stateMachine.notifyStateMachineListener(false, fromState, stateEvent, toState);
            stateMachine.getCurrentState().onExit(stateEvent, stateMachine);
            stateMachine.setCurrentState(toState);
            stateMachine.getCurrentState().onEntry(stateEvent, stateMachine);
            this.atomicBooleanConsumer.accept(this.inFlightEvents.remove(stateEvent));
            stateMachine.notifyStateMachineListener(true, fromState, stateEvent, toState);
          } else {
            logger.info(LOG_EVENT_BEING_PROCESSED, stateEvent.getName(),
                stateMachine.getCurrentState().getName());
          }
        } finally {
          eventQueue.poll();
        }
      }
    });
  }

  //TODO test the builder
  static class Builder<T, S> {

    private Supplier<AtomicBoolean> atomicBooleanSupplier;
    private Consumer<AtomicBoolean> atomicBooleanConsumer;
    private final Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states;
    private final BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;

    Builder(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states,
        BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
      this.states = states;
      this.unmappedEventHandler = unmappedEventHandler;
    }

    /**
     * Allows an object pool of {@link AtomicBoolean}s to be set. The pool implementation must be
     * comprised of a {@link Supplier}  and a {@link Consumer}. It is expected that the
     * implementation of these provide the logic where objects are taken from and given to the pool.
     *
     * @param atomicBooleanSupplier supplies {@link AtomicBoolean}s (take from pool)
     * @param atomicBooleanConsumer consumes {@link AtomicBoolean}s (give to pool)
     */
    DropDuplicateEventStrategy.Builder<T, S> withAtomicBooleanPool(
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
      return new DropDuplicateEventStrategy<>(atomicBooleanSupplier, atomicBooleanConsumer, states,
          unmappedEventHandler);
    }
  }
}
