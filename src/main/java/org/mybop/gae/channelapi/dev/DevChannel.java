package org.mybop.gae.channelapi.dev;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.mybop.gae.channelapi.BaseChannel;
import org.mybop.gae.channelapi.ChannelHandler;
import org.mybop.gae.channelapi.exception.ChannelException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URI;
import java.net.URLEncoder;

/**
 * GAE Channel API implementation for development implementation of GAE servlet.
 *
 * @author GautierLevert
 */
public class DevChannel extends BaseChannel {

	public DevChannel(URI serverUrl, String token) {
		super(serverUrl, token);
	}

	public DevChannel(URI serverUrl, String token, ChannelHandler handler) {
		super(serverUrl, token, handler);
	}

	@Override
	protected synchronized void connect() throws IOException, ChannelException {
		URI url = getDevUrl("connect");

		HttpGet request = new HttpGet(url);
		XHR resp = new XHR(getHttpClient().execute(request));
		if (!resp.isSuccess()) {
			throw new ChannelException("server response is invalid");
		} else {
			setClientId(resp.getResponseText());
		}
	}

	@Override
	protected Thread newLongPollingThread() {
		return new Thread() {

			private HttpRequestBase currentRequest = null;

			@Override
			public void run() {
				while (!isInterrupted()) {
					try {
						HttpGet request = new HttpGet(getDevUrl("poll"));
						synchronized (this) {
							currentRequest = request;
						}
						XHR xhr = new XHR(getHttpClient().execute(request));
						if (xhr.isSuccess()) {
							String message = StringUtils.chomp(xhr.getResponseText());
							synchronized (DevChannel.this) {
								getHandler().onMessage(message);
							}
						} else {
							throw new ChannelException("Invalid server response: " + xhr.getStatus() + " - " + xhr.getStatusText());
						}
					} catch (Exception e) {
						synchronized (DevChannel.this) {
							if (!ChannelState.CLOSING.equals(DevChannel.this.getState())
									|| !(e instanceof SocketException)) {
								getHandler().onException(e);
							}
						}
					}
					if (!isInterrupted()) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							interrupt();
						}
					}
				}
				try {
					HttpGet request = new HttpGet(getDevUrl("disconnect"));
					getHttpClient().execute(request);
				} catch (ClientProtocolException ignored) {
				} catch (IOException ignored) {
				}
				getHandler().onClose();
				setState(ChannelState.NOT_CONNECTED);
			}

			@Override
			public void interrupt() {
				synchronized (this) {
					if (currentRequest != null) {
						currentRequest.abort();
					}
				}
				super.interrupt();
			}
		};
	}

	/**
	 * Helper to get URL formatted for development server
	 *
	 * @param command channel operation
	 * @return desired URL
	 */
	private URI getDevUrl(String command) {
		try {
			return getServerUrl().resolve(CHANNEL_URL + "dev?command=" + command + "&channel=" + URLEncoder.encode(getToken(), "UTF-8") + (getClientId() != null ? "&client=" + URLEncoder.encode(getClientId(), "UTF-8") : ""));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return getServerUrl().resolve(CHANNEL_URL);
		}
	}
}
