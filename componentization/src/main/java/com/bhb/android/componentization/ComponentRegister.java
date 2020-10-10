package com.bhb.android.componentization;

import java.util.List;

/**
 * 组件注册接口
 * Created by Tesla on 2020/09/22.
 */
public interface ComponentRegister {

  Item register();

  class Item {
    public final List<Class<? extends API>> apis;
    public final Class<? extends API> service;

    public Item(List<Class<? extends API>> apis, Class<? extends API> service) {
      this.apis = apis;
      this.service = service;
    }
  }

}
