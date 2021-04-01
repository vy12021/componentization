package com.bhb.android.plugin.componentization

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import java.util.*

/**
 * 组件收集和自动化注入插件
 * Created by Tesla on 2020/09/30.
 */
class ComponentizationPlugin: Plugin<Project> {

  companion object {
    private const val OPTION_MODULE_NAME = "option.module.name"
    private const val OPTION_PLUGIN_DIR = "option.plugin.module.dir"
    private const val OPTION_RESOURCES_DIR = "option.resources.dir"
  }

  private lateinit var config: ComponentizationConfig

  private val properties by lazy {
    Properties().apply {
      Thread.currentThread().contextClassLoader.getResourceAsStream("artifact.properties")!!.run {
        load(this)
      }
    }
  }

  private val Group by lazy { properties.getProperty("implementation-group", "") }

  private val Version by lazy { properties.getProperty("implementation-version", "") }

  private val annotationConfig = "${Group}:compiler:${Version}"
  private val runtimeConfig = "${Group}:componentization:${Version}"

  private lateinit var applicationProject: Project

  override fun apply(project: Project) {
    applicationProject = project
    println(">>>>>>>>>>>>>>>>>>>>>>注册插件ComponentizationPlugin<<<<<<<<<<<<<<<<<<<<<<")
    val applicationExt = project.extensions.findByType(AppExtension::class.java)!!
    config = try {
      project.extensions.create("componentization", ComponentizationConfig::class.java)
    } catch (e: Exception) {
      e.printStackTrace()
      ComponentizationConfig()
    }
    // 不能在此处使用扩展配置，需要至少在下一条任务中才能使用，此处扩展属性dsl并没有读取和装载
    applicationExt.apply {
      registerTransform(ComponentScanner(this, config))
      // fixme 如果在主项目的afterEvaluate中注入依赖会导致注解处理器失效？？？
      injectDependency(project)
      project.afterEvaluate {
        // 添加资源目录
        // applicationExt.sourceSets.maybeCreate("main").resources.srcDir(config.resourcesDir)
        injectCompileOptions(project)
        project.rootProject.subprojects {subProject ->
          if (subProject.name == project.name ||
                  !matchProject(config.includeModules, subProject)) {
            return@subprojects
          }
          project.addDependency("implementation", subProject)
          subProject.afterEvaluate {
            injectDependency(it)
            injectCompileOptions(subProject)
          }
          config.addModuleDir(subProject.projectDir.absolutePath)
        }
      }
    }
  }

  /**
   * 注入必要依赖
   */
  private fun injectDependency(project: Project) {
    project.addDependency("implementation", runtimeConfig)
    project.addProcessor(annotationConfig)
  }

  /**
   * 注入编译选项
   */
  private fun injectCompileOptions(project: Project) {
    val options = mapOf(
            OPTION_MODULE_NAME to project.name,
            OPTION_PLUGIN_DIR to applicationProject.projectDir.absolutePath,
            OPTION_RESOURCES_DIR to config.resourcesDir
    )
    if (project.isApplication()) {
      project.extensions.findByType(AppExtension::class.java)?.defaultConfig?.apply {
        javaCompileOptions {
          annotationProcessorOptions {
            arguments.putAll(options)
          }
        }
      }
    } else {
      project.extensions.findByType(LibraryExtension::class.java)?.defaultConfig?.apply {
        javaCompileOptions {
          annotationProcessorOptions {
            arguments.putAll(options)
          }
        }
      }
    }
    if (project.hasKaptPlugin()) {
      project.extensions.findByType(KaptExtension::class.java)?.arguments {
        options.forEach { (option, value) ->
          arg(option, value)
        }
      }
    }
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

private fun Project.isApplication() = this.pluginManager.hasPlugin("com.android.application")

private fun Project.isLibrary() = this.pluginManager.hasPlugin("com.android.library")

private fun Project.hasKaptPlugin() = this.pluginManager.hasPlugin("kotlin-kapt")

private fun Project.addDependency(configuration: String, vararg dependencies: Any) {
  dependencies.forEach {
    this.dependencies.add(configuration, it)
    println("injectDependency: ${name}---> $configuration $it")
  }
}

private fun Project.addProcessor(vararg dependencies: Any) {
  dependencies.forEach {
    if (hasKaptPlugin()) {
      this.dependencies.add("kapt", it)
      println("injectDependency: ${name}---> kapt $it")
    }
    this.dependencies.add("annotationProcessor", it)
    println("injectDependency: ${name}---> annotationProcessor $it")
  }
}
