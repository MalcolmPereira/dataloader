package com.malcolm.dataloader.dao;

import com.malcolm.dataloader.ProductToken;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.List;

/**
 * DAO for Product Token
 *
 * @author Malcolm
 */
@Repository("ProductTokenDao")
public class ProductTokenDAO {

    /**
     * Product Token Mapper
     */
    private final ProductTokenMapper productTokenMapper;

    /**
     * Constructor
     * @param productTokenMapper Product Token Mapper
     */
    public ProductTokenDAO(ProductTokenMapper productTokenMapper) {
        this.productTokenMapper = productTokenMapper;
    }

    /**
     * Insert Product Token
     * @param productTokens Product Tokens
     */
    //@DataLoaderExecutionTimeLogger
    @Retryable(retryFor = {SQLException.class, Exception.class, SocketException.class, SocketTimeoutException.class}, maxAttempts = 5)
    public void insertProductToken(List<ProductToken> productTokens) {
        this.productTokenMapper.insert(productTokens);
    }

    /**
     * Insert Product Token
     * @param productTokens Product Tokens
     */
    //@DataLoaderExecutionTimeLogger
    @Retryable(retryFor = {SQLException.class, Exception.class, SocketException.class, SocketTimeoutException.class}, maxAttempts = 5)
    public void insertProductTokenWithHash(List<ProductToken> productTokens) {
        this.productTokenMapper.insertWithHash(productTokens);
    }


    /**
     * Select Product Token by Region Code
     * @param regionCode Region Codes
     * @return List<Integer> Region Code
     */
    //@DataLoaderExecutionTimeLogger
    public List<Long> getProductTokensByRegionCode(String regionCode){
        return this.productTokenMapper.selectProductTokensByRegionCode(regionCode);
    }
}
