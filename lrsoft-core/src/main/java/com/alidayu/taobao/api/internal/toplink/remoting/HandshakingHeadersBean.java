package com.alidayu.taobao.api.internal.toplink.remoting;

import com.alidayu.taobao.api.internal.toplink.channel.websocket.WebSocketClientHelper;

import java.net.URI;
import java.util.Map;

public class HandshakingHeadersBean {
	
	protected Map<String, String> headers;

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	
	protected final void setUri(URI uri) {
		WebSocketClientHelper.setHeaders(uri, this.headers);
	}
}
