package com.example.myapplication.pojo;

import lombok.Data;

@Data
public class UserInfo {

    private int id;
    private String name;
    private String email;
    private String pass_word;
    private String create_time;

}