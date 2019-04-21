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
		try {
			inputStream = new DataInputStream(this.socket.getInputStream());
			outputStream = new DataOutputStream(this.socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		String line = ""; 
        
        // reads message from client until "Over" is sent 
        while (!line.equals("Over")) 
        { 
            try
            { 
                line = inputStream.readUTF(); 
                System.out.println(line); 
	        } catch (Exception e) {
	        	try{
		        	this.inputStream.close();
		        	this.outputStream.close();
		        	this.socket.close();
		        	break;
		        } catch(Exception e1){
		        	
		        	System.out.println(e1);
		        }
	            //report exception somewhere.
	            e.printStackTrace();
	        }
    	}

	}
}