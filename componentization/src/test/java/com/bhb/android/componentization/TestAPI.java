package com.bhb.android.componentization;

import java.util.List;

@Api_
public interface TestAPI extends API {
  String doSomething(String aaa, Boolean bbb, List<Integer> ccc);
}