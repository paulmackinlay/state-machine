/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service.api;

/**
 * The basic contract for a standalone service. From a high level an {@link AppService} is comprised
 * from a collection of {@link Subsystem}s and has an {@link AppContext} as it's core state.
 */
public interface AppService<C> {

  /**
   * @return the {@link AppContext}
   */
  C getAppContext();

  /**
   * Starts the {@link AppService}
   */
  void start();

  /**
   * Stops the {@link AppService}
   */
  void stop();
}
