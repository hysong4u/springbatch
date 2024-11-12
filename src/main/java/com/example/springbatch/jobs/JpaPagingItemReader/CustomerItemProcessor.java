package com.example.springbatch.jobs.JpaPagingItemReader;

import com.example.springbatch.jobs.entity.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public class CustomerItemProcessor implements ItemProcessor<Customer, Customer> {
    @Override
    public Customer process(Customer item) throws Exception {
        log.info("Item Processor ------------------- {}", item);
        return item;
    }
}