package com.jacky.finalexam.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Util {

    private static final String TAG = "Util";
    // get picture in photo
    public static void getImage(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    public static String getUri(Context context, Uri uri) {
        String result;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    public static Bitmap getScaleBitmap(String filePath) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, opt);

        int bmpWidth = opt.outWidth;
        int bmpHeight = opt.outHeight;

        int maxSize = 500;

        opt.inSampleSize = 1;
        while (true) {
            if (bmpWidth / opt.inSampleSize < maxSize || bmpHeight / opt.inSampleSize < maxSize) {
                break;
            }
            opt.inSampleSize *= 2;
        }
        opt.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, opt);
    }

    public static String getPathFromAssets(Context context, String assetsFileName) {
        File f = new File(context.getCacheDir() + "/" + assetsFileName);
        try {
            InputStream is = context.getAssets().open(assetsFileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return f.getPath();
    }

    public static float[][] twoArray(float[] input) {
        int l = input.length;
        int n = input.length / 6;
        float[][] out = new float[n][6];
        int k = 0;
        for (int i = 0; i < n; i++) {
            int j = 0;
            while (j < 6) {
                out[i][j] = input[k];
                k++;
                j++;
            }
        }
        return out;
    }

    public static void loadLabels(ArrayList<String> list, Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open("ori_coco.names")));
            String readLine = null;
            while ((readLine = reader.readLine()) != null) {
                list.add(readLine);
            }
            reader.close();
        } catch (Exception e) {
            Log.e("labelCache", "error " + e);
        }
    }

    public static Bitmap rotateToDegrees(Bitmap tmpBitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degrees);
        return Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight(), matrix,
                true);
    }

    public static Bitmap compressImage(Bitmap image, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        image.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        int options = quality;
        while ( baos.toByteArray().length / 1024>100) { //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 1000;//每次都减少10
        }
        //把压缩后的数据baos存放到ByteArrayInputStream中
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null); //把ByteArrayInputStream数据生成图片
        return bitmap;
    }

}

