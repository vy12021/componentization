package com.bhb.android.app

import android.content.Context
import android.widget.Toast
import com.bhb.android.componentization.AutoWired
import com.bhb.android.componentization.Service
import com.bhb.android.library.AppAPI

@Service
object FunXXService: FunAAPI, AppAPI {

  @AutoWired
  private lateinit var contextAPI: ContextAPI
  @AutoWired
  private lateinit var dynamicAPI: DynamicAPI

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

  override fun abstractA(): String = "我是父接口"
}