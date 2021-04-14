package com.bhb.android.app

import com.bhb.android.componentization.API
import com.bhb.android.componentization.annotation.Api

@Api(dynamic = true)
interface DynamicAPI : API {

  fun abcd()

}