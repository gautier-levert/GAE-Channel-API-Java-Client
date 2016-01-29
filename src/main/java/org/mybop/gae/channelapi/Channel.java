package org.mybop.gae.channelapi;

import org.mybop.gae.channelapi.exception.ChannelException;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

/**
 * Java API for GAE Channel API implementation
 *
 * @author GautierLevert
 */
public interface Channel extends Closeable {

    /**
     * Different states in which a Channel can be
     */
    public static enum ChannelState {
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED,
        CLOSING
    }

    /**
     * Initialize communication and launch message polling
     *
     * @throws IOException      low-level error occurred
     * @throws ChannelException protocol error
     */
    void open() throws IOException, ChannelException;

    /**
     * @return server URL given during creation
     */
    URI getServerUrl();

    /**
     * @return token given during creation
     */
    String getToken();

    /**
     * This is the key used by server for Channel creation
     *
     * @return Client ID given by server during initialization
     */
    String getClientId();

    /**
     * @return current state of this Channel
     */
    ChannelState getState();

    /**
     * You can change channel event handler anytime
     *
     * @param handler new message and other events receiver
     */
    void setHandler(ChannelHandler handler);
}
