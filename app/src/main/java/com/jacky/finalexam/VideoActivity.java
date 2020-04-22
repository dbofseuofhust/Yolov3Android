package com.jacky.finalexam;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class VideoActivity extends AppCompatActivity {

    private static final String TAG = "VideoActivity";

    private boolean isLoad = false;
    private int[] dims = {1, 3, 416, 416};
    private ArrayList<String> resultLabel = new ArrayList<>();
    private Yolo Yolo = new Yolo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        try {
            initYolo();
        } catch (IOException e) {
            Log.e("MainActivity", "init yolo error");
        }
        CameraView cameraView = findViewById(R.id.camera);
        MyImageView imageView = findViewById(R.id.my_view);
        cameraView.setTag(imageView);
        Util.loadLabels(resultLabel, this);
    }

    private void initYolo() throws IOException {

        String paramPath = Util.getPathFromAssets(this, "yolov3-tiny.param");
        String binPath = Util.getPathFromAssets(this, "yolov3-tiny.bin");

        isLoad = Yolo.Init(paramPath, binPath);
        Log.d(TAG, "load model success?:" + isLoad);
    }

    void predict(Bitmap bmp, ImageView imageView) {
//        Bitmap bmp = Util.getScaleBitmap(image_path);
//        Log.d(TAG, "有调用");
        if (bmp == null) {
            Log.d(TAG, "can not get image");
            return;
        }
        Bitmap rgba = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap input_bmp = Bitmap.createScaledBitmap(rgba, dims[2], dims[3], false);
        try {

            long startTime = System.currentTimeMillis();
            float[] result = Yolo.Detect(input_bmp);
            if (result.length == 0) {
                Log.d(TAG, "predict result is null");
                return;
//                out.setText(getResources().getString(R.string.predict_result));
            }
            long endTime = System.currentTimeMillis();
            Log.d(TAG, "result:" + Arrays.toString(result));
            long cost = endTime - startTime;
            Log.d(TAG, "result length: " + result.length);

            StringBuilder resultContent = new StringBuilder();
            resultContent.append("result：").append(Arrays.toString(result));
            for (int i = 0; i < result.length; i += 6) {
                resultContent.append("\nname：")
                        .append(resultLabel.get((int) result[i])).append("\nprobability：")
                        .append(result[i]).append("\ntime：").append(cost).append("ms").append("\n");
            }
//            out.setText(resultContent.toString());
            Canvas canvas = new Canvas(rgba);
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);

            float[][] finalArray = Util.twoArray(result);
            int obj = 0;
            int num = result.length / 6;
            for (obj = 0; obj < num; obj++) {
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1);
                canvas.drawRect(finalArray[obj][2] * rgba.getWidth(),
                        finalArray[obj][3] * rgba.getHeight(),
                        finalArray[obj][4] * rgba.getWidth(),
                        finalArray[obj][5] * rgba.getHeight(), paint);

                paint.setColor(Color.YELLOW);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1);
                canvas.drawText(resultLabel.get((int) finalArray[obj][0]),
                        finalArray[obj][2] * rgba.getWidth(), finalArray[obj][3] * rgba.getHeight(), paint);
            }
//            show.setImageBitmap(rgba);
            if (null != imageView) {
                showImage(rgba, (MyImageView) imageView);
//                rgba.recycle();
            } else {
                rgba.recycle();
            }
//            rgba.recycle();
            input_bmp.recycle();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showImage(final Bitmap bmp, final MyImageView imageView) {
        //将裁切的图片显示出来（测试用，需要为CameraView  setTag（ImageView））
        MainThread.getInstance().
                execute(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageBitmap(bmp);
                    }
                });
    }


}
