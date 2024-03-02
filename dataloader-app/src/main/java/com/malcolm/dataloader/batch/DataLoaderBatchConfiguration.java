package com.malcolm.dataloader.batch;

import com.malcolm.dataloader.ProductToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;

/**
 * This is the Configuration class for the Batch Execution of the DataLoader Job
 *
 * @author Malcolm
 */
@Configuration
@EnableBatchProcessing
public class DataLoaderBatchConfiguration  {
    /**
     * The batch size for submitting the data to the database
     */
    @Value("${dataloader.batch.size}")
    private int BATCH_SIZE;

    /**
     *Logger for the DataLoaderBatchConfiguration class
     */
    private final Logger log = LoggerFactory.getLogger(DataLoaderBatchConfiguration.class);

    /**
     * Constructor for the DataLoaderBatchConfiguration class
     */
    public DataLoaderBatchConfiguration(){
    }

    /**
     * This is the Reader for Batch Job. This will read the CSV at the configured path
     * and process each item line by line
     * @param filePath File location parameter value, the filePath variable is set when the Job us launched
     * @return FlatFileItemReader which returns a Java Bean of ProductToken
     */
    @Bean
    @StepScope
    public FlatFileItemReader<ProductToken> reader(@Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemReaderBuilder<ProductToken>()
                .name("productTokenItemReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)  // skip the header line
                .delimited()
                .names("productCode", "regionCode", "productSerial", "productTokenName", "productTokenValues")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(ProductToken.class);
                }})
                .build();
    }

    /**
     * Item processor for any transformation or business rules, in this case it just does logging
     * @return ItemProcessor for ProductToken
     */
    @Bean
    public ItemProcessor<ProductToken, ProductToken> processor() {
        return (item) -> {
            log.info("Product Code " + item.getProductCode());
            log.info("Region Code "+item.getRegionCode());
            log.info("Product Serial "+item.getProductSerial());
            log.info("Product Token Name "+item.getProductTokenName());
            log.info("Product Token Values "+item.getProductTokenValues());
            return item;
        };
    }

    /**
     * The Batch Jon
     * @param jobRepository Job  Repository
     * @param step Job Step
     * @param listener  Listener
     * @return Job Data Loader Batch Job
     */
    @Bean
    public Job importProductTokens(JobRepository jobRepository, Step step, DataLoaderJobCompletionListener listener) {
        return new JobBuilder("importProductTokens", jobRepository)
                .listener(listener)
                .start(step)
                .build();
    }

    /**
     * Data Loader Job Step
     * @param jobRepository Job Repository for storing and recovering Jobs
     * @param transactionManager DataSource Transaction Manager
     * @param reader Reader that will process CSV file one record at a time
     * @param writer Writer that will batch records and then insert to database
     * @return Step for the Batch Job
     */
    @Bean
    public Step step(JobRepository jobRepository, DataSourceTransactionManager transactionManager, FlatFileItemReader<ProductToken> reader, ProductTokenItemWriter<ProductToken> writer) {
        return new StepBuilder("step", jobRepository)
                .<ProductToken, ProductToken> chunk(BATCH_SIZE,transactionManager)
                .reader(reader)
                .writer(writer)
                .faultTolerant() // Enable fault tolerance
                .retry(UncategorizedSQLException.class)
                .retry(SQLException.class)
                .retry(SocketException.class)
                .retry(SocketTimeoutException.class)
                .retry(Exception.class)
                .retryLimit(3) // Retry 3 times
                .build();
    }
}
