package com.bhb.android.app;

import android.app.Application;

import com.bhb.android.componentization.API;
import com.bhb.android.componentization.Api_;

@Api_(singleton = true)
public interface ContextAPI extends API {

  Application getApplication();

  void toast(String msg);

}