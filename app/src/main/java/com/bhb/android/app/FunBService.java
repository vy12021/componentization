package com.bhb.android.app;

import com.bhb.android.componentization.Service;

@Service
public class FunBService implements FunBAPI {
  @Override
  public <T> T object(T t) {
    return t;
  }
}
