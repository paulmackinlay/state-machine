/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

//TODO not needed
public class AtomicBooleanPool implements Supplier<AtomicBoolean>, Consumer<AtomicBoolean> {

  @Override
  public void accept(AtomicBoolean atomicBoolean) {
    ObjectPool.give(atomicBoolean);
  }

  @Override
  public AtomicBoolean get() {
    return ObjectPool.take(AtomicBoolean.class);
  }
}
