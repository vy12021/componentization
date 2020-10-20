package com.bhb.android.plugin.componentization;

/**
 * 组件扫描配置
 * 通过"componentization"扩展dsl来注入配置
 */
public class ComponentizationConfig {

  /**
   * 是否调试模式，会打印一些详细日志
   */
  boolean debugMode = false;
  /**
   * 扫描包名列表，长度不为空表示启用白名单
   */
  String[] includes = new String[0];
  /**
   * 忽略扫描包列表，如果白名单命中，则跳过黑名单
   */
  String[] excludes = new String[0];

  public String[] getIncludes() {
    return includes;
  }

  public void setIncludes(String[] includes) {
    this.includes = includes;
  }

  public String[] getExcludes() {
    return excludes;
  }

  public void setExcludes(String[] excludes) {
    this.excludes = excludes;
  }

  public boolean isDebugMode() {
    return debugMode;
  }

  public void setDebugMode(boolean debugMode) {
    this.debugMode = debugMode;
  }



}
