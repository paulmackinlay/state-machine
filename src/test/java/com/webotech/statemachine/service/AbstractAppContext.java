/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

//TODO - review move out of test src
//TODO make an insterface from this so that there can be a service API
public abstract class AbstractAppContext<C extends AbstractAppContext<C>> {

  private final String[] initArgs;
  private final String appName;
  private final AtomicReference<List<Subsystem<C>>> subsytemsRef;

  protected AbstractAppContext(String appName, String[] initArgs) {
    this.appName = appName;
    this.initArgs = initArgs;
    this.subsytemsRef = new AtomicReference<>(List.of());
  }

  @SuppressWarnings("unchecked")
  public final C withSubsytems(List<Subsystem<C>> subsystems) {
    this.subsytemsRef.set(subsystems);
    return (C) this;
  }

  public final String[] getInitArgs() {
    return this.initArgs;
  }

  public final String getAppName() {
    return this.appName;
  }

  public final List<Subsystem<C>> getSubsystems() {
    return this.subsytemsRef.get();
  }
}
