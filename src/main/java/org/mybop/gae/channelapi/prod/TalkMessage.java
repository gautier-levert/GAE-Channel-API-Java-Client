package org.mybop.gae.channelapi.prod;

import org.mybop.gae.channelapi.exception.MalformedMessageException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author GautierLevert
 */
public class TalkMessage {
    /**
     * All known kind of TalkMessage
     */
    public enum MessageEntryKind {
        ME_STRING,
        ME_NUMBER,
        ME_EMPTY,
        ME_TALKMESSAGE
    }

    private List<TalkMessageEntry> mEntries;

    private TalkMessage(List<TalkMessageEntry> entries) {
        mEntries = entries;
    }

    public List<TalkMessageEntry> getEntries() {
        return mEntries;
    }

    /**
     * Transforms String received from server into TalkMessage entity
     *
     * @param message TalkMessage from server (eg. [[0, ""] [1, "something"]])
     * @return TalkMessage entity from this String
     * @throws IOException               low-level error
     * @throws MalformedMessageException String not in expected format
     */
    public static TalkMessage parse(String message) throws IOException, MalformedMessageException {
        Reader reader = null;
        try {
            reader = new StringReader(message);
            if (skipWhitespace(reader) != '[') {
                throw new MalformedMessageException("Expected initial [");
            }
            return new TalkMessage(parseMessage(reader));
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * @return TalkMessage in original format (except \n)
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("[");
        boolean firstEntry = true;
        for (TalkMessageEntry entry : mEntries) {
            if (!firstEntry) {
                str.append(',');
            } else {
                firstEntry = false;
            }
            str.append(entry.toString());
        }
        str.append(']');
        return str.toString();
    }

    /**
     * read TalkMessage char by char to get all entries
     *
     * @param reader reader of the original message
     * @return All entities found in this message
     * @throws IOException
     * @throws MalformedMessageException
     */
    private static List<TalkMessageEntry> parseMessage(Reader reader) throws IOException, MalformedMessageException {
        List<TalkMessageEntry> entries = new ArrayList<TalkMessageEntry>();

        int ch = skipWhitespace(reader);
        while (ch != ']') {
            if (ch < 0) {
                throw new MalformedMessageException("Unexpected end-of-message.");
            }

            if (ch == '[') {
                List<TalkMessageEntry> childEntries = parseMessage(reader);
                entries.add(new TalkMessageEntry(MessageEntryKind.ME_TALKMESSAGE, new TalkMessage(childEntries)));
            } else if (ch == '\"' || ch == '\'') {
                String stringValue = parseStringValue(reader, (char) ch);
                entries.add(new TalkMessageEntry(MessageEntryKind.ME_STRING, stringValue));
            } else if (ch == ',') {
                // blank entry
                entries.add(new TalkMessageEntry(MessageEntryKind.ME_EMPTY, null));
            } else {
                // we assume it's a number
                long numValue = parseNumberValue(reader, (char) ch);
                entries.add(new TalkMessageEntry(MessageEntryKind.ME_NUMBER, numValue));
            }

            //We expect a comma next, or the end of the message
            if (ch != ',') {
                ch = skipWhitespace(reader);
            }

            if (ch != ',' && ch != ']') {
                throw new MalformedMessageException("Expected , or ], found " + ((char) ch));
            } else if (ch == ',') {
                ch = skipWhitespace(reader);
            }
        }

        return entries;
    }

    private static String parseStringValue(Reader reader, char quote) throws IOException {
        StringBuilder str = new StringBuilder();
        for (int ch = reader.read(); ch > 0 && ch != quote; ch = reader.read()) {
            // character " is escaped
            if (ch == '\\') {
                ch = reader.read();
                if (ch < 0) {
                    break;
                }
            }
            str.append((char) ch);
        }
        return str.toString();
    }

    private static long parseNumberValue(Reader reader, char firstChar) throws IOException {
        StringBuilder str = new StringBuilder();
        for (int ch = firstChar; ch > 0 && Character.isDigit(ch); ch = reader.read()) {
            str.append((char) ch);
            reader.mark(1);
        }
        reader.reset();

        return Long.parseLong(str.toString());
    }

    private static int skipWhitespace(Reader reader) throws IOException {
        int ch = reader.read();
        while (ch >= 0) {
            if (!Character.isWhitespace(ch)) {
                return ch;
            }
            ch = reader.read();
        }
        return -1;
    }

    public static class TalkMessageEntry {

        private MessageEntryKind kind;

        private Object value;

        public TalkMessageEntry(MessageEntryKind kind, Object value) {
            this.kind = kind;
            this.value = value;
        }

        public MessageEntryKind getKind() {
            return kind;
        }

        /**
         * Only valid if kind is ME_STRING
         *
         * @return the string value contained in this entry
         * @throws MalformedMessageException if other kind
         */
        public String getStringValue() throws MalformedMessageException {
            if (MessageEntryKind.ME_STRING.equals(kind)) {
                return (String) value;
            } else {
                throw new MalformedMessageException("String value expected, found: " + kind + " (" + value + ")");
            }
        }

        /**
         * Only valid if kind is ME_NUMBER
         *
         * @return the long value contained in this entry
         * @throws MalformedMessageException if other kind
         */
        public long getNumberValue() throws MalformedMessageException {
            if (MessageEntryKind.ME_NUMBER.equals(kind)) {
                return (Long) value;
            } else {
                throw new MalformedMessageException("Number value expected, found: " + kind + " (" + value + ")");
            }
        }

        /**
         * Only valid if kind is ME_TALKMESSAGE
         *
         * @return the TalkMessage value contained in this entry
         * @throws MalformedMessageException if other kind
         */
        public TalkMessage getMessageValue() throws MalformedMessageException {
            if (MessageEntryKind.ME_TALKMESSAGE.equals(kind)) {
                return (TalkMessage) value;
            } else {
                throw new MalformedMessageException("TalkMessage value expected, found: " + kind + " (" + value + ")");
            }
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            if (MessageEntryKind.ME_STRING.equals(kind)) {
                str.append('\"').append(value.toString()).append('\"');
            } else if (!MessageEntryKind.ME_EMPTY.equals(kind)) {
                str.append(value.toString());
            }
            return str.toString();
        }
    }
}
