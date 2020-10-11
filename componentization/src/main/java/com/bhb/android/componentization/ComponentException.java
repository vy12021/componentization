package com.bhb.android.componentization;

/**
 * 组件没有找到异常
 * Created by Tesla on 2020/10/11.
 */
public class ComponentException extends Exception {

  public ComponentException(String message) {
    super(message);
  }

  public ComponentException(String message, Throwable cause) {
    super(message, cause);
  }
}
