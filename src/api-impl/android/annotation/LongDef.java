package android.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Same as IntDef, for long constants (android.annotation.LongDef of the SDK stubs). */
@Retention(SOURCE)
@Target({ANNOTATION_TYPE})
public @interface LongDef {
	String[] prefix() default {};

	String[] suffix() default {};

	long[] value() default {};

	boolean flag() default false;
}
