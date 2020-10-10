package com.bhb.android.plugin.componentization

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 组件收集和自动化注入插件
 */
class ComponentizationPlugin: Plugin<Project> {

  override fun apply(project: Project) {
    println("注册插件 ComponentRegisterScanner")
    val android = project.extensions.findByType(AppExtension::class.java)
    android?.apply {
      println("注册扫描器 ComponentRegisterScanner")
      registerTransform(ComponentScanner())
    }
  }

}
