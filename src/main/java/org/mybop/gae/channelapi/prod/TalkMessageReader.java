package org.mybop.gae.channelapi.prod;

import org.mybop.gae.channelapi.exception.ChannelException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Reader implementation able to parse TalkMessage
 *
 * @author GautierLevert
 */
public class TalkMessageReader extends Reader {

    private BufferedReader reader;

    /**
     * This reader only work with a BufferedReader for utility and performance reason
     *
     * @param reader BufferedReader from TalkMessage stream
     */
    public TalkMessageReader(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return reader.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * This function will read the stream until it founds a complete TalkMessage or channel close
     *
     * @return next iteration of talk message in this stream
     * @throws IOException      low-level error
     * @throws ChannelException error with protocol
     */
    public TalkMessage readMessage() throws IOException, ChannelException {
        String submission = readSubmission();
        return submission == null ? null : TalkMessage.parse(submission);
    }

    /**
     * Based on experience, a TalkMessage begins with a line with the number of chars contained in this message
     *
     * @return the content string in the message based on first line count
     * @throws ChannelException if there is a problem with the expected format
     * @throws IOException      error during communication
     */
    private String readSubmission() throws ChannelException, IOException {
        try {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }

            int numChars = Integer.parseInt(line);
            char[] chars = new char[numChars];
            int total = 0;
            while (total < numChars) {
                int numRead = reader.read(chars, total, numChars - total);
                total += numRead;
            }
            return new String(chars);
        } catch (NumberFormatException e) {
            throw new ChannelException("Submission was not in expected format.", e);
        }
    }
}
