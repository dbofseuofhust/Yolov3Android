package com.jacky.finalexam.jni;
import android.graphics.Bitmap;

public class Yolo {
    public native boolean Init(String param, String bin);
    public native float[] Detect(Bitmap bitmap);
    static {
        System.loadLibrary("yolov3_tiny");
    }

}
