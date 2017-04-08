package com.xinwei.xwcamera;

import static com.xinwei.xwcamera.MainActivity.ENCODE_TIME_OUT;
import static com.xinwei.xwcamera.MainActivity.FLAG_DECODER;
import static com.xinwei.xwcamera.MainActivity.H264;
import static com.xinwei.xwcamera.MainActivity.PREVIEW_SIZE_HEIGHT;
import static com.xinwei.xwcamera.MainActivity.PREVIEW_SIZE_WIDTH;
import static com.xinwei.xwcamera.util.LogUtil.L;
import static com.xinwei.xwcamera.util.LogUtil.LM;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import com.xinwei.xwcamera.util.ByteDealUtil;
import com.xinwei.xwcamera.util.FileUtil;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
@SuppressLint("NewApi") 
public class Decoder {
	
	private final String TAG = Decoder.class.getSimpleName();
	
	private final int KEY_FRAME = 5;
	private final int SPS_FRAME = 7;
	private final int PPS_FRAME = 8;
	
	private byte[] SPS = null;
	private byte[] PPS = null;
	
	private MediaCodec decoder;
	private ArrayBlockingQueue<byte[]> frameQueue;
	private Surface surface;
	private MediaFormat decoderMediaFormat;

	private boolean hasConfiged = false;
	private boolean hasFirstI = false;

	private ByteBuffer[] inputBuffers;
	private ByteBuffer[] outputBuffers;
	
	public Decoder(Surface surface, ArrayBlockingQueue<byte[]> frameQueue) {
		LM();
		this.frameQueue = frameQueue;
		this.surface = surface;
		decoderMediaFormat = MediaFormat.createVideoFormat(H264, PREVIEW_SIZE_WIDTH, PREVIEW_SIZE_HEIGHT);
		decoderMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, PREVIEW_SIZE_WIDTH * PREVIEW_SIZE_HEIGHT * 3 / 2);
	}
	
	public void open() throws Exception {
		LM();
		decoder = MediaCodec.createDecoderByType(H264);
		decoder.configure(decoderMediaFormat, surface, null, FLAG_DECODER);
		decoder.start();
		inputBuffers = decoder.getInputBuffers();
		outputBuffers = decoder.getOutputBuffers();
		Log.i(TAG, "open()");
	}
	
	public void close() {
		LM();
		if (decoder != null) {
			decoder.stop();
			decoder.release();
			decoder = null;
			hasConfiged = false;
		}
		Log.i(TAG, "close()");
	}
	/**
	 * 播放前的准备
	 * @param temp
	 * @param mc
	 * @return
	 */
	public boolean preparePlay(byte[] temp, MediaCodec mc) {
		L("preparePlay() Type = " + ByteDealUtil.getTypeFromData(temp));
		if (!hasConfiged) {
			//缓存SPS PPS 
			if(ByteDealUtil.getTypeFromData(temp) == SPS_FRAME ) {
				List<Integer> list = ByteDealUtil.findStartCodeOffSet(temp, 0);
				int spslen = list.get(1) - list.get(0);
				int ppslen = temp.length - list.get(1);
				if (SPS == null) {
					SPS = new byte[spslen];
					System.arraycopy(temp, 0, SPS, 0, spslen);
					L("preparePlay() SPS is prepare!");
				}
				if (PPS == null) {
					PPS = new byte[ppslen];
					System.arraycopy(temp, spslen, PPS, 0, ppslen);
					L("preparePlay() PPS is prepare!");
				}
			}
			boolean result = (SPS != null) && (PPS != null);
			if (result) {
				int ii = mc.dequeueInputBuffer(1000000);
				if (ii >= 0) {
					ByteBuffer inBuffer = mc.getInputBuffers()[ii];
					inBuffer.clear();
					inBuffer.put(SPS, 0, SPS.length);
					inBuffer.put(PPS, 0, PPS.length);
					mc.queueInputBuffer(ii, 0, SPS.length + PPS.length, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
					hasConfiged = true;
				}
			}
		}
		return hasConfiged;
	}
	private byte[] yuv = new byte[PREVIEW_SIZE_WIDTH * PREVIEW_SIZE_HEIGHT * 3 / 2];
	public void decodeAndPlay() throws Exception {
		LM();
		byte[] temp = frameQueue.take();
		if (preparePlay(temp, decoder)) {
			int bufferFlag = 0;
			//喂数据
			int ii = decoder.dequeueInputBuffer(ENCODE_TIME_OUT);
			if (ii >= 0) {
				ByteBuffer inBuffer = inputBuffers[ii];
				inBuffer.clear();
				inBuffer.put(temp, 0, temp.length);
				FileUtil.writefile("/sdcard/h180.h264", temp);
				decoder.queueInputBuffer(ii, 0, temp.length, 0, bufferFlag);
			}
			//取数据并且显示
			BufferInfo info = new BufferInfo();
			int oi = decoder.dequeueOutputBuffer(info, 0);
			if (oi >= 0) {
				ByteBuffer outBuffer = outputBuffers[oi];
				outBuffer.position(info.offset);
				outBuffer.limit(info.offset + info.size);
				outBuffer.get(yuv, 0, info.size);
				FileUtil.writefile("/sdcard/cmyuv1.yuv", yuv, 0, info.size);
				outBuffer.position(info.offset);
				outBuffer.limit(info.offset + info.size);
				decoder.releaseOutputBuffer(oi, true);
			}
		}
		L("decodeAndPlay() end ");
	}

	public ArrayBlockingQueue<byte[]> getFrameQueue() {
		return frameQueue;
	}

	public void setFrameQueue(ArrayBlockingQueue<byte[]> frameQueue) {
		this.frameQueue = frameQueue;
	}

	public Surface getSurface() {
		return surface;
	}

	public void setSurface(Surface surface) {
		this.surface = surface;
	}

	public boolean isHasConfiged() {
		return hasConfiged;
	}

	public void setHasConfiged(boolean hasConfiged) {
		this.hasConfiged = hasConfiged;
	}
	
	
}
