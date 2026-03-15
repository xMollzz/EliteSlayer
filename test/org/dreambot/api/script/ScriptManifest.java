package org.dreambot.api.script;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ScriptManifest {
    String name() default "";
    String author() default "";
    double version() default 1.0;
    String description() default "";
    Category category() default Category.OTHER;
}
