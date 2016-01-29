package org.mybop.gae.channelapi.prod;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.mybop.gae.channelapi.BaseChannel;
import org.mybop.gae.channelapi.ChannelHandler;
import org.mybop.gae.channelapi.exception.ChannelException;
import org.mybop.gae.channelapi.exception.MalformedMessageException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpRequestBase;
import cz.msebera.android.httpclient.client.utils.URLEncodedUtils;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;

/**
 * GAE Channel API implementation for production Google App Eengine servlet.
 *
 * @author GautierLevert
 */
public class ProdChannel extends BaseChannel {

    /**
     * Google Channel API server URL
     */
    private static final URI PROD_TALK_URL = URI.create("https://talkgadget.google.com/talkgadget/");

    private int requestId = 0;

    private String sessionId;

    private String sid;

    private long messageId = 1L;

    public ProdChannel(URI serverUrl, String token) {
        super(serverUrl, token);
    }

    public ProdChannel(URI serverUrl, String token, ChannelHandler handler) {
        super(serverUrl, token, handler);
    }

    @Override
    protected synchronized void connect() throws IOException, ChannelException {
        initialize();
        fetchSid();
        register();
    }

    /**
     * Sets up the initial connection, passes in the token
     */
    private synchronized void initialize() throws IOException, ChannelException {
        JSONObject xpc;
        try {
            xpc = new JSONObject();
            xpc.put("cn", RandomStringUtils.random(10, true, false));
            xpc.put("tp", "null");
            xpc.put("lpu", PROD_TALK_URL + "xpc_blank");
            xpc.put("ppu", getServerUrl() + CHANNEL_URL + "xpc_blank");
        } catch (JSONException e) {
            throw new ChannelException("error with JSON API", e);
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("token", getToken()));
        params.add(new BasicNameValuePair("xpc", xpc.toString()));

        String url = PROD_TALK_URL + "d?" + URLEncodedUtils.format(params, "UTF-8");

        HttpResponse resp = getHttpClient().execute(new HttpGet(url));
        if (resp.getStatusLine().getStatusCode() > 299) {
            throw new ChannelException("Initialize failed, server response: " + resp.getStatusLine());
        }

        String html = EntityUtils.toString(resp.getEntity(), "UTF-8");

        Pattern p = Pattern.compile("chat\\.WcsDataClient\\(([^\\)]+)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m = p.matcher(html);
        if (m.find()) {
            String fields = m.group(1);
            p = Pattern.compile("\"([^\"]*?)\"[\\s,]*", Pattern.MULTILINE);
            m = p.matcher(fields);

            for (int i = 0; i < 7; i++) {
                if (!m.find()) {
                    throw new ChannelException("Expected iteration #" + i + " to find something.");
                }
                if (i == 2) {
                    setClientId(m.group(1));
                } else if (i == 3) {
                    sessionId = m.group(1);
                } else if (i == 6) {
                    if (getToken() == null || !getToken().equals(m.group(1))) {
                        throw new ChannelException("Tokens do not match!");
                    }
                }
            }
        }
    }

    /**
     * Fetches and parses the SID, which is a kind of session ID.
     */
    private synchronized void fetchSid() throws IOException, MalformedMessageException {
        URI url = getBindUrl(new BasicNameValuePair("CVER", "1"));
        HttpPost request = new HttpPost(url);
        request.setEntity(new UrlEncodedFormEntity(Collections.singletonList(new BasicNameValuePair("count", "0"))));
        HttpResponse httpResponse = getHttpClient().execute(request);
        String resp = EntityUtils.toString(httpResponse.getEntity());
        resp = resp.substring(resp.indexOf('\n'));
        TalkMessage msg = TalkMessage.parse(resp);
        TalkMessage.TalkMessageEntry entry = msg.getEntries().get(0);
        entry = entry.getMessageValue().getEntries().get(1);
        List<TalkMessage.TalkMessageEntry> entries = entry.getMessageValue().getEntries();
        if (!entries.get(0).getStringValue().equals("c")) {
            throw new IOException("Expected first value to be 'c', found: " + entries.get(0).getStringValue());
        }

        sid = entries.get(1).getStringValue();
    }

    /**
     * We need to make this "connect" request to set up the binding.
     */
    private synchronized void register() throws IOException, ChannelException {
        URI url = getBindUrl(new BasicNameValuePair("AID", Long.toString(messageId)), new BasicNameValuePair("CVER", "1"));

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("count", "1"));
        params.add(new BasicNameValuePair("ofs", "0"));
        params.add(new BasicNameValuePair("req0_m", "[\"connect-add-client\"]"));
        params.add(new BasicNameValuePair("req0_c", getClientId()));
        params.add(new BasicNameValuePair("req0__sc", "c"));

        HttpPost request = new HttpPost(url);
        request.setEntity(new UrlEncodedFormEntity(params));
        getHttpClient().execute(request);
    }

    @Override
    protected Thread newLongPollingThread() {
        return new Thread() {

            private HttpRequestBase currentRequest = null;

            private TalkMessageReader poll() throws IOException, MalformedMessageException {
                URI url;
                synchronized (ProdChannel.this) {
                    url = getBindUrl(new BasicNameValuePair("CI", "0"),
                            new BasicNameValuePair("AID", Long.toString(messageId)),
                            new BasicNameValuePair("TYPE", "xmlhttp"),
                            new BasicNameValuePair("RID", "rpc"));
                }

                HttpGet request = new HttpGet(url);
                synchronized (this) {
                    currentRequest = request;
                }
                InputStream stream = getHttpClient().execute(currentRequest).getEntity().getContent();
                return new TalkMessageReader(new BufferedReader(new InputStreamReader(stream)));
            }

            private void handleMessage(TalkMessage msg) throws MalformedMessageException {
                synchronized (ProdChannel.this) {
                    List<TalkMessage.TalkMessageEntry> entries = msg.getEntries();
                    msg = entries.get(0).getMessageValue();

                    entries = msg.getEntries();
                    messageId = entries.get(0).getNumberValue();

                    msg = entries.get(1).getMessageValue();
                    entries = msg.getEntries();

                    if (entries.get(0).getKind() == TalkMessage.MessageEntryKind.ME_STRING && entries.get(0).getStringValue().equals("c")) {
                        msg = entries.get(1).getMessageValue();
                        entries = msg.getEntries();

                        String thisSessionID = entries.get(0).getStringValue();
                        if (!thisSessionID.equals(sessionId)) {
                            sessionId = thisSessionID;
                        }

                        msg = entries.get(1).getMessageValue();
                        entries = msg.getEntries();

                        if (entries.get(0).getStringValue().equalsIgnoreCase("ae")) {
                            String message = entries.get(1).getStringValue();
                            getHandler().onMessage(message);
                        }
                    }
                }
            }

            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        TalkMessageReader messageReader = null;
                        try {
                            messageReader = poll();
                            TalkMessage message;
                            while ((message = messageReader.readMessage()) != null
                                    && !isInterrupted()) {
                                handleMessage(message);
                            }
                        } finally {
                            if (messageReader != null) {
                                try {
                                    messageReader.close();
                                } catch (IOException ignored) {
                                }
                            }
                            currentRequest = null;
                        }
                    } catch (Exception e) {
                        synchronized (ProdChannel.this) {
                            if (!ChannelState.CLOSING.equals(ProdChannel.this.getState())
                                    || !(e instanceof SocketException)) {
                                getHandler().onException(e);
                            }
                        }
                    }
                    if (!isInterrupted()) {
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            interrupt();
                        }
                    }
                }
                synchronized (ProdChannel.this) {
                    getHandler().onClose();
                    setState(ChannelState.NOT_CONNECTED);
                }
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
     * Gets the URL to the "/bind" endpoint.
     */
    private synchronized URI getBindUrl(NameValuePair... extraParams) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("token", getToken()));
        params.add(new BasicNameValuePair("gsessionid", sessionId));
        params.add(new BasicNameValuePair("clid", getClientId()));
        params.add(new BasicNameValuePair("prop", "data"));
        params.add(new BasicNameValuePair("zx", RandomStringUtils.random(12, true, false)));
        params.add(new BasicNameValuePair("t", "1"));
        if (sid != null && !"".equals(sid)) {
            params.add(new BasicNameValuePair("SID", sid));
        }
        Collections.addAll(params, extraParams);

        params.add(new BasicNameValuePair("RID", Integer.toString(requestId)));
        requestId++;

        return PROD_TALK_URL.resolve("dch/bind?VER=8&" + URLEncodedUtils.format(params, "UTF-8"));
    }
}
