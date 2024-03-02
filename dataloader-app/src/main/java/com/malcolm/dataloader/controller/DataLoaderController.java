package com.malcolm.dataloader.controller;

import com.malcolm.dataloader.DataLoaderException;
import com.malcolm.dataloader.DataLoaderExecutionTimeLogger;
import com.malcolm.dataloader.DataLoaderResponse;
import com.malcolm.dataloader.service.AsyncDataLoaderService;
import com.malcolm.dataloader.service.DataLoaderRateLimiterService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Spring Boot Rest Controller
 *
 * @author Malcolm
 */
@RestController
@RequestMapping("/dataloader/api")
public class DataLoaderController {

    /**
     * The logger for this class
     */
    private final Logger logger = LoggerFactory.getLogger(DataLoaderController.class);

    /**
     * Async Data Loader Service
     */
    private final AsyncDataLoaderService asyncDataLoaderService;

    /**
     * Rate Limiter Service
     */
    private final DataLoaderRateLimiterService dataLoaderRateLimiterService;

    /**
     * Constructor
     *
     * @param asyncDataLoaderService       Async Data Loader Service
     * @param dataLoaderRateLimiterService DataLoaderRateLimiterService
     */
    public DataLoaderController(AsyncDataLoaderService asyncDataLoaderService, DataLoaderRateLimiterService dataLoaderRateLimiterService) {
        this.asyncDataLoaderService = asyncDataLoaderService;
        this.dataLoaderRateLimiterService = dataLoaderRateLimiterService;
    }

    /**
     * Upload CSV file and process it asynchronously
     *
     * @param csvFile CSV File to Process
     * @return ResponseEntity DataLoaderResponse
     * @throws DataLoaderException Data Loader Exception
     */
    @Tag(name = "Sequential", description = "Upload and process CSV file Asynchronously using sequential processing")
    @DataLoaderExecutionTimeLogger
    @Timed(value = "dataloader.upload-sequential")
    @PostMapping(path = "/upload-sequential", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<DataLoaderResponse> uploadSequential(@RequestParam(value = "file") MultipartFile csvFile) throws DataLoaderException {
        try {
            ResponseEntity<DataLoaderResponse>  response = this.validate(csvFile);
            if(response != null){
                return response;
            }
            final File tempFile = this.copyToTempFile(csvFile);
            this.asyncDataLoaderService.processCSVFileAsync(tempFile, false);
            return this.returnSuccess();

        } catch (IOException err) {
            this.dataLoaderRateLimiterService.resetRequestCounts();
            throw new DataLoaderException(101,"Error processing CSV file", err);
        }
    }

    /**
     * Upload CSV file and process it asynchronously using Task Executor so that batches are processed in parallel with multiple threads
     *
     * @param csvFile CSV File to Process
     * @return ResponseEntity DataLoaderResponse
     * @throws DataLoaderException Data Loader Exception
     */
    @Tag(name = "Parallel", description = "Upload and process CSV file Asynchronously using Task Executor where batches processed in parallel with multiple threads")
    @DataLoaderExecutionTimeLogger
    @Timed(value = "dataloader.upload-parallel")
    @PostMapping(path = "/upload-parallel", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<DataLoaderResponse> uploadParallel(@RequestParam(value = "file") MultipartFile csvFile) throws DataLoaderException {
        try {
            ResponseEntity<DataLoaderResponse>  response = this.validate(csvFile);
            if(response != null){
                return response;
            }
            //Transfer this file to a temporary file, we are going to start async process
            //Spring will clean up the MultipartFile and we may occasionally get an invalid file handle
            final File tempFile = this.copyToTempFile(csvFile);
            this.asyncDataLoaderService.processCSVFileAsync(tempFile, true);
            return this.returnSuccess();

        } catch (IOException err) {
            this.dataLoaderRateLimiterService.resetRequestCounts();
            throw new DataLoaderException(101,"Error processing CSV file", err);
        }
    }

    /**
     * Upload CSV file and process it asynchronously by comparing data on server side using a cache only submitting new data to the database
     *
     * @param csvFile CSV File to Process
     * @return ResponseEntity DataLoaderResponse
     * @throws DataLoaderException Data Loader Exception
     */
    @Tag(name = "Sequential-Cache", description = "Upload and process CSV by fetching data from the server and comparing server side by caching hash values")
    @DataLoaderExecutionTimeLogger
    @Timed(value = "dataloader.upload-sequential-cache")
    @PostMapping(path = "/upload-sequential-cache", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<DataLoaderResponse> uploadSequentialCache(@RequestParam(value = "file") MultipartFile csvFile) throws DataLoaderException {
        try {
            ResponseEntity<DataLoaderResponse>  response = this.validate(csvFile);
            if(response != null){
                return response;
            }
            //Transfer this file to a temporary file, we are going to start async process
            //Spring will clean up the MultipartFile and we may occasionally get an invalid file handle
            final File tempFile = this.copyToTempFile(csvFile);
            this.asyncDataLoaderService.processCSVFileByCompare(tempFile, false);

            return this.returnSuccess();

        } catch (IOException err) {
            this.dataLoaderRateLimiterService.resetRequestCounts();
            throw new DataLoaderException(101,"Error processing CSV file", err);
        }
    }

    @Tag(name = "Parallel-Cache", description = "Upload and process CSV by fetching data from the server and comparing server side...very bad idea....")
    @DataLoaderExecutionTimeLogger
    @Timed(value = "dataloader.upload-parallel-cache")
    @PostMapping(path = "/upload-parallel-cache", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<DataLoaderResponse> uploadParallelCache(@RequestParam(value = "file") MultipartFile csvFile) throws DataLoaderException {
        try {
            ResponseEntity<DataLoaderResponse>  response = this.validate(csvFile);
            if(response != null){
                return response;
            }
            //Transfer this file to a temporary file, we are going to start async process
            //Spring will clean up the MultipartFile and we may occasionally get an invalid file handle
            final File tempFile = this.copyToTempFile(csvFile);
            this.asyncDataLoaderService.processCSVFileByCompare(tempFile, true);

            return this.returnSuccess();
        } catch (IOException err) {
            this.dataLoaderRateLimiterService.resetRequestCounts();
            throw new DataLoaderException(101,"Error processing CSV file", err);
        }
    }

    /**
     * Upload CSV file and process it using spring batch JobLauncher asynchronously this will return response to the client without blocking
     *
     * @param csvFile CSV File to Process
     * @return ResponseEntity DataLoaderResponse
     * @throws DataLoaderException Data Loader Exception
     */
    @Tag(name = "Spring-Batch", description = "Upload and process CSV file Asynchronously using Spring Batch")
    @DataLoaderExecutionTimeLogger
    @Timed(value = "dataloader.upload-springbatch")
    @PostMapping(path = "/upload-springbatch", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<DataLoaderResponse> uploadSpringBatch(@RequestParam(value = "file") MultipartFile csvFile) throws DataLoaderException {
        try {

            ResponseEntity<DataLoaderResponse>  response = this.validate(csvFile);
            if(response != null){
                return response;
            }

            //Transfer this file to a temporary file, we are going to start async process
            //Spring will clean up the MultipartFile and we may occasionally get an invalid file handle
            final File tempFile = this.copyToTempFile(csvFile);
            this.asyncDataLoaderService.runImportProductTokensBatchJob(tempFile.getAbsolutePath());
            return this.returnSuccess();
        } catch (IOException err) {
            this.dataLoaderRateLimiterService.resetRequestCounts();
            throw new DataLoaderException(101,"Error processing CSV file", err);
        }
    }


    /**
     * Return Successful response
     * @return ResponseEntity<DataLoaderResponse> Response
     */
    private ResponseEntity<DataLoaderResponse> returnSuccess(){
        DataLoaderResponse response = new DataLoaderResponse();
        response.setErrorCode(0);
        response.setErrorMessage("CSV File Processed Successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Check CSV File
     * @param csvFile MultipartFile CSV File
     * @return ResponseEntity<DataLoaderResponse> Response
     */
    private ResponseEntity<DataLoaderResponse> validate(final MultipartFile csvFile){
        if (csvFile.isEmpty() || !csvFile.getResource().isReadable()) {
            logger.info("Invalid CSF File, request not processed");
            DataLoaderResponse dataLoaderResponse = new DataLoaderResponse();
            dataLoaderResponse.setErrorCode(100);
            dataLoaderResponse.setErrorMessage("Invalid CSV file for processing");
            return new ResponseEntity<>(dataLoaderResponse, HttpStatus.BAD_REQUEST);
        }
        logger.info("Currently Processing Request "+this.dataLoaderRateLimiterService.currentRequestCounts());
        return null;
    }

    /**
     * Copy to Temp File
     *
     * @param csvFile MultipartFile File
     * @return File temp temporary file with content
     * @throws IOException IO Exception
     */
    private File copyToTempFile(final MultipartFile csvFile) throws IOException {
        //Transfer this file to a temporary file, we are going to start async process
        //Spring will clean up the MultipartFile and we may occasionally get an invalid file handle
        String filePath = "product_tokens_" + System.currentTimeMillis() + UUID.randomUUID();
        File tempFile = File.createTempFile(filePath, ".csv");
        tempFile.deleteOnExit();
        if (tempFile.exists()) {
            csvFile.getInputStream().transferTo(new FileOutputStream(tempFile));
        } else {
            throw new IOException("Error processing CSV File");
        }
        return tempFile;
    }
}