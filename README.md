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