package com.bhb.android.componentization

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bhb.android.componentization.library.AppAPI

class MainActivity: AppCompatActivity() {

  @CAutoWired(lazy = true)
  lateinit var funAAPI: AppAPI

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Componentization.register()
    funAAPI = Componentization.getComponents()[AppAPI::class.java] as AppAPI
    setContentView(R.layout.main_activity)
    findViewById<View>(android.R.id.content).setOnClickListener {
      funAAPI.mustImplInApp(applicationContext)
    }
  }

}