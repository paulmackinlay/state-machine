/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service.api;

import java.util.List;

/**
 * Minimal API for an application context. Real world implementations are likely to have many more
 * getters.
 */
public interface AppContext<C extends AppContext<?>> {

  /**
   * @return initialisation arguments - analogous to the args parameter in a main method.
   */
  String[] getInitArgs();

  /**
   * @return the application name
   */
  String getAppName();

  /**
   * @return the thread name that will be used for the app's state machine.
   */
  String getAppThreadName();

  /**
   * @return the {@link Subsystem}s for the {@link AppService}
   */
  List<Subsystem<C>> getSubsystems();
}
