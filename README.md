# CodecCamera by CM!

## 遇到问题

### 在海信终端android 4.4.2:可长期运行,预览,与编解码界面都正常.

### 在180终端以及186终端,都是android 4.4.4:开始一段时间正常,随后,预览界面正常,编解码界面停滞不动
	在此过程中收集到的错误信息有如下
	1. Stream not supported [dec->codec->decode]
	2. ASYNC:error while processing buffers:OMX_ErrorNotImplemented.
	3. Codec reported an error (omx error 0x80001006, internalError -2147483648)
	4. MediaCodec dequeueoutputbuffer illegalstateexception (网上找到例子,部分机型没有配置SPS,PPS会报这个错误)

## 解决停滞不动的问题:
	在编码发送数据时,遇到I帧,就在I帧前添加SPS,PPS.解决问题.
	
## 关于:H264 Android编解码类MediaCodec,ByteBuffer扫盲
		经MediaCodec编码后,在取H264数据时,应注意,H264的长度在MediaCodec.BufferInfo对象的成员变量
	size,而不是ByteBuffer.capacity()的长度,打印发现H264的实际长度比outBuffer.capacity()
	小很多,所以,在解码的时候,如果使用了ByteBuffer.capacity()的长度取数据,导致视频花屏,而且卡顿