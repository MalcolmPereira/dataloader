package com.malcolm.dataloader.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is responsible for controlling number of concurrent upload requests
 * that can be processed by this instance
 *
 * @author Malcolm
 */

@Service
public class DataLoaderRateLimiterService {
    /**
     * Lock
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Request Counter
     */
    private final AtomicInteger requestCounter;

    /**
     * Max supported Requests
     */
    private final Integer maxRequest;

    /**
     * Public Constructor
     */
    public DataLoaderRateLimiterService(Environment env){
        int MAX_REQUESTS = 10;
        try{
            MAX_REQUESTS = Integer.parseInt(Objects.requireNonNull(env.getProperty("dataloader.max.requests")));
        }catch(NullPointerException err){
            System.err.println("No Max Requests Configured Default: "+MAX_REQUESTS);
        }
        this.maxRequest = MAX_REQUESTS;
        this.requestCounter = new AtomicInteger(this.maxRequest);
    }

    /**
     * Current Request Counts
     * @return int current request counts
     */
    public int currentRequestCounts() {
        return this.requestCounter.get();
    }

    /**
     * Check if the Upload Request is permitted and Decrement
     * @return boolean - Allowed True, not otherwise
     */
    public boolean canProcessUploads() {
        try{
            lock.lock();
            if( this.requestCounter.get() > 0){
                this.requestCounter.decrementAndGet();
                return true;
            }
            return false;
        }finally {
            lock.unlock();
        }
    }

    /**
     * Reset Request Counts
     */
    public void resetRequestCounts() {
        try{
            lock.lock();
            if( this.requestCounter.get() < this.maxRequest){
                this.requestCounter.incrementAndGet();
            }
        }finally {
            lock.unlock();
        }
    }
}
