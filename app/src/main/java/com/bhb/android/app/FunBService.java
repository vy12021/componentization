package com.bhb.android.app;

import com.bhb.android.componentization.Service;

@Service
public class FunBService implements FunBAPI<String> {
  @Override
  public <T> T object(T t) {
    return t;
  }

  @Override
  public <T> String xxx(T t) {
    return "xxx";
  }
}
