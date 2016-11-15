package com.xinwei.xwcamera;

import static com.xinwei.xwcamera.MainActivity.ENCODE_TIME_OUT;
import static com.xinwei.xwcamera.MainActivity.FLAG_DECODER;
import static com.xinwei.xwcamera.MainActivity.H264;
import static com.xinwei.xwcamera.MainActivity.PREVIEW_SIZE_HEIGHT;
import static com.xinwei.xwcamera.MainActivity.PREVIEW_SIZE_WIDTH;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import com.xinwei.xwcamera.util.ByteDealUtil;

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
	
	public Decoder(Surface surface, ArrayBlockingQueue<byte[]> frameQueue) {
		this.frameQueue = frameQueue;
		this.surface = surface;
		decoderMediaFormat = MediaFormat.createVideoFormat(H264, PREVIEW_SIZE_WIDTH, PREVIEW_SIZE_HEIGHT);
		decoderMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, PREVIEW_SIZE_WIDTH * PREVIEW_SIZE_HEIGHT * 3 / 2);
	}
	
	public void open() throws Exception {
		decoder = MediaCodec.createDecoderByType(H264);
		decoder.configure(decoderMediaFormat, surface, null, FLAG_DECODER);
		decoder.start();
		Log.i(TAG, "open()");
	}
	
	public void close() {
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
		Log.i(TAG, "preparePlay() Type = " + ByteDealUtil.getTypeFromData(temp));
		if (!hasConfiged) {
			//缓存SPS PPS 
			if(ByteDealUtil.getTypeFromData(temp) == SPS_FRAME ) {
				List<Integer> list = ByteDealUtil.findStartCodeOffSet(temp, 0);
				int spslen = list.get(1) - list.get(0);
				int ppslen = temp.length - list.get(1);
				if (SPS == null) {
					SPS = new byte[spslen];
					System.arraycopy(temp, 0, SPS, 0, spslen);
					Log.i(TAG, "preparePlay() SPS is prepare!");
				}
				if (PPS == null) {
					PPS = new byte[ppslen];
					System.arraycopy(temp, spslen, PPS, 0, ppslen);
					Log.i(TAG, "preparePlay() PPS is prepare!");
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
	
	public void decodeAndPlay() throws Exception {
		Log.i(TAG, "decodeAndPlay() start ");
		byte[] temp = frameQueue.take();
		if (preparePlay(temp, decoder)) {
			int bufferFlag = 0;
			//喂数据
			int ii = decoder.dequeueInputBuffer(ENCODE_TIME_OUT);
			if (ii >= 0) {
				ByteBuffer inBuffer = decoder.getInputBuffers()[ii];
				inBuffer.clear();
				inBuffer.put(temp, 0, temp.length);
				decoder.queueInputBuffer(ii, 0, temp.length, 0, bufferFlag);
			}
			//取数据并且显示
			BufferInfo info = new BufferInfo();
			int oi = decoder.dequeueOutputBuffer(info, 0);
			while (oi == -2) {
				oi = decoder.dequeueOutputBuffer(info, 0);
			}
			if (oi >= 0) {
				decoder.releaseOutputBuffer(oi, true);
			}
		}
		Log.i(TAG, "decodeAndPlay() end ");
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
