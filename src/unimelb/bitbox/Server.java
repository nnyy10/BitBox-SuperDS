package unimelb.bitbox;

//A Java program for a Server 
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import unimelb.bitbox.ServerMain;
import unimelb.bitbox.PeerConnection;
import unimelb.bitbox.JSON_process;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;


public class Server extends PeerConnection implements Runnable {

	public static int numberOfConnections = 0;
	
	public Server(Socket socket) {
		super(socket);
		numberOfConnections++;
		this.fileSystemObserver.add(this);
		System.out.println("server successfully connected to " + socket.getRemoteSocketAddress().toString());
//		for(FileSystemEvent e:this.fileSystemObserver.fileSystemManager.generateSyncEvents()){
//			System.out.println(e);
//		}
	}
	
	protected void CloseConnection(){
		super.CloseConnection();
		numberOfConnections--;
	}
	
	protected void finalize() throws Throwable {
		this.CloseConnection();
		System.out.println("in server finalize");
	}
}