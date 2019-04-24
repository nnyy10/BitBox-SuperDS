package unimelb.bitbox;

//A Java program for a Server 
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import unimelb.bitbox.ServerMain;
import unimelb.bitbox.PeerConnection;
import unimelb.bitbox.JSON_process;


public class Server extends PeerConnection implements Runnable {

	public Server(Socket socket) {
		super(socket);
		
		this.fileSystemObserver.add(this);
		System.out.println("server successfully connected to " + socket.getRemoteSocketAddress().toString() + Integer.toString(socket.getPort()));
	}
}