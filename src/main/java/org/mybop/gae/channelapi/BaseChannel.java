package org.mybop.gae.channelapi;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.mybop.gae.channelapi.exception.ChannelException;

import java.io.IOException;
import java.net.URI;

/**
 * Base implementation for Channel, common operations between dev and prod implementation are here.
 *
 * @author GautierLevert
 * @see org.mybop.gae.channelapi.dev.DevChannel
 * @see org.mybop.gae.channelapi.prod.ProdChannel
 */
public abstract class BaseChannel implements Channel {

	/**
	 * This builder is public for customisation reason
	 */
	public static HttpClientBuilder HTTP_CLIENT_BUILDER = HttpClientBuilder.create();

	/**
	 * base url for channel operation on server
	 */
	protected static final String CHANNEL_URL = "/_ah/channel/";

	/**
	 * Basic ChannelHandler that don't do anything (except logging exception)
	 */
	private static ChannelHandler MOCK_HANDLER = new ChannelHandler() {
		@Override
		public void onOpen() {
		}

		@Override
		public void onMessage(String message) {
		}

		@Override
		public void onException(Exception e) {
			e.printStackTrace();
		}

		@Override
		public void onClose() {
		}
	};

	private URI serverUrl;

	private String token;

	private String clientId = null;

	private ChannelState state = ChannelState.NOT_CONNECTED;

	private ChannelHandler handler = null;

	private CloseableHttpClient httpClient = null;

	private Thread longPollingThread = null;

	public BaseChannel(URI serverUrl, String token) {
		this.serverUrl = serverUrl;
		this.token = token;
	}

	public BaseChannel(URI serverUrl, String token, ChannelHandler handler) {
		this(serverUrl, token);
		setHandler(handler);
	}

	@Override
	public synchronized void open() throws IOException, ChannelException {
		if (ChannelState.NOT_CONNECTED.equals(getState())) {
			setState(ChannelState.CONNECTING);
			httpClient = HTTP_CLIENT_BUILDER.build();
			connect();
			longPoll();
		}
	}

	/**
	 * must initialize everything to be ready for polling
	 * @throws IOException low level error
	 * @throws ChannelException error with protocol format
	 */
	protected abstract void connect() throws IOException, ChannelException;

	/**
	 * launch the polling
	 */
	private synchronized void longPoll() {
		if (ChannelState.CONNECTING.equals(getState())) {
			longPollingThread = newLongPollingThread();
			longPollingThread.start();
			setState(ChannelState.CONNECTED);
			getHandler().onOpen();
		}
	}

	/**
	 * must create a new thread that will perform polling once started
	 * @return a new thread instance able to perform long polling
	 */
	protected abstract Thread newLongPollingThread();

	@Override
	public void close() throws IOException {
		if (ChannelState.CONNECTED.equals(getState())) {
			setState(ChannelState.CLOSING);
			longPollingThread.interrupt();
			try {
				longPollingThread.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			synchronized (this) {
				httpClient.close();
				httpClient = null;
			}
			setState(ChannelState.NOT_CONNECTED);
		}
	}

	@Override
	public URI getServerUrl() {
		return serverUrl;
	}

	@Override
	public String getToken() {
		return token;
	}

	@Override
	public synchronized String getClientId() {
		return clientId;
	}

	/**
	 * change client id after server response
	 * @param clientId new client id
	 */
	protected synchronized void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@Override
	public synchronized ChannelState getState() {
		return state;
	}

	/**
	 * Change current state of this channel
	 * @param state new current state
	 */
	protected synchronized void setState(ChannelState state) {
		this.state = state;
	}

	/**
	 *
	 * @return a correct ChannelHandler implementation
	 */
	protected synchronized ChannelHandler getHandler() {
		return handler == null ? MOCK_HANDLER : handler;
	}

	@Override
	public synchronized void setHandler(ChannelHandler handler) {
		this.handler = handler;
	}

	/**
	 * Give the CloseableHttpClient created just before connection and closed with channel
	 * @return the current HttpClient to use (or null if not connected)
	 */
	protected synchronized HttpClient getHttpClient() {
		return httpClient;
	}
}
