package com.bhb.android.componentization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 组件实现声明，最终以父接口形式注册，接口必须包含{@link Api_}声明才会生效
 * Created by Tesla on 2020/09/18.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {
}
