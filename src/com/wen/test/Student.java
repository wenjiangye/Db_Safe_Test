package com.wen.test;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by wen on 2017/3/14.
 */
public class Student implements Serializable
{
    private String name;
    private int age;
    private boolean sex;
    private Map<String, String> cc;
    public Student(@JsonProperty("name")String inname, @JsonProperty("age")int inage, @JsonProperty("sex")boolean insex
    ,@JsonProperty("cc") Map incc)
    {
        name = inname;
        age  = inage;
        sex = insex;
        cc = incc;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String  name) {
        this.name = name;
    }
    public int getAge() {
        return this.age;
    }
    public void setAge(int age) {
        this.age = age;
    }
    public boolean getSex() {
        return this.sex;
    }
    public void setSex(boolean sex) {
        this.sex= sex;
    }
    public Map getCc()
    {
        return this.cc;
    }
    public void setCc(Map incc)
    {
        this.cc = incc;
    }
}
