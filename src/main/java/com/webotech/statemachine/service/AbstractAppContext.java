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
  private final AtomicReference<String> appThreadNameRef;
  private final AtomicReference<List<Subsystem<C>>> subsystemsRef;

  /**
   * Constructs with an app name, initArgs will be empty
   */
  protected AbstractAppContext(String appName) {
    this(appName, new String[0]);
  }

  /**
   * Constructs with an app name and initArgs
   */
  protected AbstractAppContext(String appName, String[] initArgs) {
    this.appName = appName;
    this.appThreadNameRef = new AtomicReference<>("app");
    this.initArgs = initArgs;
    this.subsystemsRef = new AtomicReference<>(List.of());
  }

  /**
   * The app's {@link Subsystem}s that will be started in order, they should be defined at
   * construction time.
   */
  @SuppressWarnings("unchecked")
  public final C withSubsystems(List<Subsystem<C>> subsystems) {
    subsystemsRef.set(subsystems);
    return (C) this;
  }

  /**
   * The app's default thread name is 'app', it can be overridden here at construction time.
   */
  @SuppressWarnings("unchecked")
  public final C withAppThreadName(String appThreadName) {
    appThreadNameRef.set(appThreadName);
    return (C) this;
  }

  @Override
  public final String[] getInitArgs() {
    return initArgs;
  }

  @Override
  public final String getAppName() {
    return appName;
  }

  @Override
  public final List<Subsystem<C>> getSubsystems() {
    return subsystemsRef.get();
  }

  @Override
  public String getAppThreadName() {
    return appThreadNameRef.get();
  }
}
