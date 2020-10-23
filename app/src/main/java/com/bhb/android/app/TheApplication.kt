package com.bhb.android.app

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.bhb.android.componentization.Service

@Service
class TheApplication: Application(), ContextAPI {

  companion object {

    private lateinit var INSTANCE: TheApplication

  }

  init {
    INSTANCE = this
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