/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.webotech.statemachine.api.StateMachine;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;

public class TestingUtil {

  private static final long stateEventQueueTimeoutMills = 5000;
  private static final long machineEndTimeoutMills = 10000;
  private static final AtomicInteger streamCount = new AtomicInteger();

  private TestingUtil() {
    //Not for instanciation outside this class
  }

  public static OutputStream initLogCaptureStream() {
    OutputStream logStream = new ByteArrayOutputStream();
    addOutputStreamLogAppender(logStream,
        String.format("LogStream-%s", streamCount.incrementAndGet()));
    return logStream;
  }

  public static void waitForAllEventsToProcess(StateMachine<?, ?> stateMachine) {
    boolean success = awaitCondition(stateEventQueueTimeoutMills, TimeUnit.MILLISECONDS,
        () -> stateMachine.getEventQueueSize() == 0);
    if (!success) {
      throw new IllegalStateException(
          "Timed out while waiting for all events to process, took longer than "
              + stateEventQueueTimeoutMills + " millis");
    }
    sleep(200);
  }

  public static void waitForMachineToEnd(StateMachine<?, ?> stateMachine) {
    boolean success = awaitCondition(stateEventQueueTimeoutMills, TimeUnit.MILLISECONDS,
        () -> stateMachine.isEnded());
    if (!success) {
      throw new IllegalStateException(
          "Timed out while waiting for state machine to end, took longer than "
              + machineEndTimeoutMills + " millis");
    }
  }

  public static OutputStream initStdOutStream() {
    OutputStream stdOutStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(stdOutStream));
    return stdOutStream;
  }

  public static void sleep(long millis) {
    try {
      TimeUnit.MILLISECONDS.sleep(millis);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * @return true if the condition becames true before the timeout
   */
  public static boolean awaitCondition(long timeout, TimeUnit timeUnit,
      Supplier<Boolean> condition) {
    long epochNow = System.currentTimeMillis();
    long timeoutMillis = timeUnit.toMillis(timeout);
    long epochEval = epochNow;
    while (!condition.get() && epochEval <= epochNow + timeoutMillis) {
      try {
        TimeUnit.MILLISECONDS.sleep(50);
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
      epochEval = System.currentTimeMillis();
    }
    return epochEval <= epochNow + timeoutMillis;
  }

  @Test
  void shouldAwaitCondition() {
    assertTrue(awaitCondition(200, TimeUnit.MILLISECONDS, () -> 1 > 0));
    assertFalse(awaitCondition(200, TimeUnit.MILLISECONDS, () -> 1 < 0));
  }

  private static void addOutputStreamLogAppender(OutputStream logStream, String streamName) {
    LoggerContext context = LoggerContext.getContext(false);
    Configuration configuration = context.getConfiguration();
    PatternLayout patternLayout = PatternLayout.createDefaultLayout();
    Level level = null;
    Filter filter = null;
    Appender appender = OutputStreamAppender.createAppender(patternLayout, filter, logStream,
        streamName, false, true);
    appender.start();
    configuration.addAppender(appender);
    for (LoggerConfig loggerConfig : configuration.getLoggers().values()) {
      loggerConfig.addAppender(appender, level, filter);
    }
    configuration.getRootLogger().addAppender(appender, level, filter);
  }
}