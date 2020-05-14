#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>

#include <sys/time.h>
#include <unistd.h>

#include <stdio.h>
#include <algorithm>
#include <fstream>

#include "platform.h"
#include "net.h"

#if NCNN_VULKAN
#include "gpu.h"
#endif // NCNN_VULKAN

extern "C" {

static ncnn::Net yolov3;

//struct Object
//{
//  cv::Rect_<float> rect;
//  int label;
//  float prob;
//};

JNIEXPORT jboolean JNICALL
Java_com_jacky_finalexam_jni_Yolo_Init(JNIEnv *env, jobject obj, jstring param, jstring bin) {
    __android_log_print(ANDROID_LOG_DEBUG, "yolov3", "jni start");

#if NCNN_VULKAN
    yolov3.opt.use_vulkan_compute = true;
#endif // NCNN_VULKAN

    const char *param_path = env->GetStringUTFChars(param, NULL);
    if (param_path == NULL)
        return JNI_FALSE;
    __android_log_print(ANDROID_LOG_DEBUG, "yolov3", "load_param %s", param_path);

    int ret = yolov3.load_param(param_path);
    __android_log_print(ANDROID_LOG_DEBUG, "yolov3", "load_param result %d", ret);
    env->ReleaseStringUTFChars(param, param_path);

    const char *bin_path = env->GetStringUTFChars(bin, NULL);
    if (bin_path == NULL)
        return JNI_FALSE;
    __android_log_print(ANDROID_LOG_DEBUG, "yolov3", "load_model %s", bin_path);

    int ret2 = yolov3.load_model(bin_path);
    __android_log_print(ANDROID_LOG_DEBUG, "yolov3", "load_model result %d", ret2);
    env->ReleaseStringUTFChars(bin, bin_path);
    return JNI_TRUE;
}

JNIEXPORT jfloatArray JNICALL
Java_com_jacky_finalexam_jni_Yolo_Detect(JNIEnv *env, jobject thiz, jobject bitmap) {
    const int target_size = 416; //input size

    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    int width = info.width;
    int height = info.height;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
        return NULL;

    void *indata;
    AndroidBitmap_lockPixels(env, bitmap, &indata);

    ncnn::Mat in = ncnn::Mat::from_pixels_resize((const unsigned char *) indata,
                                                 ncnn::Mat::PIXEL_RGBA2RGB, width, height,
                                                 target_size, target_size);

    __android_log_print(ANDROID_LOG_DEBUG, "yolov3", "data input, in.w: %d; in.h: %d", in.w, in.h);
    AndroidBitmap_unlockPixels(env, bitmap);

    const float norm_vals[3] = {1 / 255.0, 1 / 255.0, 1 / 255.0};
    in.substract_mean_normalize(0, norm_vals);
    ncnn::Extractor ex = yolov3.create_extractor();
    ex.input("data", in);
    ex.set_light_mode(false);
    ex.set_num_threads(4);

    ncnn::Mat out;
    int result = ex.extract("yolo_106", out); // yolo_23 is the out_blob name in param file for yolov3 tiny and yolo_106 for yolov3
    __android_log_print(ANDROID_LOG_DEBUG, "yolov3", "extract stop %d", result);
    if (result != 0)
        return NULL;
    int output_wsize = out.w;
    int output_hsize = out.h;
    __android_log_print(ANDROID_LOG_DEBUG, "yolov3", "width %d", output_wsize);
    __android_log_print(ANDROID_LOG_DEBUG, "yolov3", "height %d", output_hsize);

    jfloat *output[output_wsize * output_hsize];
    for (int i = 0; i < out.h; i++) {
        for (int j = 0; j < out.w; j++) {
            output[i * output_wsize + j] = &out.row(i)[j];
        }
    }
    jfloatArray jOutputData = env->NewFloatArray(output_wsize * output_hsize);
    if (jOutputData == nullptr) return nullptr;
    env->SetFloatArrayRegion(jOutputData, 0, output_wsize * output_hsize,
                             reinterpret_cast<const jfloat *>(*output));  // copy
    __android_log_print(ANDROID_LOG_DEBUG, "yolov3", "output array %p", jOutputData);
    return jOutputData;

}
}
