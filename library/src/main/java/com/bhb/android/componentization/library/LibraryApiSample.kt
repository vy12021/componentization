package com.bhb.android.componentization.library

import android.content.Context
import com.bhb.android.componentization.AutoWired

class LibraryApiSample {

  @AutoWired
  private lateinit var appAPI: AppAPI

  fun invoke(context: Context) {
    appAPI.mustImplInApp(context)
  }

}