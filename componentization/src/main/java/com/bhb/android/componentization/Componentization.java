package com.bhb.android.componentization;

import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 组件入口调用
 * Created by Tesla on 2020/09/22.
 */
public class Componentization {

  /**
   * 收集到的组件注册信息
   */
  private static Map<Class<? extends API>, API> sComponents = new HashMap<>();

  /**
   * 注册所有收集到的组件
   */
  public static void register() {

  }

  /**
   * 自动绑定当前实例中的公共组件变量
   * @param target 实例
   */
  public static void bind(Object target) {

  }

  /**
   * 获取组件缓存
   * @return registers
   */
  public static Map<Class<? extends API>, API> getComponents() {
    return Collections.unmodifiableMap(sComponents);
  }
}
