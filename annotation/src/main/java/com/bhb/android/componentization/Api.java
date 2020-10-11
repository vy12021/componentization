package com.bhb.android.componentization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * api接口声明注解，实现类必须被标记{@link Service}标记才可实现自动注册
 * Created by Tesla on 2020/09/22.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Api {

  /**
   * 是否单例模式，且必须存在静态INSTANCE引用，可以和kotlin单例对象保持兼容
   */
  boolean singleton() default false;

}
