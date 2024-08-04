/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

public interface Subsystem<C extends AbstractAppContext<C>> {

  void start(C appContext);

  void stop(C appContext);

}
