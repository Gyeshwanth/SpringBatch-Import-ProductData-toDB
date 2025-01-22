package com.yeshwanth;

import com.yeshwanth.entites.Product;
import com.yeshwanth.respository.ProductRespository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;

import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;

@Configuration
public class GenerateProductFile {

    private static final Logger logger = LoggerFactory.getLogger(GenerateProductFile.class);

    @Autowired
    ProductRespository productRespository;

    @Bean
    public RepositoryItemReader<Product> repositoryItemReader() {
        logger.info("Initializing RepositoryItemReader...");
        RepositoryItemReader<Product> reader = new RepositoryItemReader<>();
        reader.setRepository(productRespository);
        reader.setMethodName("findAll");
        reader.setPageSize(10);
        reader.setSort(Collections.singletonMap("id", Sort.Direction.ASC));
        logger.info("RepositoryItemReader initialized successfully.");
        return reader;
    }

    @Bean
    public FlatFileItemWriter<Product> writer() {
        FlatFileItemWriter<Product> writer = new FlatFileItemWriter<>();

        writer.setResource(new FileSystemResource("products.csv"));

        // Header Callback
        writer.setHeaderCallback(writerCallback -> writerCallback.write("id,name,description,price,quantity"));

        // Line Aggregator for mapping fields
        writer.setLineAggregator(new DelimitedLineAggregator<>() {
            {
                setDelimiter(",");
                setFieldExtractor(product -> new Object[]{
                        product.getId(),            // Maps to 'id'
                        product.getName(),          // Maps to 'name'
                        product.getDescription(),   // Maps to 'description'
                        product.getPrice(),         // Maps to 'price'
                        product.getQuantity()       // Maps to 'quantity'
                });
            }
        });

        return writer;
    }

    @Bean
    public Step productExportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        logger.info("Creating productExportStep...");
        Step step = new StepBuilder("Products-Export-Step", jobRepository)
                .<Product, Product>chunk(100, transactionManager)
                .reader(repositoryItemReader())
                .writer(writer())
                .build();
        logger.info("productExportStep created successfully.");
        return step;
    }

    @Bean
    public Job exportProductJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        logger.info("Creating exportProductJob...");
        Job job = new JobBuilder("Products-Export-Job", jobRepository)
                .start(productExportStep(jobRepository, transactionManager))
                .build();
        logger.info("exportProductJob created successfully.");
        return job;
    }
}