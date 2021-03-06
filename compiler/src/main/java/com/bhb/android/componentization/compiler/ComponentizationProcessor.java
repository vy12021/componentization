package com.bhb.android.componentization.compiler;

import com.bhb.android.componentization.annotation.Api;
import com.bhb.android.componentization.annotation.Service;
import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
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

import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * ????????????????????????
 * Created by Tesla on 2020/09/30.
 */
@AutoService(Processor.class)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.DYNAMIC)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public final class ComponentizationProcessor extends AbstractProcessor {

  private static final String PACKAGE_SPACE = "com.bhb.android.componentization";
  private static final String ComponentRegister_SUFFIX = "_Register";
  private static final String LazyDelegate_SUFFIX = "_Lazy";
  private static final String LazyDelegate_Field_DELEGATE_SUFFIX = "Delegate";
  private static final TypeName ArrayListType = TypeName.get(ArrayList.class);
  private static final ClassName ComponentRegisterType = ClassName.get(
          PACKAGE_SPACE, "ComponentRegister");
  private static final ClassName RegisterItemType = ClassName.get(
          PACKAGE_SPACE, "ComponentRegister.Item");
  private static final ClassName APIType = ClassName.get(
          PACKAGE_SPACE, "API");
  private static final ClassName LazyDelegateType = ClassName.get(
          PACKAGE_SPACE, "LazyDelegate");
  private static final ClassName LazyDelegateImplType = ClassName.get(
          PACKAGE_SPACE, "LazyDelegateImpl");
  private static final ClassName AnnotationMetaType = ClassName.get(
          PACKAGE_SPACE + ".annotation", "Meta");

  /**
   * ???????????????????????????????????????boolean??????????????????????????????
   */
  private static final String OPTION_DEBUG_MODE = "option.debug.enable";
  /**
   * ???????????????????????????????????????String????????????????????????
   */
  private static final String OPTION_MODULE_NAME = "option.module.name";
  /**
   * ???????????????
   */
  private static final String OPTION_ROOT_MODULE_DIR = "option.root.module.dir";
  /**
   * ????????????????????????{@link #OPTION_ROOT_MODULE_DIR}
   */
  private static final String OPTION_RESOURCES_DIR = "option.resources.dir";

  private Types typeUtils;
  private Filer filer;
  private @Nullable Trees trees;
  private Messager logger;
  private Map<String, String> options;
  private Set<String> registers = new HashSet<>();
  private boolean debugEnabled;
  private String moduleName;
  private String rootDirectory;
  private String resourcesDirectory;

  @Override
  public synchronized void init(ProcessingEnvironment env) {
    super.init(env);
    options = env.getOptions();
    typeUtils = env.getTypeUtils();
    logger = env.getMessager();
    filer = env.getFiler();
    debugEnabled = options.containsKey(OPTION_DEBUG_MODE)
            && Boolean.parseBoolean(options.get(OPTION_DEBUG_MODE));
    moduleName = options.get(OPTION_MODULE_NAME);
    rootDirectory = options.get(OPTION_ROOT_MODULE_DIR);
    resourcesDirectory = options.get(OPTION_RESOURCES_DIR);
    logger.printMessage(Diagnostic.Kind.WARNING,
            "options--->{"
                    + "debuggable: " + debugEnabled
                    + ", moduleName: " + moduleName
                    + ", rootDirectory: " + rootDirectory
                    + ", resourcesDirectory: " + resourcesDirectory + "}\n ");
    try {
      generateRegisterHolder();
    } catch (Exception e) {
      e.printStackTrace();
      logger.printMessage(Diagnostic.Kind.ERROR,
              "???????????????????????????????????????: " + e.getLocalizedMessage());
    }
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

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  @Override public Set<String> getSupportedOptions() {
    Set<String> options = new LinkedHashSet<>(7);
    if (trees != null) {
      options.add(OPTION_DEBUG_MODE);
      options.add(OPTION_ROOT_MODULE_DIR);
      options.add(OPTION_MODULE_NAME);
      options.add(OPTION_RESOURCES_DIR);
      options.add(IncrementalAnnotationProcessorType.ISOLATING.getProcessorOption());
    }
    return options;
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
    annotations.add(Service.class);
    return annotations;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    for (Element element : env.getElementsAnnotatedWith(Service.class)) {
      if (!SuperficialValidation.validateElement(element)) {
        logger.printMessage(Diagnostic.Kind.WARNING,
                "??????????????????" + element.getSimpleName().toString());
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
    try {
      generateRegisterProperty();
    } catch (Exception e) {
      e.printStackTrace();
      logger.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
    }
    return false;
  }

  /**
   * ?????????????????????
   * @param element ??????
   * @throws IOException ????????????
   */
  private void generateRegisterClassFile(Element element) throws IOException {
    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(
            element.getSimpleName() + ComponentRegister_SUFFIX)
            .addModifiers(Modifier.FINAL)
            .addSuperinterface(ComponentRegisterType)
            .addMethod(buildRegisterMethod(element))
            .addAnnotation(buildRegisterMeta(element));
    JavaFile file = JavaFile.builder(PACKAGE_SPACE, typeBuilder.build())
            .addFileComment("??????????????????????????????????????????????????????").build();
    file.writeTo(filer);
    registers.add(file.packageName + "." + file.typeSpec.name);
  }

  /**
   * ??????????????????key
   */
  private synchronized void generateRegisterHolder() throws Exception {
    Properties properties = new Properties();
    File propDir = new File(rootDirectory, resourcesDirectory);
    File propFile = new File(propDir, "module-register.properties");
    if (!propDir.exists() && !propDir.mkdirs()) {
      logger.printMessage(Diagnostic.Kind.ERROR, "???????????????????????????: " + propDir + "\n ");
      return;
    }
    if (!propFile.exists() && !propFile.createNewFile()) {
      logger.printMessage(Diagnostic.Kind.ERROR, "??????????????????????????????: " + propFile + "\n ");
      return;
    }
    // ????????????????????????build??????
    File lockFile = new File(new File(rootDirectory, "build"), "module-register.lock");
    while (lockFile.exists()) {
      Thread.sleep(5);
    }
    lockFile.createNewFile();
    try (InputStream is = new FileInputStream(propFile)) {
      properties.load(is);
      if (!properties.containsKey(moduleName)) {
        properties.put(moduleName, "");
        if (debugEnabled) {
          logger.printMessage(Diagnostic.Kind.NOTE, "????????????--->" + moduleName + "\n ");
        }
      }
      try (OutputStream writer = new FileOutputStream(propFile)) {
        properties.store(writer, "module registers");
      }
    } finally {
      lockFile.delete();
    }
  }

  /**
   * ????????????????????????????????????????????????????????????
   */
  private synchronized void generateRegisterProperty() throws Exception {
    StringBuilder classesBuilder = new StringBuilder();
    Iterator<String> iterator = registers.iterator();
    while (iterator.hasNext()) {
      String register = iterator.next();
      classesBuilder.append(register);
      if (iterator.hasNext()) {
        classesBuilder.append(",");
      }
    }
    Properties properties = new Properties();
    File propDir = new File(rootDirectory, resourcesDirectory);
    File propFile = new File(propDir, "module-register.properties");
    if (!propDir.exists() && !propDir.mkdirs()) {
      logger.printMessage(Diagnostic.Kind.ERROR, "???????????????????????????: " + propDir + "\n ");
      return;
    }
    if (!propFile.exists() && !propFile.createNewFile()) {
      logger.printMessage(Diagnostic.Kind.ERROR, "??????????????????????????????: " + propFile + "\n ");
      return;
    }
    // ????????????????????????build??????
    File lockFile = new File(new File(rootDirectory, "build"), "module-register.lock");
    while (lockFile.exists()) {
      Thread.sleep(5);
    }
    lockFile.createNewFile();
    try (InputStream is = new FileInputStream(propFile)) {
      properties.load(is);
      properties.put(moduleName, classesBuilder.toString());
      if (debugEnabled) {
        logger.printMessage(Diagnostic.Kind.NOTE,
                "????????????--->" + moduleName + ": " + classesBuilder + "\n ");
      }
      try (OutputStream writer = new FileOutputStream(propFile)) {
        properties.store(writer, "module registers");
      }
    } finally {
      lockFile.delete();
    }
  }

  private AnnotationSpec buildRegisterMeta(Element element) {
    Type serviceType = ((Symbol.ClassSymbol) element).asType();
    AnnotationSpec.Builder builder = AnnotationSpec.builder(AnnotationMetaType);
    // ??????Service??????
    builder.addMember("service", "$S",
            getRawType(TypeName.get(serviceType)).toString());
    // ??????API????????????
    List<Type> apiTypes = getApiTypes(element);
    CodeBlock.Builder coder = CodeBlock.builder();
    coder.add("{");
    Type api;
    for (int i = 0, len = apiTypes.size(); i < len; i++) {
      api = apiTypes.get(i);
      coder.add("$S", getRawType(TypeName.get(api)).toString());
      if (i < len - 1) {
        coder.add(", ");
      }
    }
    coder.add("}");
    builder.addMember("api", coder.build());
    return builder.build();
  }

  /**
   * ??????Service??????????????????????????????
   * @param element Service??????
   * @return ????????????
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
        throw new RuntimeException(itf.toString() + "?????????????????????API??????");
      }
      apisType.add(itf);
    }
    if (apisType.isEmpty()) {
      throw new RuntimeException(
              serviceType.toString() + "???????????????????????????????????????Api???????????????API?????????");
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
                type.toString() + "???@Api??????????????????????????????");
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
    MethodSpec.Builder builder = MethodSpec.methodBuilder("register")
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
   * ???????????????????????????
   * @param element Service??????
   * @throws IOException ????????????
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
      // ??????Service????????????api??????
      typeBuilder.addSuperinterface(apiTypeName);
      // ?????????????????????
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

    // ????????????
    JavaFile.builder(PACKAGE_SPACE, typeBuilder.build())
            .addFileComment("??????????????????????????????????????????????????????")
            .build()
            .writeTo(filer);
  }

  private List<MethodSpec> generateMethod(Type service, Type api, String delegateField) {
    List<MethodSpec> methodSpecs = new ArrayList<>();
    // ????????????????????????
    Iterator<Symbol> membersIterator = api.tsym.members().getSymbols().iterator();
    Symbol member;
    while (membersIterator.hasNext()) {
      member = membersIterator.next();
      if (member.getKind() != ElementKind.METHOD) {
        // ???????????????
        continue;
      }
      Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) member;
      if (methodSymbol.isPrivate() || methodSymbol.isStatic() || methodSymbol.isDynamic()) {
        // ????????????????????????????????????
        continue;
      }
      methodSymbol = getMethodSymbol(service, methodSymbol);
      // ?????????
      String methodName = methodSymbol.getSimpleName().toString();
      // ????????????
      List<Symbol.VarSymbol> params = methodSymbol.getParameters();
      // ????????????
      Type returnType = methodSymbol.getReturnType();
      boolean hasReturn = !(returnType instanceof Type.JCVoidType
              || returnType.getKind() == TypeKind.VOID);
      TypeName returnTypeName = TypeName.get(returnType);
      List<Attribute.Compound> annotationTypes = methodSymbol.getAnnotationMirrors();
      List<AnnotationSpec> annotationSpecs = new ArrayList<>(annotationTypes.size());
      for (Attribute.Compound annotation : annotationTypes) {
        annotationSpecs.add(AnnotationSpec.get(annotation));
      }
      if (member == methodSymbol) {
        annotationSpecs.add(AnnotationSpec.builder(Override.class).build());
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
      // ??????????????????
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
   * ???Service??????????????????????????????api????????????????????????????????????????????????????????????????????????????????????????????????
   * ??????????????????????????????methodName(Params...)
   * @param service   ????????????
   * @param apiMethod ?????????????????????api
   * @return ????????????
   */
  private Symbol.MethodSymbol getMethodSymbol(Type service, Symbol.MethodSymbol apiMethod) {
    String apiMethodName = apiMethod.getSimpleName().toString();
    List<Symbol.VarSymbol> apiParams = apiMethod.getParameters();
    Type apiReturnType = apiMethod.getReturnType();
    Iterator<Symbol> memberIterator = service.asElement().members().getSymbols().iterator();
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
      // ?????????
      String methodName = methodSymbol.getSimpleName().toString();
      // ????????????
      List<Symbol.VarSymbol> params = methodSymbol.getParameters();
      // ????????????
      Type returnType = methodSymbol.getReturnType();
      // ????????????
      if (!methodName.equals(apiMethodName)) {
        continue;
      }
      // ???????????????????????????????????????????????????
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
    return apiMethod;
  }

  private TypeName getRawType(TypeName typeName) {
    return typeName instanceof ParameterizedTypeName
            ? ((ParameterizedTypeName) typeName).rawType : typeName;
  }

}