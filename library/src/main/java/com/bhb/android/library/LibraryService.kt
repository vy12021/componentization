package com.bhb.android.library

import android.util.Log
import com.bhb.android.componentization.Service

@Service
object LibraryService: LibraryAPI {
  override fun showLibrary() {
    Log.e("LibraryService", "showLibrary()")
  }
}