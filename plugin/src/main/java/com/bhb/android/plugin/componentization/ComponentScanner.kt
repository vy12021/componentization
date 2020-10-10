package com.bhb.android.plugin.componentization

import com.android.build.api.transform.*
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import java.io.File
import java.io.IOException
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Android插件提供的资源转换器
 */
class ComponentScanner: Transform() {

  private val classPool by lazy { ClassPool.getDefault() }

  private val PACKAGE = "com.bhb.android.componentization"
  private lateinit var Componentization: CtClass
  private lateinit var RegisterMethod: CtMethod
  private val registers by lazy { mutableSetOf<CtClass>() }

  override fun getName() = "ComponentScanner"

  override fun getInputTypes(): MutableSet<QualifiedContent.ContentType>
          = mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)

  override fun getScopes(): MutableSet<in QualifiedContent.Scope> = mutableSetOf(
          // 扫描引入的componentization相关依赖，导入到classpath中
          QualifiedContent.Scope.EXTERNAL_LIBRARIES,
          // 扫描子工程模块，导入到classpath中
          QualifiedContent.Scope.SUB_PROJECTS,
          // 扫描当前插件工程，导入到classpath中
          QualifiedContent.Scope.PROJECT,
  )

  override fun isIncremental() = true

  override fun transform(transformInvocation: TransformInvocation) {
    super.transform(transformInvocation)
    var componentizationJarInput: JarInput? = null
    val startTime = System.currentTimeMillis()
    transformInvocation.inputs.forEach { input ->
      input.jarInputs.forEach { jar ->
        classPool.appendClassPath(jar.file.absolutePath)
        if (null == componentizationJarInput &&
                null != classPool.getOrNull("${PACKAGE}.Componentization")) {
          Componentization = classPool.get("${PACKAGE}.Componentization")
          RegisterMethod = Componentization.getDeclaredMethod("register")
          componentizationJarInput = jar
          println("查找到组件管理类: ${Componentization.name}")
        } else {
          collectComponentsFromJar(JarFile(jar.file))
          jar.file.copyRecursively(getOutput(transformInvocation.outputProvider, jar))
        }
      }
      input.directoryInputs.forEach { dir ->
        classPool.appendClassPath(dir.file.absolutePath)
        collectComponentsFromDir(dir.file)
        dir.file.copyRecursively(getOutput(transformInvocation.outputProvider, dir))
      }
    }
    if (null == componentizationJarInput || !this::Componentization.isInitialized) {
      throw RuntimeException("没有查找到组件工具：${PACKAGE}.Componentization")
    }
    val registerLines = StringBuilder("{\n")
    registers.forEach {
      registerLines.append("register(${it.name}.class);\n")
      println("insert ComponentRegister: ${it.name}.class")
    }
    registerLines.append("}")
    RegisterMethod.setBody(registerLines.toString())
    transformComponentJar(JarFile(componentizationJarInput!!.file),
            getOutput(transformInvocation.outputProvider, componentizationJarInput!!))
    println("扫描组件耗时：${(System.currentTimeMillis() - startTime) / 1000}秒")
  }

  private fun getOutput(outputProvider: TransformOutputProvider, content: QualifiedContent)
  = outputProvider.getContentLocation(content.name, content.contentTypes, content.scopes,
          if (content is JarInput) Format.JAR else Format.DIRECTORY)

  /**
   * 从jar文件中扫描组件
   */
  @Throws(IOException::class, ClassNotFoundException::class)
  fun collectComponentsFromJar(jarFile: JarFile) {
    println("collectComponentsFromJar: ${jarFile.name} =================>")
    val jarEntryEnumeration: Enumeration<JarEntry> = jarFile.entries()
    while (jarEntryEnumeration.hasMoreElements()) {
      val entry: JarEntry = jarEntryEnumeration.nextElement()
      val classEntryName: String = entry.name
      println("class file: $classEntryName")
      if (!classEntryName.contains(".class") ||
              !classEntryName.replace("/", ".").startsWith(PACKAGE)) {
        continue
      }
      val className = classEntryName.substring(0, classEntryName.indexOf(".class"))
              .replace("/", ".")
      if (!className.endsWith("_Register", true)) {
        continue
      }
      registers.add(classPool.get(className))
    }
  }

  /**
   * 从目录中扫描组件
   */
  @Throws(IOException::class, ClassNotFoundException::class)
  fun collectComponentsFromDir(classPath: File) {
    println("collectComponentsFromDir: ${classPath.absolutePath} =================>")
    getAllFiles(classPath).forEach { classFile ->
      val classEntryName: String = classFile.absolutePath
              .substring(classPath.absolutePath.length + 1)
              .replace("\\", "/")
      println("class file: $classEntryName")
      if (!classEntryName.contains(".class") ||
              !classEntryName.replace("/", ".").startsWith(PACKAGE)) {
        return@forEach
      }
      val className = classEntryName.substring(0, classEntryName.indexOf(".class"))
              .replace("/", ".")
      if (!className.endsWith("_Register", true)) {
        return@forEach
      }
      registers.add(classPool.get(className))
    }
  }

  /**
   * 获取目录下所有子文件
   *
   * @param rootDir 查找根目录
   */
  private fun getAllFiles(rootDir: File): List<File> {
    val files = rootDir.listFiles()
    if (null == files || files.isEmpty()) {
      return emptyList()
    }
    val results: MutableList<File> = ArrayList()
    for (child in files) {
      if (child.isFile) {
        results.add(child)
      } else {
        results.addAll(getAllFiles(child))
      }
    }
    return results
  }

  private fun transformComponentJar(jarInput: JarFile, jarOutput: File) {
    val jos = JarOutputStream(jarOutput.outputStream())
    jarInput.entries().toList()
            .forEach {
              val inputStream = jarInput.getInputStream(it)
              val zipEntry = ZipEntry(it.name)
              if (it.name.endsWith(".class")) {
                jos.putNextEntry(zipEntry)
                jos.write(Componentization.toBytecode())
              } else {
                jos.putNextEntry(zipEntry)
                jos.write(inputStream.readBytes())
              }
              jos.closeEntry()
              inputStream.close()
            }
    jos.close()
    jarInput.close()
  }

}