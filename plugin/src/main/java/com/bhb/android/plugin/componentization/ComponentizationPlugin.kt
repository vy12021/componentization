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
      e.printStackTrace()
      ComponentizationConfig()
    }
    // 不能在此处使用扩展配置，需要至少在下一条任务中才能使用，此处扩展属性dsl并没有读取和装载
    android?.apply {
      println(">>>>>>>>>>>>>>>>>>>>>>>>注册扫描器ComponentScanner<<<<<<<<<<<<<<<<<<<<<<<<<")
      registerTransform(ComponentScanner(this, config))
      project.afterEvaluate {
        project.dependencies.apply {
          project.rootProject.subprojects {subProject ->
            if (subProject.name != project.name &&
                    matchProject(config.includeModules, subProject)) {
              println("${project.name} implementation ${subProject.name}")
              add("implementation", subProject)
              config.addModule(subProject.projectDir.absolutePath)
            }
          }
        }
      }
    }
  }

  private fun matchProject(includeModules: Array<String>, subProject: Project): Boolean {
    println("matchProject: ${includeModules.contentToString()}, ${subProject.name}")
    if (subProject.hasProperty(ComponentizationConfig.PROPERTY_MODULE)) {
      (subProject.findProperty(ComponentizationConfig.PROPERTY_MODULE) as? String)?.toBoolean()?.let {
        if (it) return true
      }
    }
    return includeModules.find {moduleName ->
      subProject.name == moduleName || subProject.name.matches(moduleName.toRegex())
    } != null
  }

}
