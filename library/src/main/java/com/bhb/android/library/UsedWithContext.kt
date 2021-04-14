package com.bhb.android.library

import android.content.Context
import com.bhb.android.componentization.annotation.AutoWired

class UsedWithContext(context: Context) {

  @AutoWired
  private lateinit var libraryAPI: LibraryAPI

}