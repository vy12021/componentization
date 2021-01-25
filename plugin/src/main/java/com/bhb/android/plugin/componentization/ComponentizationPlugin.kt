package com.bhb.android.plugin.componentization

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 组件收集和自动化注入插件
 * Created by Tesla on 2020/09/30.
 */
class ComponentizationPlugin: Plugin<Project> {

  private lateinit var config: ComponentizationConfig

  private val annotationConfig = "com.bhb.android.componentization:compiler:0.4.7.11"
  private val runtimeConfig = "com.bhb.android.componentization:componentization:0.4.7.11"

  override fun apply(project: Project) {
    println(">>>>>>>>>>>>>>>>>>>>>>注册插件ComponentizationPlugin<<<<<<<<<<<<<<<<<<<<<<")
    val android = project.extensions.findByType(AppExtension::class.java)
    config = try {
      project.extensions.create("componentization", ComponentizationConfig::class.java)
    } catch (e: Exception) {
      e.printStackTrace()
      ComponentizationConfig()
    }
    // 不能在此处使用扩展配置，需要至少在下一条任务中才能使用，此处扩展属性dsl并没有读取和装载
    android?.apply {
      registerTransform(ComponentScanner(this, config))
      project.afterEvaluate {_ ->
        injectDependency(project)
        project.rootProject.subprojects {subProject ->
          if (subProject.name == project.name ||
                  !matchProject(config.includeModules, subProject)) {
            return@subprojects
          }
          println("${project.name} implementation ${subProject.name}")
          project.dependencies.add("implementation", subProject)
          subProject.afterEvaluate {
            injectDependency(it)
          }
          config.addModuleDir(subProject.projectDir.absolutePath)
        }
      }
    }
  }

  private fun injectDependency(project: Project) {
    project.dependencies.add("implementation", runtimeConfig)
    if (project.pluginManager.hasPlugin("kotlin-kapt")) {
      project.dependencies.add("kapt", annotationConfig)
      println("injectDependency: ${project.name}---> kapt $annotationConfig")
    }
    project.dependencies.add("annotationProcessor", annotationConfig)
    println("injectDependency: ${project.name}---> annotationProcessor $annotationConfig")
  }

  private fun matchProject(includeModules: Array<String>, subProject: Project): Boolean {
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
