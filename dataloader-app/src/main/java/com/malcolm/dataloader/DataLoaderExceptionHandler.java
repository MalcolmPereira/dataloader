package com.malcolm.dataloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception Handler for Data Loader API
 *
 * @author Malcolm
 */
@RestControllerAdvice(basePackages = "com.malcolm.dataloader")
public class DataLoaderExceptionHandler {

    /**
     * The logger for this class
     */
    private final Logger logger = LoggerFactory.getLogger( DataLoaderExceptionHandler.class);

    /**
     * Handle exception when CSV file processing
     *
     * @return DataLoaderResponse Data Loader Response
     */
    @ExceptionHandler({DataLoaderException.class,RuntimeException.class,Exception.class,})
    public ResponseEntity<DataLoaderResponse> processException(Exception err) {
        logger.error( "Error processing CSV file: "+err.getMessage());
        final DataLoaderResponse response = new DataLoaderResponse();
        if(err instanceof DataLoaderException){
            response.setErrorCode(((DataLoaderException)err).getErrorCode());
            response.setErrorMessage(err.getMessage());
        }else{
            response.setErrorCode(500);
            response.setErrorMessage("Error processing CSV file");
        }
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
