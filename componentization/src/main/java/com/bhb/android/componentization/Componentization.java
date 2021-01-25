package com.bhb.android.componentization;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 组件入口调用
 * Created by Tesla on 2020/09/22.
 */
public final class Componentization {

  /**
   * Log tag
   */
  private static final String TAG = "Componentization";

  /**
   * 全包名
   */
  private static final String PACKAGE = Componentization.class.getPackage().getName();

  /**
   * 收集到的组件注册信息
   */
  private final static Map<Class<? extends API>, Class<? extends API>>
          sComponentProvider = new HashMap<>();
  /**
   * 单例组件存储
   */
  private final static Map<Class<? extends API>, API> sComponents = new HashMap<>();

  /**
   * 手动注册
   * @param api     api
   * @param service service实例
   */
  public static void register(Class<? extends API> api, Class<? extends API> service) {
    sComponentProvider.put(api, service);
  }

  /**
   * 手动注册
   * @param api     api
   * @param service service实例
   */
  public static void register(Class<? extends API> api, API service) {
    sComponents.put(api, service);
  }

  /**
   * 执行指定组件器
   */
  private static void register(Class<? extends ComponentRegister> register) {
    try {
      ComponentRegister.Item registerItem = register.newInstance().register();
      for (Class<? extends API> api : registerItem.apis) {
        sComponentProvider.put(api, registerItem.service);
      }
      Log.e(TAG, "register: " + registerItem.service.getName());
    } catch (Exception e) {
      e.printStackTrace();
      Log.e(TAG, Log.getStackTraceString(e));
    }
  }

  /**
   * 尝试获取指定api实现
   * @param type api接口
   * @param <T>  类型
   * @return     api实现：必须被AService注解修饰
   */
  public static <T extends API> T getSafely(Class<T> type) {
    try {
      return get(type);
    } catch (ComponentException e) {
      e.printStackTrace();
      Log.e(TAG, Log.getStackTraceString(e));
    }
    return null;
  }

  /**
   * 尝试获取指定api实现
   * @param type api接口
   * @param <T>  类型
   * @return     api实现：必须被AService注解修饰
   * @throws ComponentException 相关异常
   */
  @SuppressWarnings("unchecked")
  public static <T extends API> T get(Class<T> type) throws ComponentException {
    Api apiAnnotation = type.getAnnotation(Api.class);
    if (null == apiAnnotation) {
      throw new ComponentException("API接口需要被Api注解修饰");
    }
    Class<T> service = (Class<T>) sComponentProvider.get(type);
    if (null == service) {
      throw new ComponentException(
              "组件[" + type.getCanonicalName() + "]没有找到，确认是否有Service实现");
    }
    if (apiAnnotation.singleton()) {
      T serviceInstance = (T) sComponents.get(type);
      if (null != serviceInstance) {
        return serviceInstance;
      }
      serviceInstance = makeInstance(service, true);
      if (null == serviceInstance) {
        throw new ComponentException(
            "组件[" + type.getCanonicalName() + "]存在循环单例引用，" +
            "请务必打开延迟初始化模式，这样可以规避由于实例同时请求建立引发的赋值冲突");
      }
      sComponents.put(type, serviceInstance);
      return serviceInstance;
    }
    return makeInstance(service, false);
  }

  /**
   * 尝试获取指定api延迟初始化实现
   * @param apiType api接口
   * @param <T>  类型
   * @return     api实现：必须被AService注解修饰
   */
  @SuppressWarnings("unchecked")
  public static <T extends API> T getLazy(Class<T> apiType) throws ComponentException {
    Class<T> type = apiType;
    try {
      type = null != type.getAnnotation(Service.class)
              ? type : (Class<T>) sComponentProvider.get(type);
      Class<? extends LazyDelegate<T>> lazyClazz
              = loadClass(PACKAGE + "." + type.getSimpleName() + LazyDelegate.SUFFIX);
      return (T) lazyClazz.newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      throw new ComponentException("组件[" + apiType.getCanonicalName() + "]无法支持延迟初始化特性");
    }
  }

  /**
   * 尝试获取指定api延迟初始化实现，自动向下降低为非延迟初始化
   * @param type api接口
   * @param <T>  类型
   * @return     api实现：必须被AService注解修饰
   */
  public static <T extends API> T getLazySafely(Class<T> type) {
    try {
      return getLazy(type);
    } catch (ComponentException e) {
      e.printStackTrace();
      Log.e(TAG, Log.getStackTraceString(e));
      return getSafely(type);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends API> T makeInstance(Class<T> service, boolean singleton)
          throws ComponentException {
    try {
      // kotlin object class
      Field INSTANCE = service.getDeclaredField("INSTANCE");
      INSTANCE.setAccessible(true);
      return (T) INSTANCE.get(null);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (!singleton) {
      return newInstance(service);
    }

    // for singleton
    Field[] fields = service.getDeclaredFields();
    for (Field field : fields) {
      Provider provider = field.getAnnotation(Provider.class);
      if (null == provider) {
        continue;
      }
      if (!service.isAssignableFrom(field.getType())) {
        throw new ComponentException("对于Service类" + service.getName()
                + "而言，被@Provider标记为服务提供者属性\"" + field.getName() + "\"类型不兼容");
      }
      field.setAccessible(true);
      try {
        return (T) field.get(null);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    Method[] methods = service.getMethods();
    for (Method method : methods) {
      Provider provider = method.getAnnotation(Provider.class);
      if (null == provider) {
        continue;
      }
      if (method.getParameterTypes().length > 0) {
        throw new ComponentException("对于Service类" + service.getName()
                + "而言，被@Provider标记为服务提供者方法\"" + method.getName() + "\"不能有参数");
      }
      if (method.getReturnType() == null || !service.isAssignableFrom(method.getReturnType())) {
        throw new ComponentException("对于Service类" + service.getName()
                + "而言，被@Provider标记为服务提供者方法\"" + method.getName() + "\"返回类型不兼容");
      }
      method.setAccessible(true);
      try {
        return (T) method.invoke(null);
      } catch (IllegalAccessException | InvocationTargetException e) {
        e.printStackTrace();
      }
    }
    throw new ComponentException(
            "对于Service类" + service.getName() + "而言，没有找到合适的实例提供者");
  }

  @SuppressWarnings("unchecked")
  private static <T extends API> T newInstance(Class<T> service) throws ComponentException {
    try {
      Constructor<? extends API> constructor = service.getDeclaredConstructor();
      constructor.setAccessible(true);
      return (T) constructor.newInstance();
    } catch (Exception e) {
      e.printStackTrace();
    }
    throw new ComponentException("对于Service类" + service.getName() + "而言，没有找到合适的实例构造器");
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> loadClass(String className) throws ComponentException {
    Class<T> clazz;
    try {
      clazz = (Class<T>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      try {
        clazz = (Class<T>) Thread.currentThread().getContextClassLoader().loadClass(className);
      } catch (Exception e1) {
        Log.e(TAG, Log.getStackTraceString(e1));
        throw new ComponentException(e1.getMessage(), e1.getCause());
      }
    }
    return clazz;
  }

}
