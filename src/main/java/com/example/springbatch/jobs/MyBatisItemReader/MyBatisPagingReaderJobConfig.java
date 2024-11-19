package com.example.springbatch.jobs.MyBatisItemReader;

import com.example.springbatch.jobs.models.Customer;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.mybatis.spring.batch.builder.MyBatisPagingItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class MyBatisPagingReaderJobConfig {

    public static final int CHUNK_SIZE = 2;
    public static final String ENCODING = "UTF-8";
    public static final String MYBATIS_PAGING_JOB = "MYBATIS_PAGING_JOB";

    @Autowired
    DataSource dataSource;

    @Autowired
    SqlSessionFactory sqlSessionFactory;

    @Bean
    public MyBatisPagingItemReader<Customer> myBatisPagingItemReader() throws Exception {
        return new MyBatisPagingItemReaderBuilder<Customer>()
                .sqlSessionFactory(sqlSessionFactory)
                .pageSize(CHUNK_SIZE)
                .queryId("com.example.springbatch.jobs.MyBatisItemReader.selectCustomers")
                .build();
    }

    @Bean
    public FlatFileItemWriter<Customer> customerFlatFileItemWriter2() {
        return new FlatFileItemWriterBuilder<Customer>()
                .name("customerFlatFileItemWriter2")
                .resource(new FileSystemResource("./output/customer_output.csv"))
                .encoding(ENCODING)
                .delimited()
                .delimiter("\t")
                .names("name", "age", "gender")
                .build();
    }

    @Bean
    public Step customerMyBatisPagingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        log.info("Initializing customerMyBatisPagingStep...");

        return new StepBuilder("customerMyBatisPagingStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(myBatisPagingItemReader())
                .processor(new CustomerItemProcessor())
                .writer(customerFlatFileItemWriter2())
                .build();
    }

    @Bean
    public Job customerMyBatisPagingJob(Step customerMyBatisPagingStep, JobRepository jobRepository) {
        log.info("Initializing customerMyBatisPagingJob...");
        return new JobBuilder(MYBATIS_PAGING_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerMyBatisPagingStep)
                .build();
    }
}
