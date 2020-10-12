package com.bhb.android.componentization.library

import android.util.Log
import com.bhb.android.componentization.Service

@Service
object LibraryService: LibraryAPI {
  override fun showLibrary() {
    Log.e("LibraryService", "showLibrary()")
  }
}