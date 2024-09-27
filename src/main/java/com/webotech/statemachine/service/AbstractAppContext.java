/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import com.webotech.statemachine.service.api.AppContext;
import com.webotech.statemachine.service.api.Subsystem;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractAppContext<C extends AbstractAppContext<C>> implements AppContext<C> {

  private final String[] initArgs;
  private final String appName;
  private final AtomicReference<List<Subsystem<C>>> subsystemsRef;

  protected AbstractAppContext(String appName, String[] initArgs) {
    this.appName = appName;
    this.initArgs = initArgs;
    this.subsystemsRef = new AtomicReference<>(List.of());
  }

  @SuppressWarnings("unchecked")
  public final C withSubsystems(List<Subsystem<C>> subsystems) {
    this.subsystemsRef.set(subsystems);
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
    return this.subsystemsRef.get();
  }
}
