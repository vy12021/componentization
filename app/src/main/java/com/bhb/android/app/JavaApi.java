package com.bhb.android.app;

import com.bhb.android.componentization.API;
import com.bhb.android.componentization.Api;

import java.util.List;

@Api
public interface JavaApi extends API {

  void funInvoke(List<String> args);

}
