package com.bhb.android.componentization;

import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

public class ServiceTest {

  @Test
  public void componentCollect() {
    JavaFileObject source = JavaFileObjects.forSourceString("com.bhb.android.module.Test", ""
            + "package test;\n"
            + "import com.bhb.android.componentization.Component;\n"
            + "import com.bhb.android.componentization.Api;\n"
            + "@Component\n"
            + "public class TestService implements TestService.TestAPI {\n"

            + "  public void doSomething() {\n"
            + "    \n"
            + "  }\n"

            + "  @Api\n"
            + "  public interface TestAPI {\n"
            + "    void doSomething();\n"
            + "  }\n"
            + "}"
    );

    JavaFileObject bindingSource = JavaFileObjects.forSourceString("com.bhb.android.componentization/Test_ComponentBinding", ""
            + "package com.bhb.android.componentization;\n"
            + "import java.lang.Override;\n"
            + "public class Test_Register implements ComponentRegister {\n"

            + "  public Map< \n"

            + "\n"
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
