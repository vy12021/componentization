package com.bhb.android.componentization;

import com.bhb.android.componentization.annotation.Api;

import java.io.Serializable;

@Api
public interface API2<T extends Serializable, N extends Number> extends API {

  <V extends Serializable> T get(V input, N number);

}
