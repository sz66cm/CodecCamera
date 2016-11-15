package com.xinwei.xwcamera;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.xinwei.xwcamera.util.Rotate;


public class MainActivity extends Activity implements SurfaceHolder.Callback,  android.hardware.Camera.PreviewCallback

{

	public static final String TAG = "MainActivity";
	public static final String H264 = "video/avc";
	
	public static final int PREVIEW_SIZE_WIDTH = 352;
	public static final int PREVIEW_SIZE_HEIGHT = 288;
	public static final int PREVIEW_BIT_RATE = 50000;
	public static final int PREVIEW_FRAME_RATE = 15;
	public static final int PREVIEW_I_FRAME_INTERVAL = 1;
	
	public static final int ENCODE_TIME_OUT = 0;
	
	public static final int WHAT_DEAL_DATA = 0x01;
	public static final int FLAG_DECODER = 0;
	
	public static int presentationTimeUs = 0;
	
	
    private SurfaceView svp;
	private SurfaceView svds;
	private Camera camera;

	private SurfaceHolder svpHolder;
	private SurfaceHolder svdsHolder;
	private MediaCodec encoder;
	private MediaFormat mediaFormat;
	private MediaCodec decoder;
	private MediaFormat decoderMediaFormat;
	private boolean needPlay;
	private boolean isEncoding = false;
	private boolean isRuning = true;
	
	private byte[] outDatas = new byte[PREVIEW_SIZE_WIDTH * PREVIEW_SIZE_HEIGHT * 3 / 2];
	
	private Handler workHandler;
	private Handler decoderPlayHandler;
	
	private ArrayBlockingQueue<byte[]> frameQueue = new ArrayBlockingQueue<byte[]>(30);
	private TextView tv1;
	private WakeLock wakeLock;
	private Rotate yuvDealUtil;
	private boolean isFirstI = false;
	private Encoder cmEncoder;
	private Decoder cmDecoder;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initCodec();
        initView();
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        wakeLock.acquire();
        yuvDealUtil = new Rotate();
    }
	
	private void initCodec() {
		cmEncoder = new Encoder(frameQueue);
		cmDecoder = new Decoder(null, frameQueue);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		closeCamera();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		wakeLock.release();
		if(cmDecoder != null) {
			cmDecoder.close();
		}
		if(cmEncoder != null) {
			cmEncoder.close();
		}
		closeCamera();
	}

	private void initView() {
		
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int displayHeight = dm.heightPixels;
				
		tv1 = (TextView)findViewById(R.id.tv);
		svp = (SurfaceView)findViewById(R.id.sv1);
		svds = (SurfaceView)findViewById(R.id.sv2);
		svpHolder = svp.getHolder();
		svdsHolder = svds.getHolder();
		svpHolder.addCallback(this);
		svdsHolder.addCallback(svdsCallback);
		
		int svpHeight =( displayHeight - (2 * tv1.getLayoutParams().height) ) / 2;
		int svpWidth = ( svpHeight * PREVIEW_SIZE_WIDTH ) / PREVIEW_SIZE_HEIGHT;
		
		LayoutParams svplp = svp.getLayoutParams();
		LayoutParams svdslp = svds.getLayoutParams();
		svplp.height = svpHeight;
		svdslp.height = svpHeight;
		svplp.width = svpWidth;
		svdslp.width = svpWidth;
		
		svp.setLayoutParams(svplp);
		svds.setLayoutParams(svdslp);
	}
	
	private void openCamera(int cid) throws Exception {
		isRuning = true;
		camera = Camera.open(cid);
		Parameters p = camera.getParameters();
		p.setPreviewSize(PREVIEW_SIZE_WIDTH, PREVIEW_SIZE_HEIGHT);
		p.setPreviewFormat(ImageFormat.NV21);
		camera.setParameters(p);
		camera.setPreviewDisplay(svpHolder);
		camera.setPreviewCallback(MainActivity.this);
		camera.addCallbackBuffer(new byte[PREVIEW_SIZE_WIDTH * PREVIEW_SIZE_HEIGHT * 3 / 2]);
	}
	
	private void closeCamera() {
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
		isRuning = false;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			openCamera(0);
			cmEncoder.open();
			camera.startPreview();
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "surfaceCreated() Exception e = "+e);
		}
		Log.i(TAG, "surfaceCreated() svpHolder = " + svdsHolder + " holder = " + holder);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		cmEncoder.close();
		closeCamera();
	}
	private boolean resetDecoder = false;
	
	private SurfaceHolder.Callback svdsCallback = new SurfaceHolder.Callback() {
		
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			svdsHolder = holder;
			if (cmDecoder != null) {
				cmDecoder.setSurface(svdsHolder.getSurface());
			}
			try {
				cmDecoder.open();
			} catch (Exception e) {
				e.printStackTrace();
				Log.w(TAG, "surfaceCreated() cmDecoder open() -> e = " + e);
			}
			new Thread(){

				public void run() {
					while (isRuning) {
						try {
//							if (resetDecoder) {
//								cmDecoder.close();
//								cmDecoder.setSurface(svdsHolder.getSurface());
//								cmDecoder.open();
//								resetDecoder = false;
//							} else {
								cmDecoder.decodeAndPlay();
//							}
						} catch (Exception e) {
							e.printStackTrace();
							Log.d(TAG, "run() cmDecoder decodeAndPlay() -> e = " + e);
//							resetDecoder = true;
						}
					}
				};
			}.start();
		}
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			cmDecoder.close();
		}
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			cmDecoder.close();
			try {
				cmDecoder.open();
			} catch (Exception e) {
				e.printStackTrace();
				Log.w(TAG, "surfaceCreated() cmDecoder open() -> e = " + e);
			}
		}
	};
	
	@SuppressLint("NewApi") @Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		byte[] temp = new byte[data.length];
		yuvDealUtil.transNv21ToYUV420SP(temp, data, PREVIEW_SIZE_WIDTH, PREVIEW_SIZE_HEIGHT);
		cmEncoder.syncEncode(temp);
		//设置回调数据大小
		camera.addCallbackBuffer(data);
	}


	@SuppressLint("NewApi") 
	private void encode_input(byte[] data) {
		byte[] temp = data;
		//编码喂数据
		int iindex = encoder.dequeueInputBuffer(ENCODE_TIME_OUT);
		if(iindex >= 0) {
			ByteBuffer inputBuffer = encoder.getInputBuffers()[iindex];
			inputBuffer.clear();
			inputBuffer.put(temp);
			encoder.queueInputBuffer(iindex, 0, temp.length, presentationTimeUs += 40000, 0);
		} else {
			Log.w(TAG, "encode_input() encoder dequeueInputBuffer() -> " + iindex);
		}
	}
	
	@SuppressLint("NewApi") 
	private int encode_output() {
		int pos = 0;
		// 编码取数据
		BufferInfo bufferInfo = new BufferInfo();
		int oindex = encoder.dequeueOutputBuffer(bufferInfo, ENCODE_TIME_OUT);
		Log.w(TAG, "encode_output() encoder dequeueOutputBuffer() -> "
				+ oindex);
		if (oindex >= 0) {
			ByteBuffer outputBuffer = encoder.getOutputBuffers()[oindex];
			byte[] dstData = new byte[outputBuffer.capacity()];
			outputBuffer.get(dstData);
			// 向队列输入数据
			try {
				if(isFirstI || ((dstData[4] & 0x1F) == 5)) {
					if (!isFirstI) {
						isFirstI = true;
					}
					frameQueue.offer(dstData);
				}
			} catch (Exception e) {
				Log.i(TAG,
						"encode_output() decodeQueue.offer(e) Exception e = "
								+ e);
			}
			// 编码释放数据
			encoder.releaseOutputBuffer(oindex, false);
			oindex = encoder.dequeueOutputBuffer(bufferInfo, 0);
		}
		return oindex;
	}

	/**
	 * 获取解码
	 */
	@SuppressLint("NewApi")
	public void decodeDataAndPlay() {
		Log.i(TAG, "decodeDataAndPlay() queue.take() start waiting! queue.size() = " + frameQueue.size());
		byte[] temp = null;
		try {
			temp = frameQueue.take();
			Log.i(TAG, String.format("decodeDataAndPlay() data[3] = %x, data[4] = %x", temp[3], temp[4]));
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "decodeDataAndPlay() queue.take() Exception e = " + e);
			return;
		}
		Log.i(TAG, "decodeDataAndPlay() queue.take() finish! temp = " + temp);
		// 解码喂数据
		int di = decoder.dequeueInputBuffer(0);
		if (di >= 0) {
			ByteBuffer dinputBuffer = decoder.getInputBuffers()[di];
			dinputBuffer.clear();
			dinputBuffer.put(temp, 0, temp.length);
			decoder.queueInputBuffer(di, 0, temp.length, 0, 0);
			Log.w(TAG, "decodeDataAndPlay() decoder queueInputBuffer() -> " + di);
		} else {
			Log.w(TAG, "decodeDataAndPlay() decoder dequeueInputBuffer() -> "
					+ di);
		}
		// 解码释放数据
		BufferInfo decodeBufferInfo = new BufferInfo();
		int doindex = decoder.dequeueOutputBuffer(decodeBufferInfo, ENCODE_TIME_OUT);
		while (doindex >= 0) {
			decoder.releaseOutputBuffer(doindex, true);
			Log.d(TAG, "decodeDataAndPlay() decoder releaseOutputBuffer() -> "
					+ doindex);
			doindex = decoder.dequeueOutputBuffer(decodeBufferInfo, 0);
		}
	}
}
