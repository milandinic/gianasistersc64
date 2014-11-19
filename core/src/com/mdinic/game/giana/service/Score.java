package com.mdinic.game.giana.service;

public class Score {

    private String name;
    private int score;

    public Score() {
        super();

    }

    public Score(String name, int score) {
        super();
        this.name = name;
        this.score = score;
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

}
