package com.bhb.android.componentization;

import org.jetbrains.annotations.NotNull;

/**
 * 延迟代理实现示例
 * Created by Tesla on 2020/10/22.
 */
public class FunAAPI_LazyDelegate implements FunAAPI {

  private LazyDelegateImpl<FunAAPI> lazyDelegate = new LazyDelegateImpl<FunAAPI>() {};

  @Override
  public void aaaa() {
    lazyDelegate.get().aaaa();
  }

  @Override
  public void bbb() {
    lazyDelegate.get().aaaa();
  }

  @Override
  public boolean ccc(@NotNull String string) {
    return lazyDelegate.get().ccc(string);
  }
}
