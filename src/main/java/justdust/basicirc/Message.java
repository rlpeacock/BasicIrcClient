package justdust.basicirc;

import com.google.common.collect.ImmutableList;

/**
 * This class represents a message as defined by the IRC protocol.  The parameters are positional and their meaning
 * will depend on the MessageType.
 */
public class Message {

    // this string at the start of the body of a PRIVMSG indicates an emote
    public static final String ACTION = "\u0001ACTION";

    private final String sender;
    private final MessageType type;
    private final ImmutableList<String> params;

    public Message(String sender, MessageType type, ImmutableList<String> params) {
        this.sender = sender;
        this.type = type;
        this.params = params;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getParam(int param) {
        if (params.size() > param) {
            return params.get(param);
        }
        return "";
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        if (sender != null && !sender.isEmpty()) {
            sb.append(sender).append(" ");
        }
        sb.append(type.getId());
        for (String param: params) {
            if (param != null && !param.isEmpty()) {
                sb.append(" ").append(param);
            }
        }
        sb.append("\r\n");
        return sb.toString();
    }

    /**
     * Convenience method for constructing messages to be sent by client.
     *
     * @param type type of message to construct
     * @param params parameters appropriate for this message type
     * @return the message
     */
    public static Message message(MessageType type, String... params) {
        // since client-originating messages don't use sender, leave it null.
        return new Message(null, type,ImmutableList.copyOf(params));
    }



    /**
     * Attempt to parse a line of data read from the server into a Message.  Returns a message of type ERR_MALFORMED if
     * the data could not be parsed.
     *
     * This isn't really a parser, just a heuristic approach to keep the code simple.  The basic form of a message is:
     * [sender] [type] [params]
     * where params are space separated until you hit one that starts with a colon.  Everything after the colon
     * constitutes one 'param', which is the body of the message.  Note that the sender also starts with a colon so
     * we have to get that out first.  Also, sender is optional.
     *
     * @param line the line sent by the server
     * @return the message.
     */
    public static Message parseMessage(String line) {
        // Split into max of 3 parts on whitespace.  We should end up with sender, type, and params
        String[] parts = line.trim().split("\\s", 3);
        if (parts.length == 2) {
            // if we have only 2 parts then we must not have a sender and the 1st part is type
            // this fails if we get a message that is [sender] [type] with no params.  Hey, this is just a toy!
            return new Message(null, MessageType.getTypeForId(parts[0]), parseParams(parts[1]));
        } else if (parts.length == 3) {
            return new Message(parts[0], MessageType.getTypeForId(parts[1]), parseParams(parts[2]));
        }
        // our crude attempt at parsing has failed...just log it as unknown
        return new Message("unknown", MessageType.ERR_MALFORMED, ImmutableList.of(line));
    }

    /**
     * Parse the params component of a message.  The params are whitespace separated until it gets to one that
     * starts with a ':'.  After that the rest of the line is a single parameter.
     *
     * @param params a string containing the parameters
     * @return the parameters broken out into a list.  Returns empty list if string is empty.
     */
    private static ImmutableList<String> parseParams(String params) {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        String[] parts = params.split("\\s:");
        for (String part: parts[0].split("\\s")) {
            builder.add(part);
        }
        if (parts.length > 1 && !parts[1].isEmpty()) {
            builder.add(parts[1]);
        }
        return builder.build();
    }

    /**
     * Parse the nick from a name string.  The name string will have nick, followed by '!', followed by user
     * and host.  The nick may be prefixed by a ':'
     *
     * @param name the full name string
     * @return just the nick
     */
    public static String getNickFromName(String name) {
        // if we start with a colon, skip it
        int start = name.indexOf(":") == 0 ? 1 : 0;
        // after ! we are in location part of name
        int end = name.indexOf("!");
        if (end < 0) {
            end = name.length();
        }
        return name.substring(start, end);
    }

    /**
     * Render a PRIVMSG message as chat formatted text.  Format is:
     *   [channel] sender: message
     * unless it is an emote, in which case it renders as:
     *   [channel] sender message
     * If a non-PRIVMSG is passed in, return empty string.
     *
     * @param msg the message
     * @return the chat formatted text
     */
    public static String formatAsSpeech(Message msg) {
        String speech = "";
        if (msg.getType() == MessageType.PRIVMSG) {
            String msgSender = msg.getSender() != null ? msg.getSender() : "unknown";
            String recipient = msg.getParam(0); // will be us or channel
            String body = msg.getParam(1); // the text of the message that was sent or ACTION for emotes
            if (body.indexOf(ACTION) == 0) {
                // special handling for emote keyword: remove keyword and format output slightly differently
                body = body.substring(ACTION.length()).trim();
                speech = String.format("[%s] %s %s", recipient, Message.getNickFromName(msgSender), body);
            } else {
                speech = String.format("[%s] %s: %s", recipient, Message.getNickFromName(msgSender), body);
            }
        }
        return speech;
    }


}
