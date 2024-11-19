package com.example.springbatch.jobs.MyBatisItemReader;

import com.example.springbatch.jobs.models.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MyBatisBatchWriterJobConfig {

    private static final int CHUNK_SIZE = 10;
    private static final String JOB_NAME = "MYBATIS_BATCH_JOB";
    private final SqlSessionFactory sqlSessionFactory;

    @Bean
    public FlatFileItemReader<Customer> customerFileReader() {
        log.info("Initializing FlatFileItemReader...");
        return new FlatFileItemReaderBuilder<Customer>()
                .name("customerFileReader")
                .resource(new ClassPathResource("static/customer.csv"))
                .delimited()
                .delimiter(",")
                .names("id", "name", "age", "gender")
                .linesToSkip(1) //  header 건너뛰기 추가해야함
                .fieldSetMapper(fields -> {
                    log.info("Reading fields: {}", fields);
                    Customer customer = new Customer();
                    customer.setId(fields.readLong("id"));
                    customer.setName(fields.readString("name"));
                    customer.setAge(fields.readInt("age"));
                    customer.setGender(fields.readString("gender"));
                    return customer;
                })
                .build();
    }


    @Bean
    public MyBatisBatchItemWriter<Customer> mybatisItemWriter() {
        return new MyBatisBatchItemWriterBuilder<Customer>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("com.example.springbatch.jobs.MyBatisItemReader.insertCustomers")
                .build();
    }

    @Bean
    public Step customerBatchStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("customerBatchStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerFileReader())
                .processor(customer -> {
                    log.info("Processing customer: {}", customer);
                    return customer;
                })
                .writer(mybatisItemWriter())
                .build();
    }

    @Bean
    public Job customerBatchJob(Step customerBatchStep, JobRepository jobRepository) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerBatchStep)
                .build();
    }
}
