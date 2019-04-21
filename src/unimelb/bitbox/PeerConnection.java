package unimelb.bitbox;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class PeerConnection implements Runnable{
	
	protected Socket socket = null;
	protected DataInputStream inputStream = null;
	protected DataOutputStream outputStream = null;
	protected ServerMain fileSystemObserver = null;
	
    public PeerConnection(Socket socket) {
        this.socket = socket;

        try {
        	inputStream  = new DataInputStream(this.socket.getInputStream());
        	outputStream = new DataOutputStream(this.socket.getOutputStream());
		} catch (IOException e) {
			this.CloseConnection();
		}
    }

	protected void CloseConnection(){
		System.out.println("Closing Connection");
		try{
        	this.inputStream.close();
        	this.outputStream.close();
        	this.socket.close();
        } catch(Exception e){
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
	        	this.CloseConnection();
	        	break;
	        }
    	}
    }
    
    public void send(String message){
    	try{
    		outputStream.writeUTF(message);
    	} catch(Exception e){
    		System.out.println("cant print " + message);
    		this.CloseConnection();
    	}
    	
    }
}