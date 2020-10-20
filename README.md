# Componentization
组件化相关上下文自动化注册声明和使用注入插件

### 目的
1. 使用这种方式用来更好的面向接口编程
2. 也可以作为注解和插件开发的示例学习

### 模块说明

#### 功能模块
1. `annotation`: 定义需要使用的相关注解
2. `componentization`: 开发者调用组件的管理类，一般情况不需要处理，基本过程已经通过注解和插件完成。
3. `compiler`: 用于生产上下文注册器，简化注册流程，并检测一些注解相关的声明原则，声明到使用注解的模块。
4. `plugin`: 用于扫描收集所有生产的注册器，并写入到固定静态块中注册，达到隐式注册，声明到入口模块即可。

#### 测试模块
1. `app`: 入口模块，插件使用的地方。
2. `lib`: 对于lib类型的处理测试。

#### 接入
1. 在根项目build.gradle文件中配置插件
```groovy
buildscript {
  repositories {
    maven {
      name "plugin"
      url "https://nexus.bhbcode.com/repository/android-plugin/"
      credentials {
        username mavenUsername
        password mavenPassword
      }
      authentication {
        basic(BasicAuthentication)
      }
    }  
  }
  
  dependencies {
    classpath 'com.bhb.android.plugin:componentization:0.3.12'
  }
}
```
2. 在application模块的build.gradle文件应用插件和配置
```groovy
apply plugin: 'componentization'

componentization {
  debugMode = true
  includes = ['com.bhb.android']
  excludes = ['kotlin', 'kotlinx', 'android', 'androidx', 'org.jetbrains']
}
```

### 示例用法
1. 首先定义接口，并使用`Api`注解修饰，接口必须继承自`API`类，通常声明为XXXAPI
```kotlin
@Api
interface LibraryAPI: API {

  fun helloLibrary()

}

// 单例接口
@Api(singleton = true)
interface ApplicationAPI: API {

  fun getContext(): Application

}

```
2. 定义实现类，并使用`Service`注解修饰，通常声明为XXXService
```kotlin
@Service
object LibraryService: LibraryAPI {
  override fun showLibrary() {
    Log.e("LibraryService", "showLibrary()")
  }
}
```

单例实现
```kotlin
class MyApplication: Application(), ApplicationAPI {

  // 注意单例实现
  companion object {

    private lateinit var INSTANCE: MyApplication

  }

  init {
    INSTANCE = this
  }

  override fun getContext(): Application {
    return this
  }

}
```

3. 使用
```kotlin
class MainActivity: AppCompatActivity() {

  @AutoWired(lazy = true)
  lateinit var libraryAPI: LibraryAPI
  @AutoWired
  lateinit var applicationAPI: ApplicationAPI

  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      libraryAPI.showLibrary()
      applicationAPI.getContext()
  }

}
```