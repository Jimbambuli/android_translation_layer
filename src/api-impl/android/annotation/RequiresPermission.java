/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated element requires one or more permissions.
 */
@Retention(SOURCE)
@Target({ANNOTATION_TYPE, METHOD, CONSTRUCTOR, FIELD, PARAMETER})
public @interface RequiresPermission {
	/** The name of the permission that is required, if precisely one permission is required. */
	String value() default "";

	/** The names of the permissions of which all are required. */
	String[] allOf() default {};

	/** The names of the permissions of which at least one is required. */
	String[] anyOf() default {};

	/** Specifies that the given permission is required only if the annotated element's runtime condition holds. */
	boolean conditional() default false;

	/** Specifies that the annotated field is a permission-checked value and requires the permission on read. */
	@Target({FIELD, METHOD, PARAMETER})
	@interface Read {
		RequiresPermission value() default @RequiresPermission;
	}

	/** Specifies that the annotated field is a permission-checked value and requires the permission on write. */
	@Target({FIELD, METHOD, PARAMETER})
	@interface Write {
		RequiresPermission value() default @RequiresPermission;
	}
}
