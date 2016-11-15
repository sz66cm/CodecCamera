package com.xinwei.xwcamera.util;

public class Rotate {
	
	static {
		System.loadLibrary("rotate_jni");
	}
	
	public native void transNv21ToYUV420SP(byte[] dst, byte[] src, int width, int height);
}
