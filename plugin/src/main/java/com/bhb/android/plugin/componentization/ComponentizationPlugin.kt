package com.bhb.android.plugin.componentization

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 组件收集和自动化注入插件
 */
class ComponentizationPlugin: Plugin<Project> {

  override fun apply(project: Project) {
    println(">>>>>>>>>>>>>>>>>>>>>>注册插件ComponentizationPlugin<<<<<<<<<<<<<<<<<<<<<<")
    val android = project.extensions.findByType(AppExtension::class.java)
    val config: ComponentizationConfig = try {
      project.extensions.create("componentization", ComponentizationConfig::class.java)
    } catch (e: Exception) {
      ComponentizationConfig()
    } finally {
    }
    println("获取到插件配置：${config}")
    android?.apply {
      println(">>>>>>>>>>>>>>>>>>>>>>>>注册扫描器ComponentScanner<<<<<<<<<<<<<<<<<<<<<<<<<")
      registerTransform(ComponentScanner(this, config))
    }
  }

}
