/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jacky.finalexam.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.jacky.finalexam.jni.MobilenetSSDNcnn;
import com.jacky.finalexam.App;
import com.jacky.finalexam.utils.Util;
import com.jacky.finalexam.jni.MobilenetSSDNcnn.Obj;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraView";

    private SurfaceHolder mHolder;
    private Canvas mCanvas;
    private Paint paint;
    private Bitmap mBitmap;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;

    private boolean isLoad = false;
//    modify by dongb, for yolov3
//    private int[] dims = {1, 3, 416, 416};
    private int[] dims = {1, 3, 300, 300};
    private ArrayList<String> resultLabel = new ArrayList<>();
    private com.jacky.finalexam.jni.MobilenetSSDNcnn MobilenetSSDNcnn = new MobilenetSSDNcnn();

    private float mWidth = 0;
    private float mHeight = 0;

    private void initMobilenetSSDNcnn() throws IOException {

        isLoad = MobilenetSSDNcnn.Init(App.getContext().getAssets());
        Log.d(TAG, "load model success?:" + isLoad);
    }

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Log.i(TAG, "AutoFitSurfaceView struct");
        initParams(context);
        try {
            initMobilenetSSDNcnn();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Util.loadLabels(resultLabel, App.getContext());
    }

    private void initParams(Context context) {
        mHolder = getHolder();
        mHolder.addCallback(this);
        paint = new Paint();
        paint.setColor(Color.WHITE);
        setFocusable(true);
        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated ");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged " + width + "X" + height);
        mWidth = width;
        mHeight = height;

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed ");
        MobilenetSSDNcnn = null;
    }

    private void drawRect(Rect rect, int color, String string) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setTextSize(24f);
        paint.setStrokeWidth(3.6f);

        mCanvas.drawRect(rect, paint);
        paint.setStyle(Paint.Style.FILL);
        mCanvas.drawText(string, rect.left + 4, rect.top + 14, paint);
    }

    public void drawDetect(Obj[] data, int width, int height, int rolatedeg) {
        for (int i = 0; i < data.length; i++) {

            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);

//            modify by dongb
//            mCanvas.drawRect(data[i].x, data[i].y, data[i].x + data[i].w, data[i].y + data[i].h, paint);
            mCanvas.drawRect(data[i].x*width, data[i].y*height, data[i].xe*width, data[i].ye*height, paint);

//            modify by dongb
//            Log.d(TAG, "onImageAvailable----------------" + width + "X" + height + "X" + data[i].x+"X" + data[i].y+"X" + data[i].w+"X" + data[i].h);

            String text = data[i].label + " = " + String.format("%.1f", data[i].prob * 100) + "%";

            Paint textbgpaint = new Paint();
            textbgpaint.setColor(Color.RED);
            textbgpaint.setStyle(Paint.Style.FILL);

            Paint textpaint = new Paint();
            textpaint.setColor(Color.WHITE);
            textpaint.setTextSize(24);
            textpaint.setTextAlign(Paint.Align.LEFT);

            float text_width = textpaint.measureText(text);
            float text_height = - textpaint.ascent() + textpaint.descent();

//            modify by dongb
//            float x = data[i].x;
//            float y = data[i].y - text_height;
            float x = data[i].x*width;
            float y = data[i].y*height - text_height;
            if (y < 0)
                y = 0;
            if (x + text_width > width)
                x = width - text_width;

            mCanvas.drawRect(x, y, x + text_width, y + text_height, textbgpaint);
            mCanvas.drawText(text, x, y - textpaint.ascent(), textpaint);

        }

    }

    private Obj[] result;
    private boolean isRunning;

    @SuppressLint("WrongThread")
    public void draw(byte[] data, int width, int height, int rolatedeg) {
        long startTime, endTime;
        Log.d(TAG, "draw " + data.length + " " + width + "X" + height);
        if (data != null) {
            startTime = System.currentTimeMillis();
            Log.d("test", "-------------start----------------");

//            final YuvImage image = new YuvImage(data, ImageFormat.NV21, width, height, null);
//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            image.compressToJpeg(new Rect(0, 0, width, height), 100, stream);

            mBitmap = yuvToBitmap(data, width, height);

//            mBitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
//            Bitmap rgbout = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
            endTime = System.currentTimeMillis();
            Log.d("test", "yuv to bitmap time cost： " + (endTime - startTime) + " ms");
            Log.d("test", "----------------end------------------");

            final Bitmap inputBitmap = Bitmap.createScaledBitmap(mBitmap, dims[2], dims[3], false);
            if (mBitmap == null) {
                Log.d(TAG, "data data is to bitmap error");
                return;
            }

//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            mBitmap.compress(Bitmap.CompressFormat.JPEG, 10, stream);
//            mBitmap = null;
//            mBitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

//            将holder持有的surface界面作为画布，在上面绘图
            try {
                mCanvas = mHolder.lockCanvas();
                mWidth = mCanvas.getWidth();
                mHeight = mCanvas.getHeight();
                Log.d(TAG, "canvas size is " + mWidth + " " + mHeight);
                float scaleWidth = mWidth / width;
                float scaleHeight = mHeight / height;
                Log.d(TAG, "scale size is " + scaleWidth + " " + scaleHeight);


                Matrix matrix = new Matrix();
                matrix.setScale(scaleWidth, scaleHeight);
                Log.d("bitmap length", mBitmap.getByteCount() + "");
                mCanvas.drawBitmap(mBitmap, matrix, paint);
                Log.d("bitmap length", inputBitmap.getByteCount() + "");


                if (!isRunning) {
                    isRunning = true;
                    new Thread(new Runnable() {
                        public void run() {
                            result = MobilenetSSDNcnn.Detect(inputBitmap,false);
                            Log.d(TAG, "result: " + Arrays.toString(result));
                            isRunning = false;
                        }
                    }).start();
                }
                if (null != result) {
                    drawDetect(result, (int) mWidth, (int) mHeight, rolatedeg);
                }
                mBitmap.recycle();
            } catch (Exception e) {
                Log.d(TAG, "e=" + e);
                mHolder.unlockCanvasAndPost(mCanvas);
                return;
            }
            mHolder.unlockCanvasAndPost(mCanvas);
        } else {
            Log.d(TAG, "data data is null");
        }
    }

    public Bitmap yuvToBitmap(byte[] yuv, int width, int height) {

        if (yuvType == null) {
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(yuv.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);//分配内存
            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        }
        in.copyFrom(yuv);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        Bitmap rgba = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(rgba);
        return rgba;
    }
}
