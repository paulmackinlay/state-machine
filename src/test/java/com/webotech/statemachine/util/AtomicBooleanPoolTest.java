/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AtomicBooleanPoolTest {

  private AtomicBooleanPool atomicBooleanPool;

  @BeforeEach
  void setup() {
    this.atomicBooleanPool = new AtomicBooleanPool();
  }

  @Test
  void shouldSupplyObj() {
    AtomicBoolean atomicBoolean = this.atomicBooleanPool.get();
    assertNotNull(atomicBoolean);
    assertFalse(atomicBoolean.get());
  }

  @Test
  void shouldGetCleanObjOutOfPool() {
    AtomicBoolean atomicBoolean = new AtomicBoolean(true);
    this.atomicBooleanPool.accept(atomicBoolean);
    AtomicBoolean atomicBoolean1 = this.atomicBooleanPool.get();
    assertSame(atomicBoolean, atomicBoolean1);
    assertFalse(atomicBoolean1.get());
  }
}