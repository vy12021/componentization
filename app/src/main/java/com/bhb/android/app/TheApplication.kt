package com.bhb.android.app

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.bhb.android.componentization.annotation.AutoWired
import com.bhb.android.componentization.annotation.Provider
import com.bhb.android.componentization.annotation.Service

@Service
class TheApplication: Application(), ContextAPI {

  @AutoWired(lazy = false)
  private lateinit var funAAPI: FunAAPI

  companion object {

    @Provider
    private lateinit var aaa: TheApplication

  }

  init {
    aaa = this
  }

  override fun getApplication(): Application {
    return this
  }

  override fun toast(msg: String?) {
    Toast.makeText(application, msg, Toast.LENGTH_SHORT).show()
  }

  override fun attachBaseContext(base: Context?) {
    super.attachBaseContext(base)
  }

  override fun onCreate() {
    super.onCreate()
  }

}