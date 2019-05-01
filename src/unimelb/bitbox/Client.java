package unimelb.bitbox;

//A Java program for a Server 
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import unimelb.bitbox.ServerMain;
import unimelb.bitbox.PeerConnection;
import unimelb.bitbox.JSON_process;


public class Client extends PeerConnection implements Runnable {
	public Client(Socket socket) {
		super(socket);
		this.fileSystemObserver.add(this);
		
		try {

			String Cmesg = JSON_process.HANDSHAKE_REQUEST(this.socket.getLocalAddress().toString(), this.socket.getLocalPort());
			outputStream.write(Cmesg+"\n");
			outputStream.flush();
			System.out.println("Client sent: " + Cmesg);
			
			System.out.println("Clinet successfully recieved: "+inputStream.readLine());
		} catch (IOException e) {
			System.out.println("Connection FAILED.");
			this.CloseConnection();
		}
		System.out.println("client successfully connected to " + socket.getRemoteSocketAddress().toString());
	
	}
	
	protected void finalize() throws Throwable {
		this.CloseConnection();
	}

}
