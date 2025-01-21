package com.yeshwanth;

import com.yeshwanth.entites.Product;
import com.yeshwanth.respository.ProductRespository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ProductProcessConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductProcessConfig.class);
    private static final String FILE_PATH_KEY = "input.file.name";

    @Autowired
    private ProductRespository productRespository;

    @Bean
    public Job productImportJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        LOGGER.info("Initializing job: Products-Import-Job");
        return new JobBuilder("Products-Import-Job", jobRepository)
                .start(productImportStep(jobRepository, transactionManager))
                .listener(jobExecutionListener())
                .build();
    }

    @Bean
    public Step productImportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        LOGGER.info("Configuring step: Products-Import-Step");
        return new StepBuilder("Products-Import-Step", jobRepository)
                .<Product, Product>chunk(100, transactionManager)
                .reader(productItemReader(null)) // Reader will be appropriately scoped
                .processor(productItemProcessor())
                .writer(productItemWriter())
                .build();
    }

    private JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                LOGGER.info("Preparing to execute job: {}", jobExecution.getJobInstance().getJobName());
                String filePath = jobExecution.getJobParameters().getString(FILE_PATH_KEY);
                jobExecution.getExecutionContext().putString(FILE_PATH_KEY, filePath);
                LOGGER.info("Input file path set in execution context: {}", filePath);
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                LOGGER.info("Job finished with status: {}", jobExecution.getStatus());
            }
        };
    }

    @Bean
    @StepScope  // Step scope ensures the bean is initialized per step execution
    public FlatFileItemReader<Product> productItemReader(@Value("#{jobParameters['input.file.name']}") String filePath) {
        LOGGER.info("Creating FlatFileItemReader for Product with file path: {}", filePath);

        if (filePath == null) {
            throw new IllegalArgumentException("The file path for the product data is not set or is invalid.");
        }

        return new FlatFileItemReaderBuilder<Product>()
                .name("productItemReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .lineMapper(productLineMapper())
                .targetType(Product.class)
                .build();
    }

    private LineMapper<Product> productLineMapper() {
        LOGGER.info("Configuring LineMapper for Product class");

        DefaultLineMapper<Product> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(createDelimitedLineTokenizer());
        lineMapper.setFieldSetMapper(createFieldSetMapper());

        return lineMapper;
    }

    private DelimitedLineTokenizer createDelimitedLineTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setStrict(false);
        tokenizer.setNames("name", "description", "price", "quantity");
        return tokenizer;
    }

    private BeanWrapperFieldSetMapper<Product> createFieldSetMapper() {
        BeanWrapperFieldSetMapper<Product> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Product.class);
        return fieldSetMapper;
    }

    @Bean
    public ItemProcessor<Product, Product> productItemProcessor() {
        return product -> {
            LOGGER.debug("Processing product: {}", product);
            return product;  // Add any transformations here if required
        };
    }

    private RepositoryItemWriter<Product> productItemWriter() {
        LOGGER.info("Configuring RepositoryItemWriter for Product");
        RepositoryItemWriter<Product> writer = new RepositoryItemWriter<>();
        writer.setRepository(productRespository);
        writer.setMethodName("save");
        return writer;
    }
}