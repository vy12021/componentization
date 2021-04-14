package com.bhb.android.app;

import android.content.Context;

import com.bhb.android.componentization.annotation.AutoWired;

class TransientTest {

  private Context context;

  TransientTest(Context context) {
    this.context = context;
  }

  @AutoWired
  private ContextAPI contextAPI;

  @AutoWired
  private static FunAAPI funAAPI;

}
