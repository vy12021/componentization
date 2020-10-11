package com.bhb.android.componentization;

import android.app.Application;

@Api(singleton = true)
public interface ContextAPI extends API {

  Application getApplication();

  void toast(String msg);

}
