package com.malcolm.dataloader;

/**
 * Data Loader Response
 *
 * @author Malcolm
 */
public class DataLoaderResponse {

    /**
     * Error Code
     */
    private Integer errorCode;

    /**
     * Error Message
     */
    private String errorMessage;

    /**
     * Get Error Code
     * @return errorCode Error Code
     */
    @SuppressWarnings( "unused" )
    public Integer getErrorCode() {
        return errorCode;
    }

    /**
     * Set Error Code
     * @param errorCode Error Code
     */
    public void setErrorCode(final Integer errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Get Error Message
     * @return Error Message
     */
    @SuppressWarnings( "unused" )
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Set Error Message
     * @param errorMessage Error Message
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
