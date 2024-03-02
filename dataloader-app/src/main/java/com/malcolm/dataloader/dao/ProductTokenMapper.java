package com.malcolm.dataloader.dao;

import com.malcolm.dataloader.ProductToken;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.retry.annotation.Retryable;

import java.sql.SQLException;
import java.util.List;

/**
 * Product Token Mapper
 *
 * @author Malcolm
 */
@Mapper
public interface ProductTokenMapper {

    /**
     * Insert Product Token
	 * <p/>
	 * The INSERT_PRODUCT_TOKEN query checks for the existence of each product token in the database before inserting it. 
	 * Depending on the size of your product_tokens table and the number of product tokens we are inserting, 
	 * this could potentially be a performance bottleneck, will need to adjust batch size.
	 */
    String INSERT_PRODUCT_TOKEN = """
        <script>
            INSERT INTO product_tokens(product_code,region_code,product_serial,product_token_name,product_token_values,product_token_values_hash)
            SELECT V.product_code, V.region_code, V.product_serial, V.product_token_name,V.product_token_values,V.product_token_values_hash
            FROM (
                    VALUES
                    <foreach collection="productTokens" item="productToken" separator=",">
                        (
                            #{productToken.productCode},
                            #{productToken.regionCode},
                            #{productToken.productSerial},
                            #{productToken.productTokenName},
                            #{productToken.productTokenValues},
                            CHECKSUM(#{productToken.productTokenValues})
                        )
                    </foreach>
            )
            AS V(product_code, region_code, product_serial,product_token_name,product_token_values,product_token_values_hash)
            WHERE NOT EXISTS (SELECT 1 FROM product_tokens WHERE region_code = V.region_code AND product_token_values_hash = V.product_token_values_hash )
        </script>
    """;
    //@DataLoaderExecutionTimeLogger
    @Insert(INSERT_PRODUCT_TOKEN)
    @Retryable(retryFor = {SQLException.class, Exception.class})
    void insert(List<ProductToken> productTokens);

     /**
     * Insert Product Token with Hash Value. The token hash values is calculated on the server not the database
	 *  <p/>
	 * The INSERT_PRODUCT_TOKEN query checks for the existence of each product token in the database before inserting it. 
	 * Depending on the size of your product_tokens table and the number of product tokens we are inserting, 
	 * this could potentially be a performance bottleneck, will need to adjust batch size.
	 */
    String INSERT_PRODUCT_TOKEN_WITH_HASH = """
        <script>
            INSERT INTO product_tokens(product_code,region_code,product_serial,product_token_name,product_token_values,product_token_values_hash)
            SELECT V.product_code, V.region_code, V.product_serial, V.product_token_name,V.product_token_values,V.product_token_values_hash
            FROM (
                    VALUES
                    <foreach collection="productTokens" item="productToken" separator=",">
                        (
                            #{productToken.productCode},
                            #{productToken.regionCode},
                            #{productToken.productSerial},
                            #{productToken.productTokenName},
                            #{productToken.productTokenValues},
                            #{productToken.productTokenValuesHash}
                        )
                    </foreach>
            )
            AS V(product_code, region_code, product_serial,product_token_name,product_token_values,product_token_values_hash)
            WHERE NOT EXISTS (SELECT 1 FROM product_tokens WHERE region_code = V.region_code AND product_token_values_hash = V.product_token_values_hash )
        </script>
    """;
    //@DataLoaderExecutionTimeLogger
    @Insert(INSERT_PRODUCT_TOKEN_WITH_HASH)
    @Retryable(retryFor = {SQLException.class, Exception.class})
    void insertWithHash(List<ProductToken> productTokens);

    /**
     * Select Product Token Hash
     */
    String SELECT_PRODUCT_TOKEN_HASH = """
           SELECT
                    product_token_values_hash
           FROM
                    product_tokens
           WHERE
                    region_code = #{regionCode}
    """;
    //@DataLoaderExecutionTimeLogger
    @Select(SELECT_PRODUCT_TOKEN_HASH)
    List<Long> selectProductTokensByRegionCode(@Param(value = "regionCode") String regionCode);
}
