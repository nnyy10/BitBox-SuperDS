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

			outputStream.write("handshake"+"\n");
			outputStream.flush();
			System.out.println(inputStream.readLine());
			this.fileSystemObserver.add(this);
			//outputStream.writeUTF(JSON_process.HANDSHAKE_REQUEST());
			//String response =inputStream.readUTF();
			//JSON_process.getMessage(response);
		} catch (IOException e) {
			System.out.println("Connection to: FAILED");
			e.printStackTrace();
		}
		System.out.println("client successfully connected to " + socket.getRemoteSocketAddress().toString() + Integer.toString(socket.getPort()));
	
	}

}