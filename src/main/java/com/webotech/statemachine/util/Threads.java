/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class Threads {

  private Threads() {
    // Not for instanciation outside this class
  }

  public static ThreadFactory newNamedDaemonThreadFactory(final String threadName,
      UncaughtExceptionHandler uncaughtExceptionHandler) {
    return new NamedDaemonThreadFactory(threadName, uncaughtExceptionHandler);
  }

  private static class NamedDaemonThreadFactory implements ThreadFactory {

    private static final String DASH_FORMAT = "%s-%s";
    private static final AtomicInteger threadNumber = new AtomicInteger();
    private final String threadName;
    private final UncaughtExceptionHandler uncaughtExceptionHandler;

    public NamedDaemonThreadFactory(String threadName,
        UncaughtExceptionHandler uncaughtExceptionHandler) {
      this.threadName = threadName;
      this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    public NamedDaemonThreadFactory(String threadName) {
      this(threadName, null);
    }

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable);
      thread.setName(String.format(DASH_FORMAT, this.threadName, threadNumber.getAndIncrement()));
      thread.setDaemon(true);
      thread.setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
      return thread;
    }
  }
}
