package com.bhb.android.plugin.componentization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 组件扫描配置
 * 通过"componentization"扩展dsl来注入配置
 * Created by Tesla on 2020/09/30.
 */
public class ComponentizationConfig {

  /**
   * 表示支持组件化扫描的模块属性文件中定义, value为{@link Boolean}
   */
  static final String PROPERTY_MODULE = "componentization.enable";

  /**
   * 是否调试模式，会打印一些详细日志
   */
  boolean debugMode = false;
  /**
   * 是否增量模式
   */
  boolean incremental = false;
  /**
   * 应用模块名称，预设置项，方面某些前置配置
   */
  String applicationModule = "app";
  /**
   * 支持的导入module名称(正则)
   */
  String[] includeModules = new String[0];
  /**
   * 支持扫描的jar包
   */
  String[] includeJars = new String[0];
  /**
   * 扫描包名列表，长度不为空表示启用白名单
   */
  String[] includePackages = new String[0];
  /**
   * 忽略扫描包列表，如果白名单命中，则跳过黑名单
   */
  String[] excludePackages = new String[0];
  /**
   * 被导入的模块目录
   */
  List<String> modulesDir = new ArrayList<>();
  /**
   * 资源目录
   */
  String resourcesDir = "src/main/resources";

  public String[] getIncludeModules() {
    return includeModules;
  }

  public void setIncludeModules(String[] includeModules) {
    this.includeModules = includeModules;
  }

  public String[] getIncludeJars() {
    return includeJars;
  }

  public void setIncludeJars(String[] includeJars) {
    this.includeJars = includeJars;
  }

  public String[] getIncludePackages() {
    return includePackages;
  }

  public void setIncludePackages(String[] includePackages) {
    this.includePackages = includePackages;
  }

  public String[] getExcludePackages() {
    return excludePackages;
  }

  public void setExcludePackages(String[] excludePackages) {
    this.excludePackages = excludePackages;
  }

  public boolean isDebugMode() {
    return debugMode;
  }

  public void setDebugMode(boolean debugMode) {
    this.debugMode = debugMode;
  }

  public boolean isIncremental() {
    return incremental;
  }

  public void setIncremental(boolean incremental) {
    this.incremental = incremental;
  }

  public String getResourcesDir() {
    return resourcesDir;
  }

  public void setResourcesDir(String resourcesDir) {
    this.resourcesDir = resourcesDir;
  }

  void addModuleDir(String... modules) {
    this.modulesDir.addAll(Arrays.asList(modules));
  }

  public String getApplicationModule() {
    return applicationModule;
  }

  public void setApplicationModule(String applicationModule) {
    this.applicationModule = applicationModule;
  }

  @Override
  public String toString() {
    return "ComponentizationConfig{" +
            "debugMode=" + debugMode +
            ", incremental=" + incremental +
            ", resourcesDir=" + resourcesDir +
            ", applicationModule=" + applicationModule +
            ", includeModules=" + Arrays.toString(includeModules) +
            ", includeJars=" + Arrays.toString(includeJars) +
            ", includePackages=" + Arrays.toString(includePackages) +
            ", excludePackages=" + Arrays.toString(excludePackages) +
            ", modulesDir=" + modulesDir +
            '}';
  }
}
