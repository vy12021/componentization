package com.bhb.android.componentization

import androidx.appcompat.app.AppCompatActivity

class MainActivity: AppCompatActivity() {

  @AutoWired(lazy = true)
  lateinit var funAAPI: FunAAPI

}