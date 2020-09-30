package com.bhb.android.plugin.componentization

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 组件收集和自动化注入插件
 */
class ComponentizationPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.afterEvaluate {
      def androidPlugin = project.android
      if (null == androidPlugin) {
        return
      }
      
    }
  }

}
