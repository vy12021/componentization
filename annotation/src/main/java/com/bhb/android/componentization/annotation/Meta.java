package com.bhb.android.componentization.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 关于组件辅助描述信息注解，便于非运行时理解关系，比如插件处理过程
 * Created by Tesla on 2020/11/13.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@interface Meta {

  /**
   * 服务实现类全名
   * @return {@link Class#getName()}
   */
  String service();

  /**
   * 接口类型列表
   * @return {@link Class#getName()}
   */
  String[] api();

}
