package com.jacky.finalexam;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final int SELECT_PHOTO = 999;
    private ImageView show;
    private TextView out;
    private boolean isLoad = false;
    private int[] dims = {1, 3, 416, 416};
    private ArrayList<String> resultLabel = new ArrayList<>();
    private Yolo Yolo = new Yolo();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        CameraView cameraView = findViewById(R.id.camera);
        try {
            initYolo();
        } catch (IOException e) {
            Log.e("MainActivity", "init yolo error");
        }
        initView();
        Util.loadLabels(resultLabel, this);
    }

    private void initYolo() throws IOException {

        String paramPath = Util.getPathFromAssets(this, "yolov3-tiny.param");
        String binPath = Util.getPathFromAssets(this, "yolov3-tiny.bin");

        isLoad = Yolo.Init(paramPath, binPath);
        Log.d(TAG, "load model success?:" + isLoad);
    }

    private void initView() {
        permissionRequest();
        show = findViewById(R.id.image);
        out = findViewById(R.id.result);
        out.setMovementMethod(ScrollingMovementMethod.getInstance());
        Button use_photo = findViewById(R.id.use_photo);
        use_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isLoad) {
                    Toast.makeText(MainActivity.this, "never load model", Toast.LENGTH_SHORT).show();
                    return;
                }
                Util.getImage(MainActivity.this, SELECT_PHOTO);
            }
        });

        findViewById(R.id.video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VideoActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.detect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DetectActivity.class);
                startActivity(intent);
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
                    imagePath = Util.getUri(MainActivity.this, imageUri);
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
        Bitmap input_bmp = Bitmap.createScaledBitmap(rgba, dims[2], dims[3], false);
        try {

            long startTime = System.currentTimeMillis();
            float[] result = Yolo.Detect(input_bmp);
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
            resultContent.append("result：").append(Arrays.toString(result));
            for (int i = 0; i < result.length; i += 6) {
                resultContent.append("\nname：")
                        .append(resultLabel.get((int) result[i])).append("\nprobability：")
                        .append(result[i]).append("\ntime：").append(cost).append("ms").append("\n");
            }
            out.setText(resultContent.toString());
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
            show.setImageBitmap(rgba);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void permissionRequest() {
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        int grantResult = grantResults[i];
                        if (grantResult == PackageManager.PERMISSION_DENIED) {
                            String s = permissions[i];
                            Toast.makeText(this, s + "permission was denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }
}
