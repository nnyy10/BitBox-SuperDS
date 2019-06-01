package unimelb.bitbox;

//A Java program for a TCP_Server

import java.net.*;
import java.util.logging.Logger;

public class TCP_Server extends TCP_peerconnection implements Runnable {

    private static Logger log = Logger.getLogger(PeerConnection.class.getName());

    public static int numberOfConnections = 0;

    public TCP_Server(Socket socket) {
        super(socket);
        numberOfConnections++;
        this.fileSystemObserver.add(this);
        log.info("server successfully connected to " + socket.getRemoteSocketAddress().toString());
    }
}