package com.xinwei.cm.pool;

public class ObjectPoolImp extends ObjectPool<FrameBuffer<byte[]>>{

	private int mSize;
	
	public ObjectPoolImp(int size) {
		mSize = size;
	}

	@Override
	public FrameBuffer<byte[]> create() {
		return new FrameBuffer<byte[]>(new byte[mSize]);
	}

}
