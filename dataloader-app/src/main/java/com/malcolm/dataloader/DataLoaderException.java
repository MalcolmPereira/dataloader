package com.malcolm.dataloader;

/**
 * Data Loader Exception
 * @author Malcolm
 */
public class DataLoaderException extends Exception {

    /**
     * Error Code
     */
    private final Integer errorCode;

    /**
     * DataLoaderException Constructor
     * @param errorMessage Error Message
     * @param err Exception
     */
    public DataLoaderException(Integer errorCode, String errorMessage, Throwable err) {

        super(errorMessage,err);
        this.errorCode = errorCode;
    }

    public Integer getErrorCode() {
        return errorCode;
    }
}
