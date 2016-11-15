#define LOG_TAG "util_rotate"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#include <jni.h>
#include <stddef.h>
#include <stdlib.h>
#include <android/log.h>
#include <time.h>
#include <stdio.h>

jint transNV21ToNV12(jbyte * ,jbyte * ,jint ,jint );

JNIEXPORT void JNICALL Java_com_xinwei_xwcamera_util_Rotate_transNv21ToYUV420SP
  (JNIEnv * env, jobject thiz, jbyteArray dst, jbyteArray src, jint W, jint H)
{
	clock_t start_t, end_t, total_t;
	start_t = clock();
	jint len = W * H * 3 / 2;
	jbyte * dstp = (*env)->GetByteArrayElements(env, dst, 0);
	jbyte * srcp = (*env)->GetByteArrayElements(env, src, 0);
	transNV21ToNV12(dstp, srcp, W, H);
	(*env)->ReleaseByteArrayElements(env, src, srcp, 0);
	(*env)->ReleaseByteArrayElements(env, dst, dstp, 0);
	end_t = clock();
	total_t = end_t - start_t;
	LOGD("C transNv21ToYUV420SP() cost time : %ld", total_t);
}

jint transNV21ToNV12(jbyte * outputBuffer, jbyte * inputBuffer, jint width, jint height)
{
	jint frameSize = width * height;
	jint qFrameSize = frameSize / 2;
	jint i = 0;
	//deal Y
	memcpy(outputBuffer, inputBuffer, frameSize);
	//deal UV
	for (i = 0; i + 1 < qFrameSize; i += 2) {
		outputBuffer[frameSize + i] = inputBuffer[frameSize + i + 1];
		outputBuffer[frameSize + i + 1] = inputBuffer[frameSize + i];
	}
	return frameSize;
}

