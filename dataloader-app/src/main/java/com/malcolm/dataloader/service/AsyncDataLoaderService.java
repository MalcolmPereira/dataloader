package com.malcolm.dataloader.service;

import com.malcolm.dataloader.DataLoaderException;
import com.malcolm.dataloader.DataLoaderExecutionTimeLogger;
import com.malcolm.dataloader.ProductToken;
import com.malcolm.dataloader.dao.ProductTokenDAO;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
/**
 * This class is responsible for processing the CSV file asynchronously
 * It uses the @Async annotation to run the methods in a separate thread
 * It also uses the @DataLoaderExecutionTimeLogger to log the time taken to execute the methods
 * It also uses the @Timed annotation to log the time taken to execute the methods
 * It also uses the @Service annotation to indicate that it is a service class
 *
 * @author Malcolm
 */
@Service
public class AsyncDataLoaderService {

    /**
     * The batch size for submitting the data to the database
     */
    @Value("${dataloader.batch.size}")
    private int BATCH_SIZE;

    /**
     * Cache Time to Live
     */
    @Value("${dataloader.cache.ttl}")
    private int CACHE_TTL;

    /**
     * Local Cache
     */
    private static final ConcurrentHashMap<String, Set<Long> > regionProductTokenMap = new ConcurrentHashMap<>();

    /**
     * Local Cache Access
     */
    private static final ConcurrentHashMap<String,LocalDateTime > regionProductTokenMapAccess = new ConcurrentHashMap<>();


    /**
     * The logger for this class
     */
    private final Logger logger = LoggerFactory.getLogger(AsyncDataLoaderService.class);

    /**
     * The product token data access object
     */
    private final ProductTokenDAO productTokenDAO;

    /**
     * The ThreadPoolTaskExecutor to use for running the tasks
     */
    private final ThreadPoolTaskExecutor taskExecutor;

    /**
     * The JobLauncher to use for running the batch job
     */
    private final JobLauncher jobLauncher;

    /**
     * The Batch Job to use for importing the product tokens
     */
    private final Job productTokenJob;

    /**
     * Rate Limiter Service
     */
    private final DataLoaderRateLimiterService dataLoaderRateLimiterService;

    /**
     * Public Constructor
     *
     * @param productTokenDAO Product Token DAO
     * @param jobLauncher Job Launcher
     * @param productTokenJob Product Token Job
     * @param taskExecutor Task Executor
     * @param dataLoaderRateLimiterService  DataLoaderRateLimiterService
     */
    public AsyncDataLoaderService(ProductTokenDAO productTokenDAO, JobLauncher jobLauncher, Job productTokenJob ,  @Qualifier("BatchTaskExecutor") Executor taskExecutor, DataLoaderRateLimiterService dataLoaderRateLimiterService) {
        this.productTokenDAO = productTokenDAO;
        this.jobLauncher = jobLauncher;
        this.productTokenJob = productTokenJob;
        this.taskExecutor = (ThreadPoolTaskExecutor)taskExecutor;
        this.dataLoaderRateLimiterService = dataLoaderRateLimiterService;
    }


    @Async("AsyncExecutor")
    @DataLoaderExecutionTimeLogger
    @Timed(value = "processCSVFileAsync")
    public void processCSVFileAsync(final File csvFile,boolean useTaskExecutor) throws DataLoaderException {
        final StopWatch stopWatch = new StopWatch("ASYNC PROCESS");
        stopWatch.start("ASYNC PROCESS");
        logger.info("ASYNC PROCESS STARTED AT " + LocalDateTime.now());
        try(
                InputStream csvFileStream = new FileInputStream(csvFile);
                CSVReader csvReader =  new CSVReader(new BufferedReader(new InputStreamReader(csvFileStream, StandardCharsets.UTF_8),1024))) {

            csvReader.readNext();
            String[] csvDataLine = csvReader.readNext();

            logger.info( "ASYNC PROCESS FIRST LINE LENGTH : " + csvDataLine.length );
            List<ProductToken> productTokens = new ArrayList<>();

            while(csvDataLine != null && csvDataLine.length > 4){

                final ProductToken productToken = getProductToken(csvDataLine);

                if(productToken.isValidProductToken()){
                    productTokens.add(productToken);

                }else{
                    logger.error("Invalid Product Token: "+productToken);
                    csvDataLine = csvReader.readNext();
                    continue;
                }

                if(productTokens.size() == BATCH_SIZE && useTaskExecutor){
                    logger.debug("ASYNC PROCESS BATCH SIZE REACHED SUBMITTING TASK BATCH SIZE: " + productTokens.size());
                    this.submitTask(productTokens.stream().distinct().toList());
                    productTokens.clear();
                }

                if(productTokens.size() == BATCH_SIZE && !useTaskExecutor){
                    logger.debug("ASYNC PROCESS BATCH SIZE REACHED CALLING INSERT NOW  BATCH SIZE: " + productTokens.size());
                    this.productTokenDAO.insertProductToken(productTokens.stream().distinct().toList());
                    productTokens.clear();
                }

                csvDataLine = csvReader.readNext();
            }

            if(!productTokens.isEmpty() && useTaskExecutor){
                logger.debug("ASYNC PROCESS SUBMITTING TASK ON FINAL BATCH SIZE: " + productTokens.size());
                this.submitTask(productTokens.stream().distinct().toList());
                productTokens.clear();
            }

            if(!productTokens.isEmpty()){
                logger.debug("ASYNC PROCESS CALLING INSERT ON FINAL BATCH SIZE: " + productTokens.size());
                this.productTokenDAO.insertProductToken(productTokens.stream().distinct().toList());
                productTokens.clear();
            }

            if(useTaskExecutor){
                // Wait for all tasks to finish, this just so we can log
                checkAllTasksCompleted();
            }

            stopWatch.stop();
            logger.info("TOTAL EXECUTION TIME FOR ASYNC PROCESS " + stopWatch.getTotalTimeMillis() + "ms, "+ stopWatch.getTotalTimeSeconds()+" seconds, " + stopWatch.getTotalTime(TimeUnit.MINUTES)+ " minutes");
        } catch (IOException | CsvValidationException err) {
            throw new DataLoaderException(101,"Failed processing CSV File",err);
        }finally{
            this.dataLoaderRateLimiterService.resetRequestCounts();
            logger.info("DELETED TEMP FILE " + csvFile.delete());
        }
    }

    @Async("AsyncExecutor")
    @DataLoaderExecutionTimeLogger
    @Timed(value = "processCSVFileByCompare")
    public void processCSVFileByCompare(final File csvFile, boolean useTaskExecutor) throws DataLoaderException {
        final StopWatch stopWatch = new StopWatch("ASYNC PROCESS WITH COMPARE");
        stopWatch.start("ASYNC PROCESS WITH COMPARE");
        logger.info("ASYNC PROCESS WITH COMPARE STARTED AT " + LocalDateTime.now());
        try(
                InputStream csvFileStream = new FileInputStream(csvFile);
                CSVReader csvReader =  new CSVReader(new BufferedReader(new InputStreamReader(csvFileStream, StandardCharsets.UTF_8),1024))) {

            csvReader.readNext();
            String[] csvDataLine = csvReader.readNext();


            logger.info( "ASYNC PROCESS WITH COMPARE  FIRST LINE LENGTH : " + csvDataLine.length );
            List<ProductToken> productTokens = new ArrayList<>();
            while(csvDataLine != null && csvDataLine.length > 4){

                cacheProductTokenValuesByRegionCode( csvDataLine, productTokens);

                if(productTokens.size() == BATCH_SIZE && useTaskExecutor){
                    this.submitTaskWithCache(productTokens.stream().distinct().toList());
                    productTokens.clear();
                }

                if(productTokens.size() == BATCH_SIZE && !useTaskExecutor){
                    this.insertProductTokensWithHash( productTokens.stream().distinct().toList() );
                    productTokens.clear();
                }

                csvDataLine = csvReader.readNext();
            }

            if(!productTokens.isEmpty() && useTaskExecutor){
                logger.debug("ASYNC PROCESS CALLING INSERT ON FINAL BATCH SIZE: " + productTokens.size());
                this.submitTaskWithCache(productTokens.stream().distinct().toList());
                productTokens.clear();
            }

            if(!productTokens.isEmpty()){
                logger.debug("ASYNC PROCESS CALLING INSERT ON FINAL BATCH SIZE: " + productTokens.size());
                this.insertProductTokensWithHash( productTokens.stream().distinct().toList() );
                productTokens.clear();
            }

            if(useTaskExecutor){
                // Wait for all tasks to finish, this just so we can log
                checkAllTasksCompleted();
            }
            stopWatch.stop();
            logger.info("TOTAL EXECUTION TIME FOR ASYNC PROCESS WITH COMPARE " + stopWatch.getTotalTimeMillis() + "ms, "+ stopWatch.getTotalTimeSeconds()+" seconds, " + stopWatch.getTotalTime(TimeUnit.MINUTES)+ " minutes");
        } catch (IOException | CsvValidationException err) {
            throw new DataLoaderException(101,"Failed processing CSV File",err);
        }finally{
            this.dataLoaderRateLimiterService.resetRequestCounts();
            logger.info("DELETED TEMP FILE " + csvFile.delete());
        }
    }

    /**
     * Process the file as a Spring Batch Job
     * @param csvFile CSV File for Processing
     */
    @Async("AsyncExecutor")
    @DataLoaderExecutionTimeLogger
    @Timed(value = "runImportProductTokensBatchJob")
    public void runImportProductTokensBatchJob(final String csvFile) {
        try{

            // Run the job with the path of the temporary file as a parameter
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("filePath", csvFile)
                    .toJobParameters();

            jobLauncher.run(this.productTokenJob, params);

        } catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | JobRestartException err) {
            logger.error("Error processing CSV file", err);
        }
    }

    @Scheduled(cron = "${dataloader.cache.cleanup.cron}")
    public void cleanUpCacheByTTL() {
        logger.info( "START CACHE CLEAN UP CACHE SIZE: "+regionProductTokenMap.size()+ " ACCESS MAP SIZE: "+regionProductTokenMapAccess.size() );
        LocalDateTime lastAccessTime  = LocalDateTime.now().minusMinutes( CACHE_TTL );
        regionProductTokenMapAccess.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(lastAccessTime)) {
                regionProductTokenMap.remove(entry.getKey());
                return true;
            }
            return false;
        });
        logger.info( "DONE CACHE CLEAN UP CACHE SIZE: "+regionProductTokenMap.size()+ " ACCESS MAP SIZE: "+regionProductTokenMapAccess.size() );
    }


    /**
     * Get Product Token Details from CSV Line
     * @param csvDataLine CSV Data Line
     * @return ProductToken ProductToken Object
     */
    private ProductToken getProductToken(final String[] csvDataLine) {
        ProductToken productToken = new ProductToken();
        productToken.setProductCode(csvDataLine[0]);
        productToken.setRegionCode(csvDataLine[1]);
        productToken.setProductSerial(Integer.parseInt(csvDataLine[2]));
        productToken.setProductTokenName(csvDataLine[3]);
        productToken.setProductTokenValues(csvDataLine[4]);
        return productToken;
    }

    /**
     * Cache Product Token Values By Region Code
     *
     * @param csvDataLine   CSV Data Line
     * @param productTokens Product Tokens
     */
    private void cacheProductTokenValuesByRegionCode( final String[] csvDataLine, final List<ProductToken> productTokens) {
        ProductToken productToken = getProductToken(csvDataLine);

        if(!productToken.isValidProductToken()){
            logger.error("Invalid Product Token: "+productToken);
            return;
        }

        //Calculate CRC32 CheckSum
        CRC32 crc = new CRC32();
        crc.update(productToken.getProductTokenValues().getBytes());
        productToken.setProductTokenValuesHash(crc.getValue());

        AsyncDataLoaderService.regionProductTokenMap.compute(productToken.getRegionCode(),(key, list) ->  {
            if ( list == null ) {
                list = new HashSet<>();
                List<Long> productTokenValues = this.productTokenDAO.getProductTokensByRegionCode(productToken.getRegionCode());
                if(productTokenValues != null && !productTokenValues.isEmpty()){
                    list.addAll( productTokenValues);
                }
            }
            return list;
        });
        if( AsyncDataLoaderService.regionProductTokenMap.isEmpty()
                || !AsyncDataLoaderService.regionProductTokenMap.containsKey(productToken.getRegionCode())
                || !AsyncDataLoaderService.regionProductTokenMap.get(productToken.getRegionCode()).contains(productToken.getProductTokenValuesHash())){
            productTokens.add(productToken);

        }else{
            regionProductTokenMapAccess.put(productToken.getRegionCode(),LocalDateTime.now());
            logger.debug("Product Token Value already exists for Region Code in cache CACHE SIZE: "+ AsyncDataLoaderService.regionProductTokenMap.size()+" REGION CODE DATA LIST SIZE: "+ AsyncDataLoaderService.regionProductTokenMap.get(productToken.getRegionCode()).size());
        }
        logger.debug( "CACHE SIZE: "+regionProductTokenMap.size()+ " ACCESS MAP SIZE: "+regionProductTokenMapAccess.size() );
    }

    /**
     * Insert Product Tokens with Hash
     * @param productTokens Product Tokens
     */
    private void insertProductTokensWithHash( final List< ProductToken > productTokens ) {
        //logger.info("ASYNC PROCESS BATCH SIZE REACHED CALLING INSERT NOW  BATCH SIZE: " + productTokens.size());
        this.productTokenDAO.insertProductTokenWithHash( productTokens);

        //Cache the newly added product tokens hash values
        this.updateCache(productTokens);
    }


    /**
     * Submit to Task Executor
     * Catch TaskRejectedException and retry requests with a delay
     */
    @Retryable(retryFor = {TaskRejectedException.class, Exception.class}, maxAttempts = 5,  backoff = @Backoff(delay = 2000, multiplier = 2))
    private void submitTask(final List<ProductToken> finalProductTokens) {
        logger.debug("SUBMITTING BATCH TO TASKS EXECUTOR BATCH SIZE: " + finalProductTokens.size());
        this.taskExecutor.execute(() -> {
            logger.debug("TOTAL IN BATCH NOW: " + finalProductTokens.size());
            productTokenDAO.insertProductToken( finalProductTokens );

        });
        logger.debug("DONE SUBMITTING BATCH TO TASKS EXECUTOR BATCH SIZE: " + finalProductTokens.size());
    }

    /**
     * Submit to Task Executor
     * Catch TaskRejectedException and retry requests with a delay
     */
    @Retryable(retryFor = {TaskRejectedException.class, Exception.class}, maxAttempts = 5,  backoff = @Backoff(delay = 2000, multiplier = 2))
    private void submitTaskWithCache(final List<ProductToken> finalProductTokens) {
        logger.debug("SUBMITTING BATCH TO TASKS EXECUTOR BATCH SIZE: " + finalProductTokens.size());

        this.taskExecutor.execute(() -> {

            //logger.info("ASYNC PROCESS BATCH SIZE REACHED CALLING INSERT NOW  BATCH SIZE: " + productTokens.size());
            this.productTokenDAO.insertProductTokenWithHash( finalProductTokens.stream().distinct().toList());

            //Cache the newly added product tokens hash values
            this.updateCache(finalProductTokens);

        });
        logger.debug("DONE SUBMITTING BATCH TO TASKS EXECUTOR BATCH SIZE: " + finalProductTokens.size());
    }

    /**
     * Update Cache
     * @param finalProductTokens List of Product Tokens that needs to be updated in the cache
     */
    private void updateCache( final List<ProductToken> finalProductTokens ) {
        //Cache the newly added product tokens hash values
        Map<String, Set<Long>> productTokenSet = finalProductTokens.stream().collect(
                Collectors.groupingBy(
                        ProductToken::getRegionCode,
                        Collectors.mapping(ProductToken::getProductTokenValuesHash, Collectors.toSet()
                        )));

        for( Map.Entry<String,Set<Long>> entry : productTokenSet.entrySet()){
            AsyncDataLoaderService.regionProductTokenMap.compute(entry.getKey(),(key, list) ->  {
                if ( list == null ) {
                    list = new HashSet<>();
                }
                list.addAll( entry.getValue());
                return list;
            });
        }
    }

    /**
     * Check All Tasks Completed
     * Wait for all tasks to complete
     */
    private void checkAllTasksCompleted() {
        while (this.taskExecutor.getActiveCount() > 0) {
            try {
                logger.info("Waiting for all in tasks executor to complete: Active Counts: "+this.taskExecutor.getActiveCount()+ " Core Pool Size: "+this.taskExecutor.getCorePoolSize() + " Pool Size: "+this.taskExecutor.getPoolSize());
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException err) {
                logger.error("Error waiting for tasks to complete", err);
            }
        }
    }
}
