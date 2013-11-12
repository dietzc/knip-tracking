package org.knime.knip.tracking.data.features;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Feature {
	String name();

	double defaultValue() default Double.NaN;

	String[] neededFeatures() default {};
}
