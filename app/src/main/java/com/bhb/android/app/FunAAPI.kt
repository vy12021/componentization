package com.bhb.android.app

import com.bhb.android.componentization.API
import com.bhb.android.componentization.Api

@Api
interface FunAAPI: API {

  fun aaaa()

  fun bbb()

  fun ccc(string: String): Boolean

}