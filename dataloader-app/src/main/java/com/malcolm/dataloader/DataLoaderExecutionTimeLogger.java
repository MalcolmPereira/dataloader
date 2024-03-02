package com.malcolm.dataloader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DataLoaderLogger
 * <p>
 * This annotation is used to mark methods so that their execution times can be logged.
 * <p>
 *
 * @author Malcolm
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DataLoaderExecutionTimeLogger {
}
