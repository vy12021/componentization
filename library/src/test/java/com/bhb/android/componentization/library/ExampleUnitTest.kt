package com.bhb.android.componentization.library

import javassist.ClassPool
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    val classPool = ClassPool.getDefault()
    classPool.get("com.bhb.android.componentization.library.LibraryApiSample").declaredFields.forEach {
      it.annotations.forEach {

      }
    }
  }
}