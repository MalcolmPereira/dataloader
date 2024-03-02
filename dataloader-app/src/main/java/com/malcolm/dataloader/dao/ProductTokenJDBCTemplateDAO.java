package com.malcolm.dataloader.dao;

import com.malcolm.dataloader.ProductToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DAO for Product Token Using JDBC Template
 *
 * @author Malcolm
 */
@Repository("ProductTokenJDBCTemplateDAO")
public class ProductTokenJDBCTemplateDAO {

    /**
     * Logger for the class
     */
    private final Logger log = LoggerFactory.getLogger(ProductTokenJDBCTemplateDAO.class);


    /**
     * JdbcTemplate to execute the SQL
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor to inject the JdbcTemplate
     *
     * @param jdbcTemplate JdbcTemplate to execute the SQL
     */
    public ProductTokenJDBCTemplateDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Insert SQL Statement Prefix
     */
    private static final String INSERT_PRODUCT_TOKEN_SQL_PREFIX = """
         INSERT INTO product_tokens(product_code,region_code,product_serial,product_token_name,product_token_values,product_token_values_hash)
         SELECT V.product_code, V.region_code, V.product_serial, V.product_token_name,V.product_token_values,V.product_token_values_hash
         FROM (
             VALUES
    """;

    /**
     * Insert SQL Statement Suffix
     */
    private static final String INSERT_PRODUCT_TOKEN_SQL_SUFFIX = """
        )
        AS V(product_code, region_code, product_serial,product_token_name,product_token_values,product_token_values_hash)
        WHERE NOT EXISTS (SELECT 1 FROM product_tokens WHERE region_code = V.region_code AND product_token_values_hash = V.product_token_values_hash )
    """;

    //@DataLoaderExecutionTimeLogger
    @Retryable(retryFor = {SQLException.class, SocketException.class, SocketTimeoutException.class,Exception.class}, maxAttempts = 5)
    public void insertProductToken(List<ProductToken> productTokens) {
        final StopWatch stopWatch = new StopWatch("insertProductToken JDBC Template");
        stopWatch.start("insertProductToken JDBC Template");
        final StringBuilder stringBuilder  = new StringBuilder();
        stringBuilder.append(INSERT_PRODUCT_TOKEN_SQL_PREFIX);
        List<String> arguments = new ArrayList<>();
        for(ProductToken productToken : productTokens){
            stringBuilder.append("(");
            stringBuilder.append("?,");
            stringBuilder.append("?,");
            stringBuilder.append("?,");
            stringBuilder.append("?,");
            stringBuilder.append("?,");
            stringBuilder.append("CHECKSUM(");
            stringBuilder.append("?");
            stringBuilder.append(")");
            stringBuilder.append("),");
            arguments.add(productToken.getProductCode());
            arguments.add(productToken.getRegionCode());
            arguments.add(productToken.getProductSerial()+"");
            arguments.add(productToken.getProductTokenName());
            arguments.add(productToken.getProductTokenValues());
            arguments.add(productToken.getProductTokenValues());
        }
        stringBuilder.deleteCharAt(stringBuilder.length()-1);
        stringBuilder.append(INSERT_PRODUCT_TOKEN_SQL_SUFFIX);
        jdbcTemplate.update(stringBuilder.toString(), arguments.toArray());
        stopWatch.stop();
        log.debug("DONE EXECUTION BATCH OF SIZE "+productTokens.size());
        log.debug("TOTAL EXECUTION TIME FOR JDBC Template " + stopWatch.getTotalTimeMillis() + "ms, "+ stopWatch.getTotalTimeSeconds()+" seconds, " + stopWatch.getTotalTime(TimeUnit.MINUTES)+ " minutes");
    }


}
