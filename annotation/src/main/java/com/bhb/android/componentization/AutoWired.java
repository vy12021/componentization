package com.bhb.android.componentization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动注入被{@link Component}标记过的组件
 * Created by Tesla on 2020/09/18.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface AutoWired {

  /**
   * 是否延迟初始化，只对kotlin生效，使用by lazy机制
   * @return 默认立即初始化
   */
  boolean lazy() default false;

}
