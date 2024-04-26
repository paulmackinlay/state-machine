/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractAppContext<C extends AbstractAppContext<C>> {

  private final String[] initArgs;
  private final String appName;
  private final AtomicReference<List<Component<C>>> componentsRef;

  protected AbstractAppContext(String appName, String[] initArgs) {
    this.appName = appName;
    this.initArgs = initArgs;
    this.componentsRef = new AtomicReference<>();
  }

  @SuppressWarnings("unchecked")
  public final C withComponents(List<Component<C>> components) {
    this.componentsRef.set(components);
    return (C) this;
  }

  public final String[] getInitArgs() {
    return this.initArgs;
  }

  public final String getAppName() {
    return this.appName;
  }

  public final List<Component<C>> getComponents() {
    return this.componentsRef.get();
  }
}
