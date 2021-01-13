package com.bhb.android.componentization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记服务创提供者，一般用来在单例中使用，用于指定单例引用
 * 如果标记到方法上，方法返回值必须为API类型，如果标记到属性上，则属性也必须为API类型
 * Created by Tesla on 2020/12/2.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Provider {
}
