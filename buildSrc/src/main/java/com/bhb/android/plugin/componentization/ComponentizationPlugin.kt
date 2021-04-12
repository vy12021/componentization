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
    private const val OPTION_ROOT_MODULE_DIR = "option.root.module.dir"
    private const val OPTION_APP_MODULE_DIR = "option.app.module.dir"
    private const val OPTION_RESOURCES_DIR = "option.resources.dir"
    private const val OPTION_RESOURCES_OUTPUT_DIR = "option.resources.output.dir"
    private const val RESOURCES_OUTPUT_PREFIX = "build/intermediates/java_res"

    private const val REGISTER_FILE_NAME = "module-register.properties"
  }

  private lateinit var config: ComponentizationConfig

  private val properties by lazy {
    Properties().apply {
      Thread.currentThread().contextClassLoader.getResourceAsStream("artifact.properties")?.run {
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
      it.requireAndroidExt().apply {
        println("Project[${it.name}].registerTransform(${config})")
        registerTransform(ComponentScanner(it))
      }

      if (!it.isApplicationModule()) {
        return@afterEvaluate
      }

      injectDependency(it)
      injectCompileOptions(it)

      it.eachSubProject { subProject ->
        project.addDependency("implementation", subProject)
        subProject.afterEvaluate {
          injectDependency(subProject)
          injectCompileOptions(subProject)
        }
      }

      it.afterEvaluate {_ ->
        // 同步module配置过程同步属性文件
        migrateProperties(it)
        // 注册合并java资源任务
        it.getBuildNames().forEach { buildName ->
          it.tasks.apply {
            register("syncProperties$buildName") { task ->
              task.doLast { _ ->
                migrateProperties(it)
              }
            }
            findByName("merge${buildName}JavaResource")?.apply {
              dependsOn("syncProperties$buildName")
            }
          }
        }
      }
    }
  }

  private fun migrateProperties(project: Project) {
    // 同步module配置过程同步属性文件
    project.rootProject.file(config.resourcesDir).resolve(REGISTER_FILE_NAME).let {rootFile ->
      if (!rootFile.exists()) {
        return@let
      }
      project.getBuildNames().forEach { buildName ->
        project.file(RESOURCES_OUTPUT_PREFIX)
                .resolve(buildName).resolve("out")
                .resolve(REGISTER_FILE_NAME).let {buildFile ->
                  if (!buildFile.exists()) {
                    rootFile.copyTo(buildFile)
                  }
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
    /*project.addDependency("implementation", runtimeConfig)
    project.addProcessor(annotationConfig)*/
  }

  /**
   * 注入编译选项，如果是入口Project则必须在evaluated之前配置
   */
  private fun injectCompileOptions(project: Project) {
    val resourcesDirs = StringBuilder()
    project.getBuildNames().apply {
      forEachIndexed { index, buildName ->
        resourcesDirs.apply {
          append(RESOURCES_OUTPUT_PREFIX).append("/")
          append(buildName).append("/").append("out")
        }
        if (index < size - 1) {
          resourcesDirs.append(",")
        }
      }
    }
    if (config.debugMode) {
      println("Project[${project.name}].injectCompileOptions--->${resourcesDirs}")
    }
    val options = mapOf(
            OPTION_DEBUG_MODE to config.debugMode.toString(),
            OPTION_ROOT_MODULE_DIR to project.rootProject.projectDir.absolutePath,
            OPTION_APP_MODULE_DIR to project.requireApplicationProject().projectDir.absolutePath,
            OPTION_MODULE_NAME to project.name,
            OPTION_RESOURCES_DIR to config.resourcesDir,
            OPTION_RESOURCES_OUTPUT_DIR to resourcesDirs.toString())
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

internal fun Project.getBuildNames(): Set<String> {
  return (requireApplicationProject().requireAndroidExt() as AppExtension).let {androidExt ->
    if (androidExt.applicationVariants.isNotEmpty()) {
      androidExt.applicationVariants.map { it.name }
    } else {
      androidExt.buildTypes.map { it.name }
    }
  }.toSet()
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
