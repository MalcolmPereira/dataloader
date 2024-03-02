package com.malcolm.dataloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartException;

/**
 * Multipart Exception Handler for Data Loader API
 *
 * @author Malcolm
 */
@ControllerAdvice
public class DataLoaderMultiPartExceptionHandler {

	/**
	 * The logger for this class
	 */
	private final Logger logger = LoggerFactory.getLogger( DataLoaderMultiPartExceptionHandler.class);

	@ResponseBody
	@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
	@ExceptionHandler( MultipartException.class)
	public ResponseEntity<DataLoaderResponse>  handleMultipartException( MultipartException err) {
		logger.error( "Multipart Exception: "+err.getMessage());
		final DataLoaderResponse response = new DataLoaderResponse();
		response.setErrorCode(429);
		response.setErrorMessage("Too many requests in progress, please try later");
		return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
	}
}
