package com.bhb.android.plugin.componentization

import com.android.build.api.transform.*
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import java.io.File
import java.io.IOException
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Android插件提供的资源转换器
 */
class ComponentScanner: Transform() {

  companion object {
    private const val PACKAGE = "com.bhb.android.componentization"
    private const val COMPONENTIZATION = "${PACKAGE}.Componentization"
    private const val REGISTER_ITEM = "${PACKAGE}.ComponentRegister\$Item"
    private const val ANNOTATION_API = "${PACKAGE}.Api"
    private const val ANNOTATION_SERVICE = "${PACKAGE}.Service"
    private const val ANNOTATION_AUTOWIRED = "${PACKAGE}.AutoWired"
  }

  private val registers = mutableSetOf<CtClass>()

  private lateinit var outputProvider: TransformOutputProvider

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
    val startTime = System.currentTimeMillis()
    super.transform(transformInvocation)
    outputProvider = transformInvocation.outputProvider.apply {
      deleteAll()
    }
    val classPool = ClassPool(true)
    var componentizationJarInput: JarInput? = null
    val inputs = mutableListOf<QualifiedContent>()
    transformInvocation.inputs.forEach input@{ input ->
      input.jarInputs.forEach jarInput@{ jarInput ->
        if (jarInput.status == Status.REMOVED) {
          return@jarInput
        }
        classPool.appendClassPath(jarInput.file.absolutePath)
        if (null == componentizationJarInput && null != classPool.getOrNull(COMPONENTIZATION)) {
          componentizationJarInput = jarInput
          println("查找到组件管理类: $COMPONENTIZATION")
          return@jarInput
        }
        inputs.add(jarInput)
      }
      input.directoryInputs.forEach dirInput@{ dirInput ->
        classPool.appendClassPath(dirInput.file.absolutePath)
        inputs.add(dirInput)
      }
    }
    if (null == componentizationJarInput) {
      throw RuntimeException("没有查找到组件工具：$COMPONENTIZATION")
    }
    inputs.forEach input@{input ->
      println("找到资源：${input.file.absolutePath}")
      if (input is JarInput) {
        val jarOutput = getOutput(input)
        if (input.file.name == "classes.jar") {
          transformComponentsFromJar(classPool, input).apply {
            if (false && isNotEmpty()) {
              repackageJar(classPool, input,  jarOutput, this)
              return@input
            }
          }
        }
        input.file.copyTo(jarOutput)
      } else if (input is DirectoryInput) {
        val dirOutput = getOutput(input)
        // 目录先copy，然后覆盖被修改的类
        input.file.copyRecursively(dirOutput)
        // 兼容java的classes目录和kotlin的kotlin-classes目录
        if (input.file.name == "classes" || input.file.parentFile.name == "kotlin-classes") {
          println("transformComponentsFromDir: ${input.file.absolutePath} -> ${dirOutput.absolutePath}")
          transformComponentsFromDir(classPool, input).forEach { clazz ->
            clazz.writeFile(dirOutput.absolutePath)
            println("\twrite class: ${clazz.name}")
          }
        }
      }
    }
    // checkRegisterValid(classPool)
    componentizationJarInput!!.apply {
      /*repackageJar(classPool, this, getOutput(this),
              listOf(transformComponentizationJar(classPool, this)))*/
    }
    println("扫描组件耗时：${(System.currentTimeMillis() - startTime) / 1000f}秒")
    classPool.clearImportedPackages()
  }

  private fun getOutput(content: QualifiedContent)
  = outputProvider.getContentLocation(content.name, content.contentTypes, content.scopes,
          if (content is JarInput) Format.JAR else Format.DIRECTORY)

  /**
   * 从jar文件中处理，由依赖模块触发
   * @return 返回被修改过的类
   */
  @Throws(IOException::class, ClassNotFoundException::class)
  private fun transformComponentsFromJar(classPool: ClassPool,
                                         jarInput: JarInput): List<CtClass> {
    println("collectComponentsFromJar: ${jarInput.file.absolutePath}")
    val transformedClasses = mutableListOf<CtClass>()
    JarFile(jarInput.file).use {
      it.entries().toList().forEach { entry ->
        val classEntryName: String = entry.name
        if (!classEntryName.endsWith(".class")) {
          return@forEach
        }
        println("\tclass file: $classEntryName")
        collectComponentRegister(classPool, classEntryName)
        transformComponentInject(classPool, classEntryName)?.let { transformedClass ->
          transformedClasses.add(transformedClass)
        }
      }
    }
    return transformedClasses
  }

  /**
   * 从class目录中处理，由当前模块触发
   * @return 返回被修改过的类
   */
  @Throws(IOException::class, ClassNotFoundException::class)
  private fun transformComponentsFromDir(classPool: ClassPool,
                                         dirInput: DirectoryInput): List<CtClass> {
    println("collectComponentsFromDir: ${dirInput.file.absolutePath}")
    val transformedClasses = mutableListOf<CtClass>()
    getAllFiles(dirInput.file).forEach { classFile ->
      val classEntryName: String = classFile.absolutePath
              .substring(dirInput.file.absolutePath.length + 1)
              .replace("\\", "/")
      if (!classEntryName.endsWith(".class")) {
        return@forEach
      }
      println("\tclass file: $classEntryName")
      collectComponentRegister(classPool, classEntryName)
      transformComponentInject(classPool, classEntryName)?.let { transformedClass ->
        transformedClasses.add(transformedClass)
      }
    }
    return transformedClasses
  }

  /**
   * 从类资源路径转换为CtClass
   */
  private fun getCtClassFromClassEntry(classPool: ClassPool, classEntryName: String): CtClass {
    val index = classEntryName.indexOf(".class")
    if (index >= 0) {
      return classPool.get(classEntryName.substring(0, index).replace("/", "."))
    }
    return classPool.get("java.lang.Object")
  }

  /**
   * 收集组件注册器
   */
  private fun collectComponentRegister(classPool: ClassPool, classEntryName: String) {
    val ctClass = getCtClassFromClassEntry(classPool, classEntryName)
    if (ctClass.packageName != PACKAGE) {
      return
    }
    if (!ctClass.simpleName.endsWith("_Register")) {
      return
    }
    registers.add(ctClass)
  }

  /**
   * 处理组件注入
   * @return 返回被修改过的类
   */
  private fun transformComponentInject(classPool: ClassPool, classEntryName: String): CtClass? {
    val Componentization = classPool.get(COMPONENTIZATION)
    val AutoWired = classPool.get(ANNOTATION_AUTOWIRED)
    val ctClass = getCtClassFromClassEntry(classPool, classEntryName)
    var hasChanged = false
    ctClass.declaredFields.filter { it.hasAnnotation(AutoWired.name) }.forEach {field ->
      println("\ttransformComponentInject: ${ctClass.name} --> ${field.name}")
      ctClass.removeField(field)
      ctClass.addField(field,
          CtField.Initializer.byExpr("${Componentization.name}.getSafely(${field.type.name}.class)")
      )
      if (ctClass.isKotlin) {
        // todo 延迟初始化实现
      }
      hasChanged = true
    }
    return if (hasChanged) ctClass else null
  }

  /**
   * 转换Componentization所属jar资源
   */
  private fun transformComponentizationJar(classPool: ClassPool, jarInput: JarInput): CtClass {
    println("transformComponentizationJar: ${jarInput.file.absolutePath}")
    val Componentization = classPool.get(COMPONENTIZATION)
    val registerBody = StringBuilder("{\n")
    registers.forEach {
      registerBody.append("register(${it.name}.class);\n")
      println("\tinsert ComponentRegister: ${it.name}.class")
    }
    registerBody.append("}")
    (Componentization.classInitializer?: Componentization.makeClassInitializer())
        .setBody(registerBody.toString())
    return Componentization
  }

  /**
   * 重新打jar
   * @param jarInput  输入的jar包
   * @param transformClassed jar包中被转换过的class
   */
  private fun repackageJar(classPool: ClassPool,
                           jarInput: JarInput, jarOutput: File,
                           transformClassed: List<CtClass>) {
    println("repackageJar: ${jarInput.file.absolutePath} -> ${jarOutput.absolutePath}")
    JarFile(jarInput.file).use {jarFile ->
      JarOutputStream(jarOutput.outputStream()).use {jarOs ->
        jarFile.entries().toList().forEach {entry ->
          jarFile.getInputStream(entry).use {entryIs ->
            val zipEntry = ZipEntry(entry.name)
            val clazz = getCtClassFromClassEntry(classPool, entry.name)
            if (transformClassed.contains(clazz)) {
              println("\twrite class: ${clazz.name}")
              jarOs.putNextEntry(zipEntry)
              jarOs.write(clazz.toBytecode())
            } else {
              jarOs.putNextEntry(zipEntry)
              jarOs.write(entryIs.readBytes())
            }
            jarOs.closeEntry()
          }
        }
      }
    }
  }

  /**
   * 检查注册器和组件的正确性，例如：一个api接口不能同时有两个service实现
   */
  private fun checkRegisterValid(classPool: ClassPool) {
    val RegisterItem = classPool.get(REGISTER_ITEM).toClass()
    val api2ServiceMap = mutableMapOf<String, String>()
    registers.forEach {registerCls ->
      registerCls.toClass().run {
        getDeclaredMethod("register").invoke(newInstance()).run {
          val serviceCls = RegisterItem.getDeclaredField("apis").get(this) as Class<Any>
          (RegisterItem.getDeclaredField("apis").get(this) as List<Class<Any>>).forEach {apiCls ->
            api2ServiceMap.put(apiCls.canonicalName, serviceCls.canonicalName)?.let {existService ->
              throw java.lang.RuntimeException(
                  "对于${apiCls.simpleName}找到重复实现：${existService}和${serviceCls.simpleName}")
            }
          }
        }
      }
    }
  }

  /**
   * 获取目录下所有子文件
   */
  private fun getAllFiles(rootDir: File): List<File> {
    val files = rootDir.listFiles()
    if (null == files || files.isEmpty()) {
      return emptyList()
    }
    val results = mutableListOf<File>()
    for (child in files) {
      if (child.isFile) {
        results.add(child)
      } else {
        results.addAll(getAllFiles(child))
      }
    }
    return results
  }

}