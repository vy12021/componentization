module com.bhb.android.componentization.compiler {
  exports com.bhb.android.componentization.compiler;
  requires com.squareup.javapoet;
  requires auto.common;
  requires com.google.auto.service;
  requires java.base;
  requires incap;
  requires annotations;
  requires com.bhb.android.componentization.annotation;
  requires jdk.compiler;
}