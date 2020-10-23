package com.bhb.android.componentization;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 组件入口调用
 * Created by Tesla on 2020/09/22.
 */
public final class Componentization {

  private static final String TAG = "Componentization";

  /**
   * 收集到的组件注册信息
   */
  private static Map<Class<? extends API>, Class<? extends API>> sComponentProvider;
  /**
   * 单例组件存储
   */
  private static Map<Class<? extends API>, API> sComponents;

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
    if (null == sComponentProvider) {
      sComponentProvider = new HashMap<>();
    }
    if (null == sComponents) {
      sComponents = new HashMap<>();
    }
    try {
      Log.e(TAG, "register: " + register);
      ComponentRegister.Item registerItem = register.newInstance().register();
      for (Class<? extends API> api : registerItem.apis) {
        sComponentProvider.put(api, registerItem.service);
      }
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
    Api_ apiAnnotation = type.getAnnotation(Api_.class);
    if (null == apiAnnotation) {
      throw new ComponentException("API接口需要被CApi注解修饰");
    }
    Class<T> service = (Class<T>) sComponentProvider.get(type);
    if (null == service) {
      throw new ComponentException("组件[" + type.getCanonicalName() + "]没有找到，清查找是否有实现");
    }
    if (apiAnnotation.singleton()) {
      // 检查INSTANCE静态引用
      T serviceInstance = (T) sComponents.get(type);
      if (null != serviceInstance) {
        return serviceInstance;
      }
      sComponents.put(type, serviceInstance = makeInstance(service));
      return serviceInstance;
    }
    return makeInstance(service);
  }

  /**
   * 尝试获取指定api延迟初始化实现
   * @param type api接口
   * @param <T>  类型
   * @return     api实现：必须被AService注解修饰
   */
  @SuppressWarnings("unchecked")
  public static <T extends API> T getLazy(Class<T> type) throws ComponentException {
    try {
      Class<? extends LazyDelegate<T>> lazyClazz
              = loadClass(type.getName() + LazyDelegate.SUFFIX);
      return (T) lazyClazz.newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      throw new ComponentException("组件[" + type.getCanonicalName() + "]无法支持延迟初始化特性");
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
  private static <T extends API> T makeInstance(Class<T> service) {
    T serviceInstance = null;
    try {
      Field INSTANCE = service.getDeclaredField("INSTANCE");
      INSTANCE.setAccessible(true);
      serviceInstance = (T) INSTANCE.get(null);
    } catch (Exception e) {
      e.printStackTrace();
      try {
        Constructor<? extends API> constructor = service.getDeclaredConstructor();
        constructor.setAccessible(true);
        serviceInstance = (T) constructor.newInstance();
      } catch (Exception e1) {
        e1.printStackTrace();
      }
    }
    return serviceInstance;
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
      } catch (ClassNotFoundException e1) {
        Log.e(TAG, Log.getStackTraceString(e1));
        throw new ComponentException(e1.getMessage(), e1.getCause());
      }
    }
    return clazz;
  }

}
