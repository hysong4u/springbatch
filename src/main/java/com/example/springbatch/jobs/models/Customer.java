package com.example.springbatch.jobs.models;

import lombok.Data;


@Data
public class Customer {
    private Long id;
    private String name;
    private int age;
    private String gender;

}