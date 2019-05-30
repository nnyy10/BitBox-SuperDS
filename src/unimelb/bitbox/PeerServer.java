package unimelb.bitbox;

//A Java program for a TCP_Server
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.io.*;
import unimelb.bitbox.ServerMain;
import unimelb.bitbox.PeerConnection;
import unimelb.bitbox.JSON_process;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;


<<<<<<< Updated upstream:src/unimelb/bitbox/PeerServer.java
public class PeerServer extends PeerConnection implements Runnable {
=======
public class TCP_Server extends TCP_peerconnection implements Runnable {
>>>>>>> Stashed changes:src/unimelb/bitbox/TCP_Server.java

	private static Logger log = Logger.getLogger(PeerConnection.class.getName());
	
	public static int numberOfConnections = 0;
	
<<<<<<< Updated upstream:src/unimelb/bitbox/PeerServer.java
	public PeerServer(Socket socket) {
=======
	public TCP_Server(Socket socket) {
>>>>>>> Stashed changes:src/unimelb/bitbox/TCP_Server.java
		super(socket);
		numberOfConnections++;
		this.fileSystemObserver.add(this);
		log.info("server successfully connected to " + socket.getRemoteSocketAddress().toString());
	}
	
	protected void finalize() throws Throwable {
		this.CloseConnection();
		numberOfConnections--;
	}
}