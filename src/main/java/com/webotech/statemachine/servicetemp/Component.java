package com.webotech.statemachine.servicetemp;

public interface Component<C extends AbstractAppContext<C>> {

  void start(C context);

  void stop(C context);

}
