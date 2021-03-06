package com.xinwei.xwcamera;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import com.xinwei.xwcamera.util.ByteDealUtil;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.SystemClock;
import static com.xinwei.xwcamera.MainActivity.H264;
import static com.xinwei.xwcamera.MainActivity.PREVIEW_SIZE_WIDTH;
import static com.xinwei.xwcamera.MainActivity.PREVIEW_SIZE_HEIGHT;
import static com.xinwei.xwcamera.MainActivity.PREVIEW_BIT_RATE;
import static com.xinwei.xwcamera.MainActivity.PREVIEW_FRAME_RATE;
import static com.xinwei.xwcamera.MainActivity.PREVIEW_I_FRAME_INTERVAL;
import static com.xinwei.xwcamera.MainActivity.ENCODE_TIME_OUT;
import static com.xinwei.xwcamera.util.LogUtil.*;
@SuppressLint("NewApi") 
public class Encoder {
	
	private final String TAG = Encoder.class.getSimpleName();
	
	private final byte[] START_CODE = new byte[] {0x00, 0x00, 0x00, 0x01};
	private int presentationTimeUs = 3600;
	private MediaCodec encoder;
	private MediaFormat mediaFormat;
	private ArrayBlockingQueue<byte[]> frameQueue;
	
	//发送准备成员变量
	private byte[] SPS = null;
	private byte[] PPS = null;
	private boolean hasFirstI = false;
	private boolean canStartOffer = false;

	@SuppressLint("NewApi") 
	public Encoder(ArrayBlockingQueue<byte[]> frameQueue) {
		this.frameQueue = frameQueue;
		mediaFormat = MediaFormat.createVideoFormat(H264, PREVIEW_SIZE_WIDTH, PREVIEW_SIZE_HEIGHT);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, PREVIEW_BIT_RATE);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, PREVIEW_FRAME_RATE);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, PREVIEW_I_FRAME_INTERVAL);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
	}
	
	public void open() throws Exception {
		encoder = MediaCodec.createEncoderByType(H264);
		encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		encoder.start();
	}
	
	public void close() {
		if (encoder != null) {
			encoder.stop();
			encoder.release();
			encoder = null;
		}
	}
	long cctime = 0;
	/**
	 * 同步编码方法
	 * @param dst
	 * @param src
	 * @return
	 */
	public int syncEncode(byte[] src) {
        int len = -1;
        long startTime = SystemClock.elapsedRealtime();
        //喂数据
        int ii = encoder.dequeueInputBuffer(ENCODE_TIME_OUT);
        if(ii >= 0) {
            ByteBuffer inBuffer = encoder.getInputBuffers()[ii];
            inBuffer.clear();
            inBuffer.put(src, 0, src.length);
            encoder.queueInputBuffer(ii, 0, src.length, presentationTimeUs += 3600, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
        }
        //取数据
        BufferInfo info = new BufferInfo();
        int oi = encoder.dequeueOutputBuffer(info, ENCODE_TIME_OUT);
        while (oi == -2) {
            oi = encoder.dequeueOutputBuffer(info, ENCODE_TIME_OUT);
        }
        if (oi >= 0) {
            //取数据
            ByteBuffer outBuffer = encoder.getOutputBuffers()[oi];
            //这个长度是outBuffer的最大容量长度,并非H264数据实际长度.
			//H264数据实际长度应该从MediaCodec.BufferInfo对象的成员变量size去取.
			//如果使用了outBuffer.capacity()的长度,出现编解码端花屏
			//并且十分的卡顿,打印发现H264的实际长度比outBuffer.capacity()小很多
			//这个得十分注意,如下错误代码已注释
            len = outBuffer.capacity();
//          byte[] dst = new byte[len];
//          outBuffer.get(dst, 0, len);
            byte[] dst = new byte[info.size];
            outBuffer.get(dst);
            L("syncEncode() cost time = " + (SystemClock.elapsedRealtime() - startTime) + " ms" 
                    + " info.size = " + info.size + " outBuffer.capacity() = " + len);
            //传输数据
            prepareOffer(dst);
            //释放数据
            encoder.releaseOutputBuffer(oi, false);
        }
        return len;
    }
	/**
	 * 为发送准备
	 * @param data
	 */
	public void prepareOffer(byte[] data) {
		if (canStartOffer) { //可以发送解码器
			//如果是I帧,先插入SPS,PPS
			if (ByteDealUtil.getTypeFromData(data) == 5) {
				frameQueue.offer(SPS);
				frameQueue.offer(PPS);
			}
			frameQueue.offer(data);
		} else {//准备中
			List<Integer> listOffset = ByteDealUtil.findStartCodeOffSet(data, 0);
			int startNum = listOffset.size();
			for (int i = 0; i < startNum; i++) {
				int len = 0;
				//1.SPS
				if ((SPS == null) && ((data[listOffset.get(i)] & 0x1F) == 7)) {
					if ( (i + 1) < startNum) {
						len = listOffset.get(i + 1) - listOffset.get(i) - 4;
					} else {
						len = data.length - listOffset.get(i);
					}
					SPS = new byte[len + START_CODE.length];
					System.arraycopy(START_CODE, 0, SPS, 0, START_CODE.length);
					System.arraycopy(data, listOffset.get(i), SPS, START_CODE.length, len);
					L("prepareOffer() SPS is prepare!");
				}
				//2.PPS
				if ((PPS == null) && ((data[listOffset.get(i)] & 0x1F) == 8)) {
					if ( (i + 1) < startNum) {
						len = listOffset.get(i + 1) - listOffset.get(i) - 4;
					} else {
						len = data.length - listOffset.get(i);
					}
					PPS = new byte[len + START_CODE.length];
					System.arraycopy(START_CODE, 0, PPS, 0, START_CODE.length);
					System.arraycopy(data, listOffset.get(i), PPS, START_CODE.length, len);
					L("prepareOffer() PPS is prepare!");
				}
				//3.找到SPS和PPS后,再第一个I帧
				if ((SPS != null) && (PPS != null) && !hasFirstI && ((data[listOffset.get(i)] & 0x1F) == 5)) {
					hasFirstI = true;
					byte[] spps = new byte[SPS.length + PPS.length];
					System.arraycopy(SPS, 0, spps, 0, SPS.length);
					System.arraycopy(PPS, 0, spps, SPS.length, PPS.length);
					frameQueue.offer(spps);
					frameQueue.offer(data);
					canStartOffer = true;
					L("prepareOffer() offer prepare finish!");
				}
			}
		}
	}
	
	
}
