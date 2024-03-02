package com.malcolm.dataloader.batch;

import com.malcolm.dataloader.ProductToken;
import com.malcolm.dataloader.dao.ProductTokenJDBCTemplateDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for writing the product token data to the database.
 * It uses a batch update to insert the data into the database.
 * It also uses a retryable annotation to retry the operation in case of a SQLException or Exception.
 *
 * @author Malcolm
 */
@Component
public class ProductTokenItemWriter<ProductTokenChunk> implements ItemWriter<ProductTokenChunk> {

    /**
     * Logger for the class
     */
    private final Logger log = LoggerFactory.getLogger(ProductTokenItemWriter.class);

    /**
     * The product token data access object
     */
    private final ProductTokenJDBCTemplateDAO productTokenJDBCTemplateDAO;


    public ProductTokenItemWriter(ProductTokenJDBCTemplateDAO productTokenJDBCTemplateDAO){
        this.productTokenJDBCTemplateDAO = productTokenJDBCTemplateDAO;
    }


    /**
     * This method is responsible for writing the product token data to the database.
     * It uses a batch update to insert the data into the database.
     * It also uses a retryable annotation to retry the operation in case of a SQLException or Exception.
     *
     * @param chunk Chunk of product token data to write to the database
     */
    @Retryable(retryFor = {SQLException.class, Exception.class})
    @Override
    public void write(final @NonNull Chunk<? extends ProductTokenChunk> chunk) {
        final StopWatch stopWatch = new StopWatch("ProductTokenItemWriter ");
        stopWatch.start("ProductTokenItemWriter");
        List<ProductTokenChunk> productTokensChunks = new ArrayList<>(chunk.getItems());
        List<ProductToken> productTokens = new ArrayList<>();
        for (ProductTokenChunk productTokenChunk : productTokensChunks) {
            productTokens.add( ((ProductToken)productTokenChunk));
        }
        log.debug("STARTING BATCH OF SIZE "+productTokens.size());
        this.productTokenJDBCTemplateDAO.insertProductToken(productTokens.stream().distinct().toList());
        stopWatch.stop();
        log.debug("DONE EXECUTION BATCH OF SIZE "+productTokens.size());
        log.debug("TOTAL EXECUTION TIME FOR ProductTokenItemWriter " + stopWatch.getTotalTimeMillis() + "ms, "+ stopWatch.getTotalTimeSeconds()+" seconds, " + stopWatch.getTotalTime(TimeUnit.MINUTES)+ " minutes");
    }
}
