package com.example.springbatch.jobs.flatfilereader;

import com.example.springbatch.jobs.models.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class FlatFileItemJobConfig {

    public static final int CHUNK_SIZE = 100;
    public static final String ENCODING = "UTF-8";
    public static final String FLAT_FILE_CHUNK_JOB = "FLAT_FILE_CHUNK_JOB";

    private final ConcurrentHashMap<String, Integer> aggregateCustomers = new ConcurrentHashMap<>();

    @Bean
    public FlatFileItemReader<Customer> flatFileItemReader() {
        return new FlatFileItemReaderBuilder<Customer>()
                .name("FlatFileItemReader")
                .resource(new ClassPathResource("static/customer.csv"))
                .encoding(ENCODING)
                .delimited()
                .delimiter(",")
                .names("name", "age", "gender")
                .targetType(Customer.class)
                .build();
    }

    /* 실습 1
    @Bean
    public FlatFileItemWriter<Customer> flatFileItemWriter() {
        return new FlatFileItemWriterBuilder<Customer>()
                .name("flatFileItemWriter")
                .resource(new FileSystemResource("./output/customer_new.csv"))
                .encoding(ENCODING)
                .delimited()
                .delimiter("\t")
                .names("Name", "Age", "Gender")
                .build();
    }
    */

    @Bean
    public FlatFileItemWriter<Customer> flatFileItemWriter() {
        return new FlatFileItemWriterBuilder<Customer>()
                .name("flatFileItemWriter")
                .resource(new FileSystemResource("./output/customer_new.csv"))
                .encoding(ENCODING)
                .delimited()
                .delimiter("\t")
                .names("Name", "Age", "Gender")
                .lineAggregator(new CustomerLineAggregator())
                .headerCallback(new CustomerHeader())
                .footerCallback(new CustomerFooter(aggregateCustomers))
                .build();
    }

    @Bean
    public ItemProcessor<Customer, Customer> aggregateCustomerProcessor() {
        return new AggregateCustomerProcessor(aggregateCustomers);
    }

    @Bean
    public Step flatFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init flatFileStep -----------------");

        return new StepBuilder("flatFileStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(flatFileItemReader())
                .processor(aggregateCustomerProcessor())
                .writer(flatFileItemWriter())
                .build();
    }

    @Bean
    public Job flatFileJob(Step flatFileStep, JobRepository jobRepository) {
        log.info("------------------ Init flatFileJob -----------------");

        return new JobBuilder("flatFileJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(flatFileStep)
                .build();
    }

}
