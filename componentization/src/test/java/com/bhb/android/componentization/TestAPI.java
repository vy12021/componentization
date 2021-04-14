package com.bhb.android.componentization;

import com.bhb.android.componentization.annotation.Api;

import java.io.Serializable;

@Api
public interface TestAPI extends API2<Boolean, Integer> {
  <T extends Serializable> T doSomething(T p);
}