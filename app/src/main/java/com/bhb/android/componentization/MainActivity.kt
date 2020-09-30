package com.bhb.android.componentization

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity: AppCompatActivity() {

  @AutoWired(lazy = true)
  lateinit var funAAPI: FunAAPI

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)
    findViewById<View>(android.R.id.content).setOnClickListener {
      Componentization.register()
      Log.e("MainActivity", "=====")
    }
  }

}