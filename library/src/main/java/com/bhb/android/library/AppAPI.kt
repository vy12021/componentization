package com.bhb.android.library

import android.content.Context
import com.bhb.android.componentization.API
import com.bhb.android.componentization.annotation.Api

@Api
interface AppAPI: API {

  fun mustImplInApp(context: Context)

}