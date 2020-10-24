package com.bhb.android.componentization;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;

import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
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

  private static final String PACKAGE_OUTPUT = "com.bhb.android.componentization";
  private static final String ComponentRegister_SUFFIX = "_Register";
  private static final String LazyDelegate_SUFFIX = "_Lazy";
  private static final String ComponentRegister_Field_META = "meta";
  private static final String LazyDelegate_Field_DELEGATE_SUFFIX = "Delegate";
  private static final TypeName ArrayListType = TypeName.get(ArrayList.class);
  private static final ClassName ComponentRegisterType = ClassName.get(
          PACKAGE_OUTPUT, "ComponentRegister");
  private static final ClassName RegisterItemType = ClassName.get(
          PACKAGE_OUTPUT, "ComponentRegister.Item");
  private static final ClassName APIType = ClassName.get(
          PACKAGE_OUTPUT, "API");
  private static final ClassName LazyDelegateType = ClassName.get(
          PACKAGE_OUTPUT, "LazyDelegate");
  private static final ClassName LazyDelegateImplType = ClassName.get(
          PACKAGE_OUTPUT, "LazyDelegateImpl");

  private Types typeUtils;
  private Filer filer;
  private @Nullable Trees trees;
  private Messager logger;
  private ApiMethodScanner methodScanner = new ApiMethodScanner();

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
    // annotations.add(Api.class);
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
        generateRegisterClassFile(element);
        generateLazyClassFile(element);
      } catch (Exception e) {
        e.printStackTrace();
        logger.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        return false;
      }
    }
    return false;
  }

  /**
   * 生成注册类文件
   * @param element 元素
   * @throws IOException 写入异常
   */
  private void generateRegisterClassFile(Element element) throws IOException {
    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(
            element.getSimpleName() + ComponentRegister_SUFFIX)
            .addModifiers(Modifier.FINAL)
            .addSuperinterface(ComponentRegisterType)
            .addMethod(buildRegisterMethod(element))
            .addField(buildRegisterField(element));
    JavaFile file = JavaFile.builder(PACKAGE_OUTPUT, typeBuilder.build())
            .addFileComment("此文件为自动生成，用于组件化辅助注册").build();
    file.writeTo(filer);
  }

  private FieldSpec buildRegisterField(Element element) {
    Type serviceType = ((Symbol.ClassSymbol) element).asType();
    FieldSpec.Builder builder = FieldSpec.builder(
            ClassName.get(String.class), ComponentRegister_Field_META,
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
            .returns(RegisterItemType);
    Type serviceType = ((Symbol.ClassSymbol) element).asType();
    List<Type> apisType = getApiTypes(element);
    CodeBlock.Builder coder = CodeBlock.builder();
    coder.addStatement("final $T<Class<? extends $T>> apis = new $T<>($L)",
            ArrayListType, APIType, ArrayListType, apisType.size());
    for (Type api : apisType) {
      coder.addStatement("apis.add($T.class)", TypeName.get(api));
    }
    builder.addCode(coder.build());
    builder.addStatement("return new $T(apis, $T.class)", RegisterItemType, TypeName.get(serviceType));
    return builder.build();
  }

  /**
   * 生成懒初始化代理类
   * @param element Service元素
   * @throws IOException 写入异常
   */
  private void generateLazyClassFile(Element element) throws IOException {
    Type serviceType = ((Symbol.ClassSymbol) element).asType();
    List<Type> apisType = getApiTypes(element);
    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(
            element.getSimpleName() + LazyDelegate_SUFFIX);
    TypeName apiTypeName;
    for (Type api : apisType) {
      apiTypeName = TypeName.get(api);
      // 添加Service上的所有api接口
      typeBuilder.addSuperinterface(apiTypeName);
      // 添加代理类变量
      TypeName delegateType = ParameterizedTypeName.get(LazyDelegateType, apiTypeName);
      String fieldName = ((ClassName) apiTypeName).simpleName() + LazyDelegate_Field_DELEGATE_SUFFIX;
      FieldSpec.Builder fieldBuilder = FieldSpec.builder(
              delegateType, fieldName)
              .addModifiers(Modifier.PRIVATE)
              .initializer("new $T<$T>() {}", LazyDelegateImplType, apiTypeName);
      typeBuilder.addField(fieldBuilder.build());

      // 添加接口方法实现
      Iterator<Symbol> membersIterator = api.tsym.members().getElements().iterator();
      Symbol member;
      while (membersIterator.hasNext()) {
        member = membersIterator.next();
        if (member.getKind() != ElementKind.METHOD) {
          // 只代理方法
          continue;
        }
        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) member;
        if (methodSymbol.isPrivate() || methodSymbol.isStatic() || methodSymbol.isDynamic()) {
          // 非公开成员方法不进行代理
          continue;
        }

        // 方法名
        String methodName = methodSymbol.getSimpleName().toString();
        // 形参列表
        List<Symbol.VarSymbol> params = methodSymbol.getParameters();
        // 返回类型
        Type returnType = methodSymbol.getReturnType();
        boolean hasReturn = !(returnType instanceof Type.JCVoidType
                || returnType.getKind() == TypeKind.VOID);
        TypeName returnTypeName = TypeName.get(returnType);
        List<Symbol.TypeVariableSymbol> typeParameters = methodSymbol.getTypeParameters();
        List<TypeVariableName> typeVariableNames = new ArrayList<>(typeParameters.size());
        for (Symbol.TypeVariableSymbol typeVariableSymbol : typeParameters) {
          typeVariableNames.add(TypeVariableName.get(typeVariableSymbol));
        }
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addTypeVariables(typeVariableNames)
                .returns(returnTypeName);
        CodeBlock.Builder methodBody = CodeBlock.builder();
        if (hasReturn) {
          methodBody.add("return ");
        }
        methodBody.add("$L.get().$L(", fieldName, methodName);
        // 添加参数列表
        Symbol.VarSymbol paramSymbol;
        Iterator<Symbol.VarSymbol> paramIterator = params.iterator();
        while (paramIterator.hasNext()) {
          paramSymbol = paramIterator.next();
          methodBuilder.addParameter(ParameterSpec.get(paramSymbol));
          methodBody.add(paramSymbol.name.toString());
          if (paramIterator.hasNext()) {
            methodBody.add(", ");
          }
        }
        methodBody.addStatement(")");
        methodBuilder.addCode(methodBody.build());
        typeBuilder.addMethod(methodBuilder.build());
      }
    }

    // 写入文件
    JavaFile.builder(PACKAGE_OUTPUT, typeBuilder.build())
            .addFileComment("此文件为自动生成，用于组件化延迟代理")
            .build()
            .writeTo(filer);
  }

  /**
   * 另一种语法树扫描策略
   * @param element 根元素
   */
  private void findMethodByTreeScanner(Element element) {
    methodScanner.reset();
    ((JCTree) trees.getTree(element)).accept(methodScanner);
    for (JCTree.JCMethodDecl methodDecl : methodScanner.methods) {
      // 方法名
      Name methodName = methodDecl.name;
      // 修饰符public, static, synchronized ...
      JCTree.JCModifiers modifiers = methodDecl.mods;
      // 方法返回类型
      JCTree.JCExpression returnType = methodDecl.restype;
      // 方法的类型参数[泛型]
      List<JCTree.JCTypeParameter> typarams = methodDecl.typarams;
      // 方法形参列表
      List<JCTree.JCVariableDecl> params = methodDecl.params;
    }
  }

  private static class ApiMethodScanner extends TreeScanner {

    private List<JCTree.JCMethodDecl> methods = new ArrayList<>();

    private void reset() {
      methods.clear();
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
      super.visitMethodDef(jcMethodDecl);
      methods.add(jcMethodDecl);
    }
  }

}