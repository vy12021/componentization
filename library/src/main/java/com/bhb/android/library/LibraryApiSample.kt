package com.bhb.android.library

import android.content.Context
import com.bhb.android.componentization.annotation.AutoWired

class LibraryApiSample {

  @AutoWired
  private lateinit var appAPI: AppAPI

  fun invoke(context: Context) {
    appAPI.mustImplInApp(context)
  }

}