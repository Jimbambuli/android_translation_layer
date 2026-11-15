package android.view;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AOSP ViewDebug, only its public annotations (apps put them on their views for the layout inspector and
 * read them back by reflection). The view server and the dump/profile/capture tools are left out:
 * nothing in ATL asks for them.
 */
public class ViewDebug {

	@Target({ElementType.FIELD, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ExportedProperty {
		boolean resolveId() default false;

		IntToString[] mapping() default {};

		IntToString[] indexMapping() default {};

		FlagToString[] flagMapping() default {};

		boolean deepExport() default false;

		String prefix() default "";

		String category() default "";

		boolean formatToHexString() default false;

		boolean hasAdjacentMapping() default false;
	}

	@Target({ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface IntToString {
		int from();

		String to();
	}

	@Target({ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface FlagToString {
		int mask();

		int equals();

		String name();

		boolean outputIf() default true;
	}

	@Target({ElementType.FIELD, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface CapturedViewProperty {
		boolean retrieveReturn() default false;
	}
}
