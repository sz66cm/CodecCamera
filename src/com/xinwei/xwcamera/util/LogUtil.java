package com.xinwei.xwcamera.util;

import android.util.Log;

public class LogUtil {
	
	public static boolean DEBUG = true;
	public static String TAG = "XWCamera";

	/**
	 * 打印方法名
	 */
	public static void LM() {
		if (DEBUG) {
			String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
			String className = Thread.currentThread().getStackTrace()[3].getClassName();
			Log.i(TAG, String.format("%s - %s() ", className, methodName));
		}
	}
	
	/**
	 * 打印
	 * @param msg
	 */
	public static void L(String msg) {
		if (DEBUG) {
			StackTraceElement env = new Throwable().getStackTrace()[1];
			String methodName = env.getMethodName();
			String className = env.getClassName();
			int line = env.getLineNumber();
			Log.d(TAG, String.format("%s - %s() , %s --line:%d", className, methodName, msg, line));
		}
	}
}
