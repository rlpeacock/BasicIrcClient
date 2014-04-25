package justdust.basicirc;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Very simple IRC client.  Just the minimum to connect and send messages on a single channel.
 */
public class BasicIrcClient {

    public static final int OUTGOING_QUEUE_SIZE = 50; // queue size for outbound messages
    public static final int SEND_RATE = 500; // interval at which outbound messages are sent

    private final String server;
    private final int port;
    private final String userName;
    private final String channel;
    private final BlockingQueue<Message> outgoingQueue;
    private volatile boolean shutdown;

    /**
     * Construct a client which connects using the supplied parameters.
     *
     * @param server the chat server e.g. irc.freenode.net
     * @param port the port of the chat server e.g. 6667
     * @param userName will be used as both user name and nick
     * @param channel e.g. #test
     */
    public BasicIrcClient(String server, int port, String userName, String channel) {
        this.server = server;
        this.port = port;
        this.userName = userName;
        this.channel = channel;
        outgoingQueue = new ArrayBlockingQueue<>(OUTGOING_QUEUE_SIZE);
    }

    /**
     * Open a connection to the server, log in, and join the channel.
     *
     * @throws IOException if the connection cannot be made
     */
    public void connect() throws IOException {
        Socket socket = new Socket(server, port);
        final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // start a thread which reads from input stream, parses messages and dispatches them
        new Thread(() -> {
            while (!shutdown) {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        Message msg = Message.parseMessage(line);
                        handleMessage(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    shutdown();
                }
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // start a thread which reads from outgoing message queue and sends them
        new Thread(() -> {
            while (!shutdown) {
                try {
                    Message msg = outgoingQueue.take();
                    logMessage("SENDING: ", msg);
                    out.writeBytes(msg.toString());
                    Thread.sleep(SEND_RATE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    e.printStackTrace();
                    shutdown();
                }
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // send login stuff...we seem to be able to get away with just blasting these in without waiting
        // for responses so no need to set up any callbacks :p
        sendMessage(Message.message(MessageType.USER, userName, "0", "*", "Testing"));
        sendMessage(Message.message(MessageType.NICK, userName));
        sendMessage(Message.message(MessageType.JOIN, channel));
    }

    /**
     * Attempt to shut down the client by quitting the server and triggering the connection to be closed.
     */
    public void shutdown() {
        // try to send a quit message
        sendMessage(Message.message(MessageType.QUIT, "leaving..."));
        // wait a bit for the quit message to go out.  It may not go, and it may not even be sendable depending on why
        // we are shutting down but no big deal if this fails.
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        shutdown = true; // connection will be closed when the send and receive loops exit
    }

    /**
     * Queue a message to be sent to the server.
     *
     * @param msg the message to be sent
     * @return whether the message could be queued.  Returns false if the send queue is full.
     */
    public boolean sendMessage(Message msg) {
        return !shutdown && outgoingQueue.offer(msg);
    }

    private void handleMessage(Message msg) {
        logMessage("RECEIVED: ", msg);
        switch (msg.getType()) {
            case PING:
                String pingSender = msg.getParam(0); // for ping, sender is actually passed as a param
                sendMessage(Message.message(MessageType.PONG, userName, pingSender));
                break;
            case PRIVMSG:
                System.out.println(Message.formatAsSpeech(msg));
                break;
            default:
                // do nothing
        }
    }

    private void logMessage(String prefix, Message msg) {
        System.out.print(prefix + msg);
    }

    public static void main(String[] args) throws Exception {
        String usage = "Usage: BasicIrcClient <server> <port> <user> <channel>";
        if (args.length < 4) {
            System.out.println(usage);
            System.exit(1);
        }
        String server = args[0];
        int port = 0;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port specified\n\n" + usage);
            System.exit(1);
        }
        String user = args[2];
        String channel = args[3];

        BasicIrcClient client = new BasicIrcClient(server, port, user, channel);
        client.connect();
    }
}
