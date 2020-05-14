package com.jacky.finalexam.activity;

import androidx.annotation.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jacky.finalexam.R;
import com.jacky.finalexam.utils.Util;
import com.jacky.finalexam.jni.Yolo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class GalleryActivity extends BaseActivity {

    private static final String TAG = GalleryActivity.class.getName();
    private static final int SELECT_PHOTO = 999;
    private ImageView show;
    private TextView out;
    private boolean isLoad = false;
    private int[] dims = {1, 3, 416, 416};
    private ArrayList<String> resultLabel = new ArrayList<>();
    private com.jacky.finalexam.jni.Yolo Yolo = new Yolo();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
//        CameraView cameraView = findViewById(R.id.camera);
        initView();
        Util.loadLabels(resultLabel, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            initYolo();
        } catch (IOException e) {
            Log.e("MainActivity", "init yolo error");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Yolo = null;
    }

    private void initYolo() throws IOException {

        String paramPath = Util.getPathFromAssets(this, "yolov3-416.param");
        String binPath = Util.getPathFromAssets(this, "yolov3-416.bin");

        isLoad = Yolo.Init(paramPath, binPath);
        Log.d(TAG, "load model success?:" + isLoad);
    }

    private void initView() {
        show = findViewById(R.id.image);
        out = findViewById(R.id.result);
        out.setMovementMethod(ScrollingMovementMethod.getInstance());
        Button use_photo = findViewById(R.id.use_photo);
        use_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isLoad) {
                    Toast.makeText(GalleryActivity.this, "never load model", Toast.LENGTH_SHORT).show();
                    return;
                }
                Util.getImage(GalleryActivity.this, SELECT_PHOTO);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String imagePath;
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SELECT_PHOTO:
                    if (data == null) {
                        Log.w(TAG, "user photo data is null");
                        return;
                    }
                    Uri imageUri = data.getData();
                    imagePath = Util.getUri(GalleryActivity.this, imageUri);
                    if (imagePath == null) {
                        Log.d("test", "path is null");
                    } else {
                        Log.d("test", "everything is ok");
                    }
                    predict(imagePath);
                    break;
            }
        }
    }

    private void predict(String image_path) {
        Bitmap bmp = Util.getScaleBitmap(image_path);
        if (bmp == null) {
            Log.d(TAG, "can not get image");
            return;
        }
        Bitmap rgba = bmp.copy(Bitmap.Config.ARGB_8888, true);
//        Bitmap input_bmp = Bitmap.createScaledBitmap(rgba, dims[2], dims[3], false);
        try {

            long startTime = System.currentTimeMillis();
            float[] result = Yolo.Detect(rgba);
            if (result == null) {
                out.setText(getResources().getString(R.string.predict_result));
                Log.d(TAG, "predict result is null");
                return;
            }
            long endTime = System.currentTimeMillis();
            Log.d(TAG, "result:" + Arrays.toString(result));
            long cost = endTime - startTime;
            Log.d(TAG, "result length: " + result.length);

            StringBuilder resultContent = new StringBuilder();
            resultContent.append("检测结果：").append(Arrays.toString(result));
            for (int i = 0; i < result.length; i += 6) {
                resultContent.append("\n类别：")
                        .append(resultLabel.get((int) result[i])).append("\n概率：")
                        .append(result[i + 1]).append("\n耗时：").append(cost).append("ms").append("\n");
            }
            out.setText(resultContent.toString());
            Canvas canvas = new Canvas(rgba);

            float[][] finalArray = Util.twoArray(result);
            int obj = 0;
            int num = result.length / 6;
            for (obj = 0; obj < num; obj++) {

                Paint paint = new Paint();
                paint.setColor(Color.GREEN);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1);

                canvas.drawRect(finalArray[obj][2] * rgba.getWidth(),
                        finalArray[obj][3] * rgba.getHeight(),
                        finalArray[obj][4] * rgba.getWidth(),
                        finalArray[obj][5] * rgba.getHeight(), paint);

                Paint textbgpaint = new Paint();
                textbgpaint.setColor(Color.RED);
                textbgpaint.setStyle(Paint.Style.FILL);

                Paint textpaint = new Paint();
                textpaint.setColor(Color.WHITE);
                textpaint.setTextSize(16);
                textpaint.setTextAlign(Paint.Align.LEFT);

                String text = resultLabel.get((int) finalArray[obj][0]) + " = " + String.format("%.1f", finalArray[obj][1] * 100) + "%";

                float text_width = textpaint.measureText(text);
                float text_height = - textpaint.ascent() + textpaint.descent();

                float x = finalArray[obj][2]*rgba.getWidth();
                float y = finalArray[obj][3]*rgba.getHeight() - text_height;
                if (y < 0)
                    y = 0;
                if (x + text_width > rgba.getWidth())
                    x = rgba.getWidth() - text_width;

                canvas.drawRect(x, y, x + text_width, y + text_height, textbgpaint);
                canvas.drawText(text, x, y - textpaint.ascent(), textpaint);


            }
            show.setImageBitmap(rgba);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
