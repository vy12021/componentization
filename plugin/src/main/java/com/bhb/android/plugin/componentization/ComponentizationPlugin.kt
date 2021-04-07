package com.bhb.android.plugin.componentization

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
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
    private const val OPTION_DEBUG_MODE = "option.debug.enable"
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

  override fun apply(project: Project) {
    println(">>>>>>>>>>>>>>>>>>>>>>注册插件Componentization[${project.name}]<<<<<<<<<<<<<<<<<<<<<<")
    config = project.getComponentConfig()

    if (project.isRootProject()) {
      project.afterEvaluate {
        project.eachSubProject { subProject ->
          if (config.incremental || subProject.isApplicationModule()) {
            subProject.plugins.apply(ComponentizationPlugin::class.java)
          }
        }
      }
      return
    }

    project.afterEvaluate {
      val android = it.requireAndroidExt().apply {
        println("Project[${it.name}].registerTransform(${config})")
        registerTransform(ComponentScanner(it))
        injectDependency(project)
        injectCompileOptions(it)
      }

      if (!it.isApplicationModule()) {
        return@afterEvaluate
      }

      // 添加资源目录
      android.sourceSets.maybeCreate("main").resources.srcDir(config.resourcesDir)
      it.eachSubProject { subProject ->
        project.addDependency("implementation", subProject)
        subProject.afterEvaluate {
          injectDependency(subProject)
          injectCompileOptions(subProject)
        }
      }
    }
  }

  private fun Project.eachSubProject(iterator: (subProject: Project) -> Unit) {
    subProjects { it.isApplicationModule() || matchProject(config.includeModules, it) }.forEach(iterator)
  }

  private fun Project.isApplicationModule() = isApplication() || config.applicationModule == name

  /**
   * 注入必要依赖
   */
  private fun injectDependency(project: Project) {
    project.addDependency("implementation", runtimeConfig)
    project.addProcessor(annotationConfig)
  }

  /**
   * 注入编译选项，如果是入口Project则必须在evaluated之前配置
   */
  private fun injectCompileOptions(project: Project) {
    val options = mapOf(
            OPTION_DEBUG_MODE to config.debugMode.toString(),
            OPTION_MODULE_NAME to project.name,
            OPTION_PLUGIN_DIR to project.requireApplicationProject().projectDir.absolutePath,
            OPTION_RESOURCES_DIR to config.resourcesDir)
    project.requireAndroidExt().defaultConfig.javaCompileOptions {
      annotationProcessorOptions {
        arguments.putAll(options)
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
    return includeModules.find { moduleName ->
      subProject.name == moduleName || subProject.name.matches(moduleName.toRegex())
    } != null
  }

}

internal fun Project.requireApplicationProject(): Project {
  if (this.isApplication()) return this
  rootProject.subprojects.forEach {
    if (it.isApplication()) {
      return@requireApplicationProject it
    }
  }
  throw IllegalStateException()
}

internal fun Project.requireAndroidExt(): BaseExtension {
  return if (isApplication()) {
    project.extensions.findByType(AppExtension::class.java)
  } else {
    project.extensions.findByType(LibraryExtension::class.java)
  } as BaseExtension
}

internal fun Project.isRootProject() = this == rootProject

internal fun Project.isApplication() = this.pluginManager.hasPlugin("com.android.application")

internal fun Project.isLibrary() = this.pluginManager.hasPlugin("com.android.library")

internal fun Project.hasKaptPlugin() = this.pluginManager.hasPlugin("kotlin-kapt")

internal fun Project.getComponentConfig(): ComponentizationConfig {
  return try {
    // 先查找，如果没有找到再创建，如果创建失败
    rootProject.extensions.let {
      it.findByType(ComponentizationConfig::class.java)
              ?: it.create("componentization", ComponentizationConfig::class.java)
    }
  } catch (e: Exception) {
    e.printStackTrace()
    ComponentizationConfig()
  }
}

internal fun Project.subProjects(filter: (subProject: Project) -> Boolean): Collection<Project> {
  val subProjects = mutableListOf<Project>()
  rootProject.subprojects.forEach { subProject ->
    if (subProject.name == name || !filter(subProject)) {
      return@forEach
    }
    subProjects.add(subProject)
  }
  return subProjects
}

internal fun Project.addDependency(configuration: String, vararg dependencies: Any) {
  dependencies.forEach {
    this.dependencies.add(configuration, it)
  }
}

internal fun Project.addProcessor(vararg dependencies: Any) {
  dependencies.forEach {
    if (hasKaptPlugin()) {
      this.dependencies.add("kapt", it)
    }
    this.dependencies.add("annotationProcessor", it)
  }
}
