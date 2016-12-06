package com.xinwei.xwcamera.util;

import java.util.ArrayList;
import java.util.List;

public class ByteDealUtil {
	
	public static final int DEFAULT_LENGTH = -1;
	
	/**
	 * 寻找所有StartCode 目前仅支持 [0x00 0x00 0x00 0x01]类型的起始码
	 * @param data
	 * @param postion
	 * @return
	 */
	public static List findStartCodeOffSet(byte[] data, int postion) {
		return findStartCodeOffSet(data, postion, DEFAULT_LENGTH);
	}
	/**
	 * 寻找所有StartCode 目前仅支持 [0x00 0x00 0x00 0x01]类型的起始码
	 * @param data
	 * @param postion
	 * @return
	 */
	public static List findStartCodeOffSet(byte[] data, int postion, int len) {
		if (len == DEFAULT_LENGTH) {
			len = data.length;
		}
		List<Integer> result = new ArrayList<Integer>();
		//合法性检验
		if (len < 5 || (len - postion) < 5) {
			return result;
		}
		//进行检测
		for (int i = postion + 3; i < len; i++) {
			if (data[i] != 1) {//i 非1
				if (data[i] != 0) {//i 非0
					i += 3;
					continue;
				} else {//i 0
					//FIXME i=0的时候需要优化
					continue;
				}
			} else {//i 1 在这个分支下才可以能有StartCode
				if ((data [i-1] == 0) && (data [i-2] == 0) && (data [i-3] == 0)) {
					result.add(i + 1);
				}
			}
		}
		return result;
	}
	/**
	 * F NRI TPYE
	 * 获取帧的TYPE类型
	 */
	public static int getTypeFromData(byte[] data) {
		return getTypeFromData(data, 0);
	}
	/**
	 * F NRI TPYE
	 * 获取帧的TYPE类型
	 */
	public static int getTypeFromData(byte[] data, int position) {
		return getTypeFromData(data, position, DEFAULT_LENGTH);
	}
	/**
	 * F NRI TPYE
	 * 获取帧的TYPE类型
	 */
	public static int getTypeFromData(byte[] data, int position, int len) {
		if (len == DEFAULT_LENGTH) {
			len = data.length;
		}
		int type = -1;
		List<Integer> result = findStartCodeOffSet(data, position, len);
		if (result.size() > 0) {
			type = data[result.get(0)] & 0x1F;
		}
		return type;
	}
}
