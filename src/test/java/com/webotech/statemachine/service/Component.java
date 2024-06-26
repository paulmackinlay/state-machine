/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

public interface Component<C extends AbstractAppContext<C>> {

  void start(C context);

  void stop(C context);

}
