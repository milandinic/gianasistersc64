package com.mdinic.game.giana.service;

import java.util.Date;

public class Score {

    private String name;
    private int score;
    private int level;
    private final Date date;

    public Score() {
        super();
        date = new Date();
    }

    public Score(String name, int score, int level) {
        this();
        this.name = name;
        this.score = score;
        this.level = level;
    }

    public Date getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

}
