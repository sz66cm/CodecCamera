package com.xinwei.cm.pool;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.Semaphore;

import android.os.SystemClock;
import android.util.Log;

public abstract class ObjectPool<K> {
	
	private final String TAG = ObjectPool.class.getSimpleName();
	
	/**
	 * 默认空闲超时时间(单位:ms)
	 */
	public final static long DEFAULT_TIME_OUT_IDLE = 1000;
	/**
	 * 默认使用延后复用时间(单位:ms)
	 */
	public final static long DEFAULT_TIME_DELAY_RUNNING = 1000; 
	/**
	 * 可以使用的对象
	 */
	private Hashtable<K, Long> canUse = new Hashtable<K, Long>();
	/**
	 * 正在使用的对象
	 */
	private Hashtable<K, Long> inUsing = new Hashtable<K, Long>();
	
	/**
	 * 控制read等待write值
	 */
	private Semaphore semaphore = new Semaphore(0);
	
	/**
	 * 写入操作
	 * @return
	 */
	public K write() {
		long currentTime = SystemClock.elapsedRealtime();
		K k = null;
		if(canUse.isEmpty()) {
			k = create();
		} else {
			Enumeration<K> keys = canUse.keys();
			if (keys.hasMoreElements()) {
				k = keys.nextElement();
				canUse.remove(k);
			}
		}
		inUsing.put(k, currentTime);
		semaphore.release();
		return k;
	}
	
	/**
	 * 读取操作
	 * @return
	 */
	public K read() {
		K k = null;
		try {
			semaphore.acquire();
			Enumeration<K> keys = inUsing.keys();
			if (keys.hasMoreElements()) {
				k = keys.nextElement();
				inUsing.remove(k);
			}
		} catch (InterruptedException e) {
			Log.e(TAG, "read() e = " + e);
		}
		return k;
	}
	
	/**
	 * 重用
	 * @param k
	 * @return
	 */
	public void reUse(K k) {
		long currentTime = SystemClock.elapsedRealtime();
		canUse.put(k, currentTime);
	}
	
	/**
	 * 生成对象,需要用户实现
	 * @return
	 */
	public abstract K create();
	
	/**
	 * 正在使用对象个数
	 * @return
	 */
	public int getInUseObjectNum() {
		return inUsing.size();
	}
	
	/**
	 * 空闲对象个数
	 * @return
	 */
	public int getCanUseObjectNum() {
		return inUsing.size();
	}
	
}
