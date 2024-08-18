/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

//TODO - review move out of test src

/**
 * API for a subsystem in an {@link AbstractAppService} where an app starts and stops a collection
 * of {@link Subsystem}s in a controlled manner when the app starts and stops.
 */
//TODO replace AbstractAppContet with interface called AppContext
public interface Subsystem<C extends AbstractAppContext<?>> {

  /**
   * Starts the subsystem
   */
  void start(C appContext);

  /**
   * Stops the subsystem
   */
  void stop(C appContext);

}
