package com.bhb.android.componentization;

import java.util.List;

/**
 * 组件注册接口
 * Created by Tesla on 2020/09/22.
 */
interface ComponentRegister {

  /**
   * 生成的代理类后缀
   */
  String SUFFIX = "_Register";

  Item register();

  class Item {
    final List<Class<? extends API>> apis;
    final Class<? extends API> service;

    Item(List<Class<? extends API>> apis, Class<? extends API> service) {
      this.apis = apis;
      this.service = service;
    }
  }

}
