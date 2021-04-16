package com.bhb.android.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bhb.android.componentization.AutoWired
import com.bhb.android.library.AppAPI
import com.bhb.android.library.Library2

class MainActivity: AppCompatActivity() {

  @AutoWired
  private lateinit var funAAPI: AppAPI
  @AutoWired(lazy = true)
  private lateinit var contextAPI: ContextAPI
  @AutoWired
  private lateinit var dynamicAPI: DynamicAPI

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)
    findViewById<View>(android.R.id.content).setOnClickListener {
      contextAPI.toast("I'm ContextAPI")
      dynamicAPI.abcd()
    }
    Library2.invoke(this)
  }
}