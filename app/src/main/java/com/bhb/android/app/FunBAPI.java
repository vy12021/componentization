package com.bhb.android.app;

import com.bhb.android.componentization.API;
import com.bhb.android.componentization.annotation.Api;

import java.io.Serializable;

@Api
public interface FunBAPI<X extends Serializable> extends API {

  <T> T object(T t);

  <T> X xxx(T t);

}
