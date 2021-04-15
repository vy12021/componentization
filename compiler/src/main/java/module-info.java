module com.bhb.android.componentization.compiler {
  exports com.bhb.android.componentization.compiler;
  requires com.squareup.javapoet;
  requires com.google.auto.common;
  requires com.google.auto.service;
  requires java.base;
  requires net.ltgt.gradle.incap;
  requires org.jetbrains.annotations;
  requires com.bhb.android.componentization.annotation;
  requires jdk.compiler;
}