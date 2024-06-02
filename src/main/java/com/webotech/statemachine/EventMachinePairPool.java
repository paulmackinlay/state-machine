/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple (and naive) object pool - stores objects in a unbound thread-safe {@link Queue}
 */
public class EventMachinePairPool<T, S> {

  private final Queue<EventMachinePair<T, S>> pool = new ConcurrentLinkedQueue<>();

  /**
   * Takes an object out of the pool or returns a new one if the pool is empty
   */
  EventMachinePair<T, S> take() {
    EventMachinePair<T, S> eventMachinePair = pool.poll();
    if (eventMachinePair == null) {
      eventMachinePair = new EventMachinePair<>();
    }
    return eventMachinePair;
  }

  /**
   * Resets the state of an object and gives it back to the pool
   */
  void give(EventMachinePair<T, S> eventMachinePair) {
    if (eventMachinePair != null) {
      pool.offer(eventMachinePair);
    }
  }
}
