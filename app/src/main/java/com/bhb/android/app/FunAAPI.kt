package com.bhb.android.app

import com.bhb.android.componentization.annotation.Api

@Api
interface FunAAPI: AbstractAPI<String> {

  fun aaaa()

  fun bbb()

  fun ccc(string: String): Boolean

}