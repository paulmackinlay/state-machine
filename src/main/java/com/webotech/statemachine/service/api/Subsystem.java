/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service.api;

/**
 * API for a subsystem in an {@link AppService} where an app starts and stops a collection
 * of {@link Subsystem}s in a controlled manner when the app itself starts and stops.
 */
public interface Subsystem<C extends AppContext<?>> {

  /**
   * Starts the subsystem
   */
  void start(C appContext);

  /**
   * Stops the subsystem
   */
  void stop(C appContext);

}
