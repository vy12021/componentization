package com.bhb.android.componentization

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bhb.android.componentization.library.AppAPI

class MainActivity: AppCompatActivity() {

  @AutoWired(lazy = true)
  lateinit var funAAPI: AppAPI
  @AutoWired
  lateinit var contextAPI: ContextAPI

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)
    findViewById<View>(android.R.id.content).setOnClickListener {
      contextAPI.toast("I'm ContextAPI")
    }
  }

}