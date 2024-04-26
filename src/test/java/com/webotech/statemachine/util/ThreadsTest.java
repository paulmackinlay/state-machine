/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThreadsTest {

  private static final AtomicReference<CountDownLatch> errorLatchRef = new AtomicReference<>();
  private static final AtomicReference<String> taskErrorRef = new AtomicReference<>();
  private static final ExecutorService executor = Executors.newSingleThreadExecutor(
      Threads.newNamedDaemonThreadFactory(
          ThreadsTest.class.getSimpleName(), (a, b) -> {
            taskErrorRef.set(b + " " + a.getName());
            CountDownLatch errorLatch = errorLatchRef.get();
            if (errorLatch != null) {
              errorLatch.countDown();
            }
          }
      ));

  @BeforeEach
  void setup() {
    taskErrorRef.set("");
  }

  @Test
  void shouldHandleUncaughtException() throws InterruptedException {
    assertEquals("", taskErrorRef.get());
    CountDownLatch latch = new CountDownLatch(1);
    errorLatchRef.set(latch);
    executor.execute(() -> {
      throw new IllegalStateException("Test induced");
    });
    boolean success = latch.await(1, TimeUnit.SECONDS);
    if (!success) {
      fail("The thread timed out");
    }
    assertTrue(taskErrorRef.get()
        .matches("java.lang.IllegalStateException: Test induced ThreadsTest-\\d+"));
  }

  @Test
  void shouldHaveNicelyNamedThread() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> threadNameRef = new AtomicReference<>();
    executor.execute(() -> {
      threadNameRef.set(Thread.currentThread().getName());
      latch.countDown();
    });
    boolean success = latch.await(1, TimeUnit.SECONDS);
    if (!success) {
      fail("The thread timed out");
    }
    assertTrue(threadNameRef.get().matches(ThreadsTest.class.getSimpleName() + "-\\d+"));
  }

}