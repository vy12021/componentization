package com.bhb.android.componentization;

/**
 * 延迟初始化代理类接口
 *
 * @param <C> 被代理类
 */
public interface LazyDelegate<C extends API> {

  /**
   * 创建实例对象
   * @return C
   */
  C create();

  /**
   * 获取实例对象
   * @return C
   */
  C get();

}
