package com.malcolm.dataloader.batch;

import com.malcolm.dataloader.service.DataLoaderRateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Job Completion Listener to log the status of the job and delete the file after the job is completed.
 *
 * @author Malcolm
 */
@Component
public class DataLoaderJobCompletionListener implements JobExecutionListener {

    /**
     * Logger for the class.
     */
    private final Logger logger = LoggerFactory.getLogger(DataLoaderJobCompletionListener.class);

    /**
     * Rate Limiter Service
     */
    private final DataLoaderRateLimiterService dataLoaderRateLimiterService;

    /**
     * Public Constructor
     * @param dataLoaderRateLimiterService DataLoaderJobCompletionListener
     */
    public DataLoaderJobCompletionListener(DataLoaderRateLimiterService dataLoaderRateLimiterService){
        this.dataLoaderRateLimiterService = dataLoaderRateLimiterService;
    }

    /**
     * Log the status of the job and delete the file after the job is completed.
     * @param jobExecution JobExecution
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        this.dataLoaderRateLimiterService.resetRequestCounts();
        String filePath = jobExecution.getJobParameters().getString("filePath");
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            logger.info("Job Completed Successfully for file: " + filePath);
        } else {
            logger.info("Job Failed for file for file: " + filePath);
        }
        if (filePath != null){
            final Path path = Path.of(filePath);
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(path.toFile().lastModified()), ZoneId.systemDefault());
            logger.info("BATCH JOB STARTED AT " + dateTime);
            logger.info("TOTAL DURATION FOR BATCH JOB: " + ChronoUnit.MILLIS.between(dateTime, LocalDateTime.now())+" ms, "+ ChronoUnit.SECONDS.between(dateTime, LocalDateTime.now())+" seconds, "+ ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now())+" minutes.");
            logger.info("DELETING JOB FILE: " + filePath +"  "+ path.toFile().delete());
        }
    }
}