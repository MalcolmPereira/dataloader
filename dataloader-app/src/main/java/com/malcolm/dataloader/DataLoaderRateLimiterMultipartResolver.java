package com.malcolm.dataloader;

import com.malcolm.dataloader.service.DataLoaderRateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 * This class is used to handle multipart requests and rate limit the requests.
 *
 */
@Component
public class DataLoaderRateLimiterMultipartResolver extends StandardServletMultipartResolver {

	/**
	 * The logger for this class
	 */
	private final Logger logger = LoggerFactory.getLogger( DataLoaderRateLimiterMultipartResolver.class);

	/**
	 * Rate Limiter Service
	 */
	private final DataLoaderRateLimiterService dataLoaderRateLimiterService;

	/**
	 * Public Constructor
	 * @param dataLoaderRateLimiterService DataLoader Rate Limiter Service
	 */
	public DataLoaderRateLimiterMultipartResolver( DataLoaderRateLimiterService dataLoaderRateLimiterService) {
		this.dataLoaderRateLimiterService = dataLoaderRateLimiterService;
	}

	@Override
	public @NonNull MultipartHttpServletRequest resolveMultipart( HttpServletRequest request) throws MultipartException {
		if (request.getRequestURI() !=null && request.getRequestURI().contains("upload") && !this.dataLoaderRateLimiterService.canProcessUploads()){
			logger.info( "FILE UPLOAD NOT QUEUED - Rate limit exceeded for uploads CURRENT COUNTS: " +this.dataLoaderRateLimiterService.currentRequestCounts());
			throw new MultipartException("Rate limit exceeded");
		}
		logger.info( "FILE UPLOAD QUEUED CURRENT PROCESS COUNT: " +this.dataLoaderRateLimiterService.currentRequestCounts());
		return super.resolveMultipart(request);
	}
}
