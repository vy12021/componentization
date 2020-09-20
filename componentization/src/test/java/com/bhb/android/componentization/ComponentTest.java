package com.bhb.android.componentization;

import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

class ComponentTest {

  @Test
  public void generateComponents() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import android.view.View;\n"
            + "import com.bhb.android.componentization.Component;\n"
            + "import com.bhb.android.componentization.AutoWired;\n"
            + "@Component\n"
            + "public class Test {\n"
            + "    \n"
            + "}"
    );

    JavaFileObject bindingSource = JavaFileObjects.forSourceString("test/Test_ComponentBinding", ""
            + "package test;\n"
            + "import android.view.View;\n"
            + "import androidx.annotation.CallSuper;\n"
            + "import androidx.annotation.UiThread;\n"
            + "import butterknife.Unbinder;\n"
            + "import java.lang.IllegalStateException;\n"
            + "import java.lang.Override;\n"
            + "public class Test_ViewBinding implements Unbinder {\n"
            + "  private Test target;\n"
            + "  @UiThread\n"
            + "  public Test_ViewBinding(Test target, View source) {\n"
            + "    this.target = target;\n"
            + "    target.thing = source.findViewById(1);\n"
            + "  }\n"
            + "  @Override\n"
            + "  @CallSuper\n"
            + "  public void unbind() {\n"
            + "    Test target = this.target;\n"
            + "    if (target == null) throw new IllegalStateException(\"Bindings already cleared.\");\n"
            + "    this.target = null;\n"
            + "    target.thing = null;\n"
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
