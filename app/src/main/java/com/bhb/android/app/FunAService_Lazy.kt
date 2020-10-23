package com.bhb.android.app

import android.content.Context
import android.widget.Toast
import com.bhb.android.componentization.library.AppAPI

class FunAService_Lazy: FunAAPI, AppAPI {

  override fun mustImplInApp(context: Context) {
    Toast.makeText(context, "class FunAService: mustImplInApp", Toast.LENGTH_SHORT).show()
  }

  override fun aaaa() {
  }

  override fun bbb() {
  }

  override fun ccc(string: String): Boolean {
    return true
  }

}