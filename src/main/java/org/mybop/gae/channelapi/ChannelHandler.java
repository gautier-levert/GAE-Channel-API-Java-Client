package org.mybop.gae.channelapi;

/**
 * Different events launched by channel
 *
 * @author GautierLevert
 */
public interface ChannelHandler {
    /**
     * Channel connected successfully and is now listening for message
     */
    void onOpen();

    /**
     * Message received from server
     *
     * @param message complete message received
     */
    void onMessage(String message);

    /**
     * Exception occured during communication with server
     *
     * @param e can be IOException or ChannelException
     */
    void onException(Exception e);

    /**
     * connection with server is now closed, user call Channel#close() or server closed connection
     */
    void onClose();
}
