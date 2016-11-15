LOCAL_PATH := $(call my-dir)
######简历旋转库

include $(CLEAR_VARS)

LOCAL_MODULE := rotate_jni
LOCAL_SRC_FILES := ./rotate/util_rotate.c
LOCAL_LDLIBS += -llog
LOCAL_ARM_MODE := arm
LOCAL_CFLAGS = -O3 

include $(BUILD_SHARED_LIBRARY)
