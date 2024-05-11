/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

//TODO not needed

/**
 * Simple (and naive) object pool - stores objects in unbound thread-safe {@link Queue}s
 */
public class ObjectPool {

  private static final Queue<AtomicBoolean> atomicBooleanPool = new ConcurrentLinkedQueue<>();
  private static final Queue<StringBuilder> stringBuilderPool = new ConcurrentLinkedQueue<>();

  private ObjectPool() {
    // Not for instanciation outside this class
  }

  /**
   * Takes an object out of the pool or returns a new one if the pool is empty
   */
  @SuppressWarnings("unchecked")
  public static <T> T take(Class<T> clazz) {
    if (clazz == AtomicBoolean.class) {
      AtomicBoolean atomicBoolean = atomicBooleanPool.poll();
      if (atomicBoolean == null) {
        atomicBoolean = new AtomicBoolean();
      }
      return (T) atomicBoolean;
    } else if (clazz == StringBuilder.class) {
      StringBuilder stringBuilder = stringBuilderPool.poll();
      if (stringBuilder == null) {
        stringBuilder = new StringBuilder();
      }
      return (T) stringBuilder;
    }
    throw new IllegalArgumentException(String.format("Type [%s] is not available", clazz));
  }

  /**
   * Resets the state of an object and gives it back to the pool
   */
  public static <T> void give(T t) {
    switch (t) {
      case AtomicBoolean ab -> {
        ab.set(false);
        atomicBooleanPool.offer(ab);
      }
      case StringBuilder sb -> {
        sb.setLength(0);
        stringBuilderPool.offer(sb);
      }
      default -> throw new IllegalArgumentException(
          String.format("Type [%s] is not supported", t.getClass()));
    }
  }
}
