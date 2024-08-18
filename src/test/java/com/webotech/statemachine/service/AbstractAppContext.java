/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

//TODO - review move out of test src
public abstract class AbstractAppContext<C extends AbstractAppContext<C>> implements AppContext<C> {

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

  @Override
  public final String[] getInitArgs() {
    return this.initArgs;
  }

  @Override
  public final String getAppName() {
    return this.appName;
  }

  @Override
  public final List<Subsystem<C>> getSubsystems() {
    return this.subsytemsRef.get();
  }
}
