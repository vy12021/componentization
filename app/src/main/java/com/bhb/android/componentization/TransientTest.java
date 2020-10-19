package com.bhb.android.componentization;

import android.content.Context;

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
