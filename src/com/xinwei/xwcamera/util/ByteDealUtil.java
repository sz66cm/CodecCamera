package com.xinwei.xwcamera.util;

import java.util.ArrayList;
import java.util.List;

public class ByteDealUtil {
	
	/**
	 * 寻找所有StartCode 目前仅支持 [0x00 0x00 0x00 0x01]类型的起始码
	 * @param data
	 * @param postion
	 * @return
	 */
	public static List findStartCodeOffSet(byte[] data, int postion) {
		List<Integer> result = new ArrayList<Integer>();
		//合法性检验
		if (data.length < 5 || (data.length - postion) < 5) {
			return result;
		}
		//进行检测
		for (int i = postion + 3; i < data.length; i++) {
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
		int type = -1;
		List<Integer> result = findStartCodeOffSet(data, 0);
		if (result.size() > 0) {
			type = data[result.get(0)] & 0x1F;
		}
		return type;
	}
}
