package com.bhb.android.app

import com.bhb.android.componentization.API
import java.io.Serializable

interface AbstractAPI<out T: Serializable>: API {

  fun abstractA(): T

}