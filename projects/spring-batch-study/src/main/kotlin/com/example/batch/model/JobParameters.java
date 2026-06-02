package com.example.batch.model;

import java.time.LocalDate;

public class JobParameters {
    private String name;
    private Integer age;
    private LocalDate targetDate;
    private Difficulty difficulty;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public String toString() {
        return "JobParameters{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", targetDate=" + targetDate +
                ", difficulty=" + difficulty +
                '}';
    }
}
