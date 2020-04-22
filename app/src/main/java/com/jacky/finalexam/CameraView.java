package com.jacky.finalexam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraView";

    private SurfaceHolder holder;
    private Camera mCamera;
    VideoActivity activity;
    private MyImageView detectView;
    int facing;
    Yolo yolo = null;

    private int imageHeight = 1080;
    private int imageWidth = 1920;

    public CameraView(Context context) {
        super(context);
        init(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        activity = (VideoActivity) context;
    }

    public void setFacing(int facing) {
        this.facing = facing;
    }

    private long startTime, endTime, firstTime;

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startTime = System.currentTimeMillis();
                    firstTime = startTime;
                    Camera.Size size = camera.getParameters().getPreviewSize();
//                    Log.d(TAG, "有调用1");

                    int left = 0;
                    int top = 0;
                    int right = size.width;
                    int bottom = size.height;

                    final YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    image.compressToJpeg(new Rect(left, top, right, bottom), 100, stream);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());


                    if (null == detectView && null != getTag()) {
                        if (getTag() instanceof MyImageView) {
                            detectView = (MyImageView) getTag();
//                            Log.d(TAG, "有调用2");
                        }
                    }
                    activity.predict(Util.rotateToDegrees(bitmap, 90), detectView);

                }catch(Exception e){

                }
            }
        }).start();

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        openCamera(facing);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (null != mCamera)
            initCameraParams(facing);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            release();
            Log.d("release", "release is called");
        } catch (Exception e) {
        }
    }

    public void initCameraParams(int facing) {
        stopPreview();
        Camera.Parameters param = mCamera.getParameters();
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        for (int i = 0; i < sizes.size(); i++) {
            if ((sizes.get(i).width >= imageWidth && sizes.get(i).height >= imageHeight) || i == sizes.size() - 1) {
                imageWidth = sizes.get(i).width;
                imageHeight = sizes.get(i).height;
                break;
            }
        }
//        imageHeight=sizes.get(1).height;
//        imageWidth=sizes.get(1).width;

        param.setPreviewSize(imageWidth, imageHeight);
        param.setPictureSize(imageWidth, imageHeight);
        param.setAntibanding(Camera.Parameters.ANTIBANDING_60HZ);
        param.setExposureCompensation(3);

        //preview frame default
        int frame = 30;
        param.setPreviewFrameRate(frame);

        mCamera.setParameters(param);
        setPreviewOrientation(activity, mCamera, facing);
        startPreview();
    }

    public void startPreview() {
        try {
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mCamera.autoFocus(autoFocus);
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        }
    }

    public void stopPreview() {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }
    }

    Camera.AutoFocusCallback autoFocus = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            postDelayed(doAutoFocus, 2000);
        }
    };
    private Runnable doAutoFocus = new Runnable() {
        @Override
        public void run() {
            if (null != mCamera) {
                try {
                    mCamera.autoFocus(autoFocus);
                } catch (Exception e) {
                }
            }
        }
    };

    public void release() {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
        }
    }

    private void openCamera(int facing) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        switch (facing) {
            case 1:
                for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
                    Camera.getCameraInfo(cameraId, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        try {
                            mCamera = Camera.open(cameraId);
                        } catch (Exception e) {
                            if (null != mCamera) {
                                mCamera.release();
                                mCamera = null;
                            }
                        }
                        break;
                    }
                }
                break;
            case 0:
                for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
                    Camera.getCameraInfo(cameraId, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        try {
                            mCamera = Camera.open(cameraId);
                        } catch (Exception e) {
                            if (null != mCamera) {
                                mCamera.release();
                                mCamera = null;
                            }
                        }
                        break;
                    }
                }
                break;
            default:
                break;
        }
    }

    private void setPreviewOrientation(VideoActivity activity, Camera camera, int facing) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(facing, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int displayDegree;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayDegree = (info.orientation + degrees) % 360;
            displayDegree = (360 - displayDegree) % 360;
        } else {
            displayDegree = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(displayDegree);
    }
}
