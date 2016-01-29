package org.mybop.gae.channelapi.dev;


import java.io.IOException;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.util.EntityUtils;

/**
 * Helper to simply get result of an HTTP request
 *
 * @author GautierLevert
 */
public class XHR {

    private String responseText;

    private int status;

    private String statusText;

    /**
     * @param response http server response
     * @throws java.io.IOException low-level exception
     */
    public XHR(HttpResponse response) throws IOException {
        StatusLine statusLine = response.getStatusLine();
        this.status = statusLine.getStatusCode();
        this.statusText = statusLine.getReasonPhrase();
        this.responseText = EntityUtils.toString(response.getEntity());
    }

    /**
     * @return the text representation of the status code given by server
     */
    public String getStatusText() {
        return this.statusText;
    }

    /**
     * @return HTTP response status code
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * @return true if the request was successful (status code equals 200)
     */
    public boolean isSuccess() {
        return (this.status == HttpStatus.SC_OK);
    }

    /**
     * @return response entity as string
     */
    public String getResponseText() {
        return this.responseText;
    }

    @Override
    public String toString() {
        return "XHR{" +
                "responseText='" + responseText + '\'' +
                ", status=" + status +
                ", statusText='" + statusText + '\'' +
                '}';
    }
}