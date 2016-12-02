package com.xinwei.cm.pool;

public class FrameBuffer<D> {
	
	private D mData = null;
	private long mSequence = -1;
	
	public FrameBuffer(D data) {
		this.mData = data;
	}

	public FrameBuffer(D mData, long mSequence) {
		super();
		this.mData = mData;
		this.mSequence = mSequence;
	}
	
	public void changeData(D data) {
		mData = data;
	}

	public D getmData() {
		return mData;
	}

	public long getmSequence() {
		return mSequence;
	}

	public void setmSequence(long mSequence) {
		this.mSequence = mSequence;
	}
	
	
}
