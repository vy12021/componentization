package com.bhb.android.componentization;

import java.lang.reflect.ParameterizedType;
import java.util.Objects;

/**
 * 这样的空构造是为了反射到实际类型参数，具体使用的地方构造一个匿名类来保留类型信息
 * @param <C> API类型
 */
abstract class LazyDelegateImpl<C extends API> implements LazyDelegate<C> {

  /**
   * 组件实例
   */
  private volatile C api;

  @SuppressWarnings("unchecked")
  private Class<C> getAPIClass() {
    return (Class<C>) ((ParameterizedType) Objects.requireNonNull(getClass()
            .getGenericSuperclass())).getActualTypeArguments()[0];
  }

  @Override
  public C create() {
    return Componentization.getSafely(getAPIClass());
  }

  @Override
  public synchronized C get() {
    if (null == api) {
      api = create();
    }
    return api;
  }
}
