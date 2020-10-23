package com.bhb.android.componentization;

import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

public class ServiceTest {

  @Test
  public void componentCollect() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.TestService", ""
            + "package test;\n"
            + "import java.lang.Override;\n"
            + "import java.util.List;\n"
            + "import com.bhb.android.componentization.Service;\n"
            + "import com.bhb.android.componentization.TestAPI;\n"
            + "@Service\n"
            + "public class TestService implements TestAPI {\n"

            + "  @Override public String doSomething(String aaa, Boolean bbb, List<Integer> ccc) {\n"
            + "    return \"\";"
            + "    \n"
            + "  }\n"

            + "}"
    );

    JavaFileObject bindingSource = JavaFileObjects.forSourceString(
            "com.bhb.android.componentization/TestService_Register", ""
            + "package test;\n"
            + "import android.util.Pair;\n"
            + "import java.lang.Override;\n"
            + "import test.TestService;\n"
            + "import com.bhb.android.componentization.API;\n"
            + "import com.bhb.android.componentization.ComponentRegister;\n"
            + "import com.bhb.android.componentization.TestAPI;\n"
            + "public class TestService_Register implements ComponentRegister {\n"
            + "  @Override public ComponentRegister.Item register() {\n"
            + "    final ArrayList<Class<? extends API> apis = new ArrayList<>(1);\n"
            + "    apis.add(TestAPI.class);\n"
            + "    return ComponentRegister.Item(apis, TestService.class);\n"
            + "  }\n"
            + "}"
    );

    assertAbout(javaSource()).that(source)
            .withCompilerOptions("-Xlint:-processing")
            .processedWith(new ComponentizationProcessor())
            .compilesWithoutWarnings()
            .and()
            .generatesSources(bindingSource);
  }

}
