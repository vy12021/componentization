package com.bhb.android.componentization.library

import android.content.Context
import com.bhb.android.componentization.API
import com.bhb.android.componentization.Api_

@Api_
interface AppAPI: API {

  fun mustImplInApp(context: Context)

}