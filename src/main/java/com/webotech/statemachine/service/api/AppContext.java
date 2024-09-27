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
   * @return initialisation arguments - analagous to the args parameter in a main method.
   */
  String[] getInitArgs();

  /**
   * @return the application name
   */
  String getAppName();

  /**
   * @return
   */
  List<Subsystem<C>> getSubsystems();
}
