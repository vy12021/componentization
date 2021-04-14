package com.bhb.android.componentization.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动注入被{@link Service}标记过的组件
 * Created by Tesla on 2020/09/18.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoWired {

  /**
   * 是否延迟初始化，具体实现参照LazyDelegate
   * @return 默认延迟初始化
   */
  boolean lazy() default true;

}
