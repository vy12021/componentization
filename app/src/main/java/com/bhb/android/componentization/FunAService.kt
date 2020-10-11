package com.bhb.android.componentization

import android.content.Context
import android.widget.Toast
import com.bhb.android.componentization.library.AppAPI

@Service
class FunAService: FunAAPI, AppAPI {

  override fun mustImplInApp(context: Context) {
    Toast.makeText(context, "class FunAService: mustImplInApp", Toast.LENGTH_SHORT).show()
  }

}