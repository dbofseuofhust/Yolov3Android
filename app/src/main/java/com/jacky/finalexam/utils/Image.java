package com.jacky.finalexam.utils;

public class Image {
    private String title;
    private int imageId;

    public Image(String title, int imageId) {
        this.title = title;
        this.imageId = imageId;
    }

    public String getTitle() {
        return title;
    }

    public int getImageId() {
        return imageId;
    }
}
