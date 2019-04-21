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
		try {

			inputStream = new DataInputStream(this.socket.getInputStream());
			outputSteam = new DataOutputStream(this.socket.getOutputStream());

			outputSteam.writeUTF("handshake");
			System.out.println(inputStream.readUTF());
		} catch (IOException e) {
			System.out.println("Connection to: FAILED");
			e.printStackTrace();
		}
	}

	public void run() {
// string to read message from input
		String line = "";

		// keep reading until "Over" is input
		while (!line.equals("Over"))
		{
			try
			{
				line = input.readLine();
				outputStream.writeUTF(line);

		} catch (IOException i) {
	        	try{
		        	this.inputStram.close();
		        	this.outputSteam.close();
		        	this.socket.close();
		        	break;
		        } catch(Exception e1){
		        	
		        	System.out.println(e1);
		        }
	            //report exception somewhere.
	            i.printStackTrace();
			}
		}

	}
}