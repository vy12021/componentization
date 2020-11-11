package com.bhb.android.componentization;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
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
import com.sun.tools.javac.code.Attribute;
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
    coder.add("\"" + getRawType(TypeName.get(serviceType)).toString() + ";");
    Type api;
    for (int i = 0, len = apisType.size(); i < len; i++) {
      api = apisType.get(i);
      coder.add(getRawType(TypeName.get(api)).toString());
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
    List<Type> interfaces = getAllInterfaces(serviceType);
    List<Type> apisType = new ArrayList<>(interfaces.size());
    for (Type itf : interfaces) {
      if (null == itf.asElement().getAnnotation(Api.class)) {
        continue;
      }
      boolean isAPIType = false;
      for (Type sitf : getAllInterfaces(itf)) {
        if (APIType.toString().equals(sitf.toString())) {
          isAPIType = true;
        }
      }
      if (!isAPIType) {
        throw new RuntimeException(itf.toString() + "的父接口必须为API类型");
      }
      apisType.add(itf);
    }
    if (apisType.isEmpty()) {
      throw new RuntimeException(serviceType.toString() + "的父接口中必须至少有一个被Api注解修饰的API接口类");
    }
    return apisType;
  }

  private List<Type> getAllInterfaces(Type type) {
    List<Type> interfaces = new ArrayList<>();
    if (type.isInterface()) {
      interfaces.add(type);
      for (Type itf : ((Symbol.ClassSymbol) type.tsym).getInterfaces()) {
        interfaces.addAll(getAllInterfaces(itf));
      }
    } else {
      if (null != type.asElement().getAnnotation(Api.class)) {
        logger.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                type.toString() + "：@Api注解不支持非接口类型");
      }
      Type superType = ((Symbol.ClassSymbol) type.tsym).getSuperclass();
      if (null != superType && superType.getKind() != TypeKind.NONE) {
        interfaces.addAll(getAllInterfaces(superType));
      }
      for (Type itf : ((Symbol.ClassSymbol) type.tsym).getInterfaces()) {
        interfaces.addAll(getAllInterfaces(itf));
      }
    }
    return interfaces;
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
    TypeName typeName;
    for (Type api : apisType) {
      typeName = TypeName.get(api);
      coder.addStatement("apis.add($T.class)", getRawType(typeName));
    }
    typeName = TypeName.get(serviceType);
    builder.addCode(coder.build());
    builder.addStatement("return new $T(apis, $T.class)", RegisterItemType, getRawType(typeName));
    return builder.build();
  }

  /**
   * 生成懒初始化代理类
   * @param element Service元素
   * @throws IOException 写入异常
   */
  private void generateLazyClassFile(Element element) throws IOException {
    Type serviceType = ((Symbol.ClassSymbol) element).asType();
    List<Type> apis = getApiTypes(element);
    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(
            element.getSimpleName() + LazyDelegate_SUFFIX);
    TypeName apiTypeName;
    List<Type> interfaces;
    for (Type api : apis) {
      apiTypeName = TypeName.get(api);
      interfaces = getAllInterfaces(api);
      // 添加Service上的所有api接口
      typeBuilder.addSuperinterface(apiTypeName);
      // 添加代理类变量
      TypeName delegateType = ParameterizedTypeName.get(LazyDelegateType, apiTypeName);
      String fieldName = ((ClassName) getRawType(apiTypeName)).simpleName()
              + LazyDelegate_Field_DELEGATE_SUFFIX;
      FieldSpec.Builder fieldBuilder = FieldSpec.builder(
              delegateType, fieldName)
              .addModifiers(Modifier.PRIVATE)
              .initializer("new $T<$T>() {}", LazyDelegateImplType, apiTypeName);
      typeBuilder.addField(fieldBuilder.build());
      for (Type intf : interfaces) {
        for (MethodSpec method : generateMethod(serviceType, intf, fieldName)) {
          typeBuilder.addMethod(method);
        }
      }
    }

    // 写入文件
    JavaFile.builder(PACKAGE_OUTPUT, typeBuilder.build())
            .addFileComment("此文件为自动生成，用于组件化延迟代理")
            .build()
            .writeTo(filer);
  }

  private List<MethodSpec> generateMethod(Type service, Type api, String delegateField) {
    List<MethodSpec> methodSpecs = new ArrayList<>();
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
      methodSymbol = getMethodSymbol(service, methodSymbol);
      // 方法名
      String methodName = methodSymbol.getSimpleName().toString();
      // 形参列表
      List<Symbol.VarSymbol> params = methodSymbol.getParameters();
      // 返回类型
      Type returnType = methodSymbol.getReturnType();
      boolean hasReturn = !(returnType instanceof Type.JCVoidType
              || returnType.getKind() == TypeKind.VOID);
      TypeName returnTypeName = TypeName.get(returnType);
      List<Attribute.Compound> annotationTypes = methodSymbol.getAnnotationMirrors();
      List<AnnotationSpec> annotationSpecs = new ArrayList<>(annotationTypes.size());
      for (Attribute.Compound annotation : annotationTypes) {
        annotationSpecs.add(AnnotationSpec.get(annotation));
      }
      List<Symbol.TypeVariableSymbol> typeParameters = methodSymbol.getTypeParameters();
      List<TypeVariableName> typeVariableNames = new ArrayList<>(typeParameters.size());
      for (Symbol.TypeVariableSymbol typeVariableSymbol : typeParameters) {
        typeVariableNames.add(TypeVariableName.get(typeVariableSymbol));
      }
      List<Type> thrownTypes = methodSymbol.getThrownTypes();
      List<TypeName> thrownTypeNames = new ArrayList<>(thrownTypes.size());
      for (Type thrownType : thrownTypes) {
        thrownTypeNames.add(TypeName.get(thrownType));
      }
      MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
              .addModifiers(Modifier.PUBLIC)
              .addAnnotations(annotationSpecs)
              .addTypeVariables(typeVariableNames)
              .addExceptions(thrownTypeNames)
              .returns(returnTypeName);
      CodeBlock.Builder methodBody = CodeBlock.builder();
      if (hasReturn) {
        methodBody.add("return ");
      }
      methodBody.add("$L.get().$L(", delegateField, methodName);
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
      methodSpecs.add(methodBuilder.build());
    }
    return methodSpecs;
  }

  /**
   * 从Service类型中查找对应的实现api，便于做类型映射，接口中涉及到泛型擦除，可能已经丢失掉一部分信息
   * 匹配方式为函数签名：methodName(Params...)
   * @param service   实现类型
   * @param apiMethod 当前需要查找的api
   * @return 实现方法
   */
  private Symbol.MethodSymbol getMethodSymbol(Type service, Symbol.MethodSymbol apiMethod) {
    String apiMethodName = apiMethod.getSimpleName().toString();
    List<Symbol.VarSymbol> apiParams = apiMethod.getParameters();
    Type apiReturnType = apiMethod.getReturnType();
    Iterator<Symbol> memberIterator = service.asElement().members().getElements().iterator();
    Symbol member;
    while (memberIterator.hasNext()) {
      member = memberIterator.next();
      if (member.getKind() != ElementKind.METHOD) {
        continue;
      }
      Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) member;
      if (methodSymbol.isPrivate() || methodSymbol.isStatic() || methodSymbol.isDynamic()) {
        continue;
      }
      // 方法名
      String methodName = methodSymbol.getSimpleName().toString();
      // 形参列表
      List<Symbol.VarSymbol> params = methodSymbol.getParameters();
      // 返回类型
      Type returnType = methodSymbol.getReturnType();
      // 比对名称
      if (!methodName.equals(apiMethodName)) {
        continue;
      }
      // 返回值都不是泛型类型时进行严格比对
      if (returnType.getKind() != TypeKind.TYPEVAR && apiReturnType.getKind() != TypeKind.TYPEVAR
              && returnType != apiReturnType) {
        continue;
      }
      if (params.size() != apiParams.size()) {
        continue;
      }
      Type paramType;
      Type apiParamType;
      boolean paramsValid = true;
      for (int pi = 0; pi < params.size(); pi++) {
        paramType = params.get(pi).type;
        apiParamType = apiParams.get(pi).type;
        if (paramType.getKind() != TypeKind.TYPEVAR && apiParamType.getKind() != TypeKind.TYPEVAR
                && paramType != apiParamType) {
          paramsValid = false;
          break;
        }
      }
      if (!paramsValid) {
        continue;
      }
      return methodSymbol;
    }
    logger.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
            "无法在[" + service.toString() + "]中找到[" + apiMethod.toString() + "]");
    return apiMethod;
  }

  private TypeName getRawType(TypeName typeName) {
    return typeName instanceof ParameterizedTypeName
            ? ((ParameterizedTypeName) typeName).rawType : typeName;
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