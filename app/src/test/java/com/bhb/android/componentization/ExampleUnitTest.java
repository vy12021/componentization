package com.bhb.android.componentization;

import com.bhb.android.app.FunAAPI;

import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
  @Test
  public void addition_isCorrect() {
    LazyDelegateImpl<FunAAPI> lazyDelegate = new LazyDelegateImpl<FunAAPI>() {};
    lazyDelegate.create();
    lazyDelegate.get();
  }
}