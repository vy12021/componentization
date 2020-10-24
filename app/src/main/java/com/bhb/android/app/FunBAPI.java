package com.bhb.android.app;

import com.bhb.android.componentization.API;
import com.bhb.android.componentization.Api;

@Api
public interface FunBAPI extends API {

  <T> T object(T t);

}
