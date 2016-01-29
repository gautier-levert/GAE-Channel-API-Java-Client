package org.mybop.gae.channelapi.exception;

/**
 * ChannelException specific for TalkMessage operations
 *
 * @author GautierLevert
 */
public class MalformedMessageException extends ChannelException {
    public MalformedMessageException() {
    }

    public MalformedMessageException(String message) {
        super(message);
    }

    public MalformedMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedMessageException(Throwable cause) {
        super(cause);
    }
}
