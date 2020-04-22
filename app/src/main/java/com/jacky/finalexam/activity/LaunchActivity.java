package com.jacky.finalexam.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.jacky.finalexam.R;
import com.jacky.finalexam.adapter.CardAdapter;
import com.jacky.finalexam.utils.Image;
import com.jacky.finalexam.view.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LaunchActivity extends AppCompatActivity {

    private String[] titles = {
            "图片识别",
            "实时识别",
    };

    private Class[] activities = {
            GalleryActivity.class,
            DetectActivity.class,
    };

    private List<Image> imageList = new ArrayList<>();

    private RecyclerView recyclerView;
    private Card card;
    private CardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_launch);
        permissionRequest();

        recyclerView = findViewById(R.id.recycler);
        initImages();
        initView();
    }

    private void initView(){
        card = new Card(this);
        adapter = new CardAdapter(imageList, new CardAdapter.OnItemClickListener() {
            @Override
            public void onclick(View view, final int position) {
                int focusPosition = card.findShouldSelectPosition();
                if (focusPosition == position) {
                    startActivity(new Intent(LaunchActivity.this, activities[position]));
                } else {
                    card.smoothScrollToPosition(position, new Card.onMimoListener() {
                        @Override
                        public void onFocusAnimEnd() {
                            startActivity(new Intent(LaunchActivity.this, activities[position]));
                        }
                    });
                }
            }
        });

        recyclerView.setLayoutManager(card);
        recyclerView.setAdapter(adapter);

    }

    private Image[] images = {new Image(titles[0], R.drawable.gallery), new Image(titles[1], R.drawable.video)};

    private void initImages() {
        imageList.clear();
        imageList.addAll(Arrays.asList(images).subList(0, titles.length));
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
