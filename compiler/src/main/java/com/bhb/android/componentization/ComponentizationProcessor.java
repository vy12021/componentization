package com.bhb.android.componentization;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;

import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * 组件相关注解处理
 * Created by Tesla on 2019/12/20.
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.DYNAMIC)
public final class ComponentizationProcessor extends AbstractProcessor {

  private static final TypeName ArrayListType = TypeName.get(ArrayList.class);
  private static final String PACKAGE = "com.bhb.android.componentization";
  private static final TypeName ComponentRegister = ClassName.get(
          PACKAGE, "ComponentRegister");
  private static final TypeName RegisterItem = ClassName.get(
          PACKAGE, "ComponentRegister.Item");
  private static final TypeName APIType = ClassName.get(
          PACKAGE, "API");

  private Types typeUtils;
  private Filer filer;
  private @Nullable Trees trees;
  private Messager logger;

  @Override
  public synchronized void init(ProcessingEnvironment env) {
    super.init(env);

    typeUtils = env.getTypeUtils();
    logger = env.getMessager();
    filer = env.getFiler();
    try {
      trees = Trees.instance(processingEnv);
    } catch (IllegalArgumentException ignored) {
      try {
        // Get original ProcessingEnvironment from Gradle-wrapped one or KAPT-wrapped one.
        for (Field field : processingEnv.getClass().getDeclaredFields()) {
          if (field.getName().equals("delegate") || field.getName().equals("processingEnv")) {
            field.setAccessible(true);
            ProcessingEnvironment javacEnv = (ProcessingEnvironment) field.get(processingEnv);
            trees = Trees.instance(javacEnv);
            break;
          }
        }
      } catch (Throwable ignored2) {
      }
    }
  }

  @Override public Set<String> getSupportedOptions() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    if (trees != null) {
      builder.add(IncrementalAnnotationProcessorType.ISOLATING.getProcessorOption());
    }
    return builder.build();
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    Set<String> types = new LinkedHashSet<>();
    for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
      types.add(annotation.getCanonicalName());
    }
    return types;
  }

  private Set<Class<? extends Annotation>> getSupportedAnnotations() {
    Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
    annotations.add(Api.class);
    annotations.add(Service.class);
    // annotations.add(AutoWired.class);
    return annotations;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    for (Element element : env.getElementsAnnotatedWith(Service.class)) {
      if (!SuperficialValidation.validateElement(element)) {
        continue;
      }
      try {
        TypeSpec spec = build(element);
        JavaFile file = JavaFile.builder(PACKAGE, spec)
                .addFileComment("此文件为自动生成，用于组件化辅助注册").build();
        file.writeTo(filer);
      } catch (Exception e) {
        logger.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        return false;
      }
    }
    return false;
  }

  private TypeSpec build(Element element) {
    return TypeSpec.classBuilder(element.getSimpleName() + "_Register")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ComponentRegister)
            .addMethod(buildRegisterMethod(element))
            .addField(buildRegisterField(element))
            .build();
  }

  private FieldSpec buildRegisterField(Element element) {
    Type serviceType = ((Symbol.ClassSymbol) element).asType();
    FieldSpec.Builder builder = FieldSpec.builder(
            ClassName.get(String.class), "meta",
            Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC);
    List<Type> apisType = getApiTypes(element);
    CodeBlock.Builder coder = CodeBlock.builder();
    coder.add("\"" + TypeName.get(serviceType).toString() + ";");
    Type api;
    for (int i = 0, len = apisType.size(); i < len; i++) {
      api = apisType.get(i);
      coder.add(TypeName.get(api).toString());
      if (i < len - 1) {
        coder.add(",");
      }
    }
    coder.add("\"");
    builder.initializer(coder.build());
    return builder.build();
  }

  /**
   * 获取Service中代表的所有实现接口
   * @param element Service元素
   * @return 接口列表
   */
  private List<Type> getApiTypes(Element element) {
    Type serviceType = ((Symbol.ClassSymbol) element).asType();
    List<Type> interfaces = ((Symbol.ClassSymbol) element).getInterfaces();
    List<Type> apisType = new ArrayList<>(interfaces.size());
    for (Type itf : interfaces) {
      if (null != itf.asElement().getAnnotation(Api.class)) {
        boolean isAPIType = false;
        for (Type sitf : ((Symbol.ClassSymbol) itf.tsym).getInterfaces()) {
          if (APIType.toString().equals(sitf.toString())) {
            isAPIType = true;
          }
        }
        if (!isAPIType) {
          throw new RuntimeException(itf.toString() + "的父接口必须为API类型");
        }
        apisType.add(itf);
      }
    }
    if (apisType.isEmpty()) {
      throw new RuntimeException(serviceType.toString() + "的父接口中必须至少有一个被Api注解修饰");
    }
    return apisType;
  }

  private MethodSpec buildRegisterMethod(Element element) {
    MethodSpec.Builder builder =  MethodSpec.methodBuilder("register")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(RegisterItem);
    Type serviceType = ((Symbol.ClassSymbol) element).asType();
    List<Type> apisType = getApiTypes(element);
    CodeBlock.Builder coder = CodeBlock.builder();
    coder.addStatement("final $T<Class<? extends $T>> apis = new $T<>($L)",
            ArrayListType, APIType, ArrayListType, apisType.size());
    for (Type api : apisType) {
      coder.addStatement("apis.add($T.class)", TypeName.get(api));
    }
    builder.addCode(coder.build());
    builder.addStatement("return new $T(apis, $T.class)", RegisterItem, TypeName.get(serviceType));
    return builder.build();
  }

}