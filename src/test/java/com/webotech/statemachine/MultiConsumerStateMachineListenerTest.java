package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachineListener;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiConsumerStateMachineListenerTest {

  private static final State<Void> fromState = new NamedState<>("fromState");
  private static final State<Void> toState = new NamedState<>("toState");
  private static final StateEvent event = new NamedStateEvent("event");
  private MultiConsumerStateMachineListener<Void> multiConsumerStateMachineListener;

  @BeforeEach
  void setup() {
    multiConsumerStateMachineListener = new MultiConsumerStateMachineListener<>();
  }

  @Test
  void shouldPassThroughToSingleListener() {
    List<Object> beginData = new ArrayList<>();
    List<Object> endData = new ArrayList<>();
    StateMachineListener<Void> listener = new TestStateMachineListener(beginData, endData);
    assertTrue(multiConsumerStateMachineListener.add(listener));
    assertTrue(beginData.isEmpty());
    multiConsumerStateMachineListener.onStateChangeBegin(fromState, event, toState);
    assertCallbackData(beginData, 1);
    assertTrue(endData.isEmpty());
    multiConsumerStateMachineListener.onStateChangeEnd(fromState, event, toState);
    assertCallbackData(endData, 1);
  }

  @Test
  void shouldPassThroughToMultipleListeners() {
    List<Object> beginData1 = new ArrayList<>();
    List<Object> endData1 = new ArrayList<>();
    StateMachineListener<Void> listener1 = new TestStateMachineListener(beginData1, endData1);
    List<Object> beginData2 = new ArrayList<>();
    List<Object> endData2 = new ArrayList<>();
    StateMachineListener<Void> listener2 = new TestStateMachineListener(beginData2, endData2);
    assertTrue(multiConsumerStateMachineListener.add(listener1));
    assertTrue(multiConsumerStateMachineListener.add(listener2));
    assertTrue(beginData1.isEmpty());
    assertTrue(beginData2.isEmpty());
    multiConsumerStateMachineListener.onStateChangeBegin(fromState, event, toState);
    assertCallbackData(beginData1, 1);
    assertCallbackData(beginData2, 1);
    assertTrue(endData1.isEmpty());
    assertTrue(endData2.isEmpty());
    multiConsumerStateMachineListener.onStateChangeEnd(fromState, event, toState);
    assertCallbackData(endData1, 1);
    assertCallbackData(endData2, 1);
  }

  @Test
  void shouldStopUpdateOnRemove() {
    List<Object> beginData = new ArrayList<>();
    List<Object> endData = new ArrayList<>();
    StateMachineListener<Void> listener = new TestStateMachineListener(beginData, endData);
    assertTrue(multiConsumerStateMachineListener.add(listener));
    multiConsumerStateMachineListener.onStateChangeBegin(fromState, event, toState);
    assertCallbackData(beginData, 1);
    assertTrue(multiConsumerStateMachineListener.remove(listener));
    multiConsumerStateMachineListener.onStateChangeBegin(fromState, event, toState);
    assertCallbackData(beginData, 1);
  }

  @Test
  void shouldAddRemoveConsumerOnce() {
    List<Object> beginData = new ArrayList<>();
    List<Object> endData = new ArrayList<>();
    StateMachineListener<Void> listener = new TestStateMachineListener(beginData, endData);
    assertTrue(multiConsumerStateMachineListener.add(listener));
    assertFalse(multiConsumerStateMachineListener.add(listener));
    multiConsumerStateMachineListener.onStateChangeBegin(fromState, event, toState);
    assertCallbackData(beginData, 1);
    assertTrue(multiConsumerStateMachineListener.remove(listener));
    assertFalse(multiConsumerStateMachineListener.remove(listener));
  }

  private void assertCallbackData(List<Object> data, int number) {
    int ptsInDataCollection = 3;
    number = Math.max(number, 1);
    assertEquals(number * ptsInDataCollection, data.size());
    for (int i = 1; i <= number; i++) {
      assertSame(fromState, data.get((i * ptsInDataCollection) - 3));
      assertSame(event, data.get((i * ptsInDataCollection) - 2));
      assertSame(toState, data.get((i * ptsInDataCollection) - 1));
    }
  }

  private static class TestStateMachineListener implements StateMachineListener<Void> {

    private final List<Object> beginData;
    private final List<Object> endData;

    TestStateMachineListener(List<Object> beginData, List<Object> endData) {
      this.beginData = beginData;
      this.endData = endData;
    }

    @Override
    public void onStateChangeBegin(State<Void> fromState, StateEvent event, State<Void> toState) {
      beginData.add(fromState);
      beginData.add(event);
      beginData.add(toState);
    }

    @Override
    public void onStateChangeEnd(State<Void> fromState, StateEvent event, State<Void> toState) {
      endData.add(fromState);
      endData.add(event);
      endData.add(toState);
    }
  }

}