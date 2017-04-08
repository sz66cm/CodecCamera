package com.xinwei.xwcamera.util;

import java.io.IOException;
import java.io.RandomAccessFile;

public class FileUtil {
	/**
	 * 写文件
	 * @param fileName
	 * @param content
	 */
	public static void writefile(String fileName, byte[] content) {  
        writefile(fileName, content, 0, content.length);
    }
	/**
	 * 写文件
	 * @param fileName
	 * @param content
	 */
	public static void writefile(String fileName, byte[] content, int offset, int length) {  
		try {  
			// 打开一个随机访问文件流，按读写方式  
			RandomAccessFile randomFile = new RandomAccessFile(fileName, "rw");  
			// 文件长度，字节数  
			long fileLength = randomFile.length();  
			// 将写文件指针移到文件尾。  
			randomFile.seek(fileLength);  
			randomFile.write(content, offset, length);
			randomFile.close();  
		} catch (IOException e) {  
			e.printStackTrace();  
		}  
	}
}
