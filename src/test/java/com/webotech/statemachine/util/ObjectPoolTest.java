/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ObjectPoolTest {

  @Test
  void shouldSupportStringBuilder() {
    StringBuilder stringBuilder = ObjectPool.take(StringBuilder.class);
    assertNotNull(stringBuilder);
    assertEquals(0, stringBuilder.length());
    stringBuilder.append("txt");
    ObjectPool.give(stringBuilder);
    StringBuilder stringBuilder1 = ObjectPool.take(StringBuilder.class);
    assertEquals(0, stringBuilder1.length());
    assertSame(stringBuilder, stringBuilder1);
  }

  @Test
  void shouldSupportAtomicBoolean() {
    AtomicBoolean atomicBoolean = ObjectPool.take(AtomicBoolean.class);
    assertNotNull(atomicBoolean);
    assertFalse(atomicBoolean.get());
    atomicBoolean.set(true);
    ObjectPool.give(atomicBoolean);
    AtomicBoolean atomicBoolean1 = ObjectPool.take(AtomicBoolean.class);
    assertFalse(atomicBoolean1.get());
    assertSame(atomicBoolean, atomicBoolean1);
  }

  @Test
  void shouldNotSupportSomeObjects() {
    assertThrows(IllegalArgumentException.class, () -> ObjectPool.take(String.class));
    assertThrows(IllegalArgumentException.class, () -> ObjectPool.give(""));
  }
}