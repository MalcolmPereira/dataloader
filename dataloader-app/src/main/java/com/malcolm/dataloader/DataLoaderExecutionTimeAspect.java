package com.malcolm.dataloader;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * DataLoaderExecutionTimeAspect
 * <p>
 * This aspect is used to log the execution time of methods marked with the DataLoaderExecutionTimeLogger annotation.
 * <p>
 *
 * @author Malcolm
 */
@Aspect
@Component
public class DataLoaderExecutionTimeAspect {
    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(DataLoaderExecutionTimeAspect.class);

    /**
     * logExecutionTime
     * <p>
     * This method logs the execution time of methods marked with the DataLoaderExecutionTimeLogger annotation.
     * <p>
     *
     * @param joinPoint JoinPoint the JoinPoint to be used
     * @return Object the Object to be returned
     * @throws Throwable the Throwable to be thrown
     */
    @Around("@annotation(com.malcolm.dataloader.DataLoaderExecutionTimeLogger)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        final StopWatch stopWatch = new StopWatch(method.getName());
        stopWatch.start(method.getName());
        try{
           return joinPoint.proceed();
        } finally {
            stopWatch.stop();
            logger.info("LOGGING ASPECT: " + method.getName()+" "+ stopWatch.getTotalTimeMillis() + "ms, "+ stopWatch.getTotalTimeSeconds()+" seconds, " + stopWatch.getTotalTime(TimeUnit.MINUTES)+ " minutes");
        }
    }
}
