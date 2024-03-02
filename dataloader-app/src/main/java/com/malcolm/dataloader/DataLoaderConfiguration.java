package com.malcolm.dataloader;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;

@Configuration
public class DataLoaderConfiguration {

	/**
	 * Rate Limiter Multipart Resolver
	 */
	private final DataLoaderRateLimiterMultipartResolver dataLoaderRateLimiterMultipartResolver;

	/**
	 * Public Constructor
	 * @param dataLoaderRateLimiterMultipartResolver DataLoader Rate Limiter Multipart Resolver
	 */
	public DataLoaderConfiguration( DataLoaderRateLimiterMultipartResolver dataLoaderRateLimiterMultipartResolver) {
		this.dataLoaderRateLimiterMultipartResolver = dataLoaderRateLimiterMultipartResolver;
	}

	/**
	 * Customer Multipart Resolver
	 * @return MultipartResolver
	 */
	@Bean
	public MultipartResolver multipartResolver() {
		return this.dataLoaderRateLimiterMultipartResolver;
	}
}
