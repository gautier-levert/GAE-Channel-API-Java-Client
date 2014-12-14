package org.mybop.gae.channelapi.exception;

/**
 * Basic Exception for channel operation
 * @author GautierLevert
 */
public class ChannelException extends Exception {
	public ChannelException() {
	}

	public ChannelException(String message) {
		super(message);
	}

	public ChannelException(String message, Throwable cause) {
		super(message, cause);
	}

	public ChannelException(Throwable cause) {
		super(cause);
	}
}
