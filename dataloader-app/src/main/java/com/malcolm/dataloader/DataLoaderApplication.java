package com.malcolm.dataloader;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Data Loader Application using Spring Boot
 *
 * @author Malcolm
 */
@SpringBootApplication(scanBasePackages = { "com.malcolm.dataloader"}, exclude = { ErrorMvcAutoConfiguration.class })
@EnableAsync
@EnableScheduling
@EnableRetry
public class DataLoaderApplication  {

    /**
     * Main Application
     * @param args Arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(DataLoaderApplication.class, args);
    }

    /**
     * MicroMeter Time Aspect to annotate methods for getting metrics
     *
     * @param registry Meter Registry
     * @return TimedAspect
     */
    @Bean
    TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Thread Pool for Async Tasks
     * @param env Environment
     * @return ThreadPoolTaskExecutor Thread Pool Executor
     */
    @Bean(name = "AsyncExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor asyncExecutor(Environment env) {
        return getThreadPoolTaskExecutor("Async-", env);
    }

    /**
     * Thread Pool for Batch Tasks
     * @param env Environment
     * @return ThreadPoolTaskExecutor Thread Pool
     */
    @Bean(name = "BatchTaskExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor batchTaskExecutor(Environment env) {
		return getThreadPoolTaskExecutor("Batch-Executor-", env);
     }

    /**
     * Get Thread Pool Task Executor
     * @param threadPrefix Thread Prefix
     * @param env TEnvironment
     * @return Thread Pool
     */
    private ThreadPoolTaskExecutor getThreadPoolTaskExecutor(String threadPrefix, Environment env) {
        int maxPoolSize  = getMaxPoolSize(env);
        int corePoolSize = maxPoolSize/2;
        System.out.println("POOL PREFIX:"+threadPrefix+" MAX POOL SIZE:  "+maxPoolSize+ " CORE POOL SIZE: "+corePoolSize);
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
        threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
        threadPoolTaskExecutor.setQueueCapacity(maxPoolSize);
        threadPoolTaskExecutor.setThreadNamePrefix(threadPrefix);
        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    /**
     * Get Thread Pool Size based on available processor and maximum wait time and processor time
     * @return int maximum pool size
     */
    private int getMaxPoolSize(Environment env) {
        double targetCPUUtilizationDouble = 0.75;
        try{
            targetCPUUtilizationDouble = Double.parseDouble(Objects.requireNonNull(env.getProperty("dataloader.cpu.utilization")));
        }catch(NullPointerException err){
            System.err.println("No Target CPU Utilization Configured Default: "+targetCPUUtilizationDouble);
        }
        long targetCPUWaitTime = 2000L;
        try{
            targetCPUWaitTime = Long.parseLong(Objects.requireNonNull(env.getProperty("dataloader.cpu.wait.time")));
        }catch(NullPointerException err){
            System.err.println("No Target CPU Wait Time Configured Default: "+targetCPUWaitTime);
        }
        long targetCPUComputeTime = 1000L;
        try{
            targetCPUComputeTime = Long.parseLong(Objects.requireNonNull(env.getProperty("dataloader.cpu.process.time")));
        }catch(NullPointerException err){
            System.err.println("No Target CPU Process Time Configured Default: "+targetCPUComputeTime);
        }
        long availableProcessors = Runtime.getRuntime().availableProcessors();
        System.out.println("availableProcessors:"+availableProcessors);
        System.out.println("targetCPUUtilizationDouble:"+targetCPUUtilizationDouble);
        System.out.println("targetCPUWaitTime:"+targetCPUWaitTime);
        System.out.println("targetCPUComputeTime:"+targetCPUComputeTime);
        return (int) Math.ceil(availableProcessors * targetCPUUtilizationDouble * (1 + (double) targetCPUWaitTime / targetCPUComputeTime));
    }
}
