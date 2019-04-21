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

    public PeerConnection(Socket socket) {
        this.socket = socket;
        
        try {
        	inputStream  = new DataInputStream(this.socket.getInputStream());
        	outputStream = new DataOutputStream(this.socket.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
    
    public void send(String message){
    	try{
    		outputStream.writeUTF(message);
    	} catch(Exception e){
    		System.out.println("cant print " + message);
    	}
    	
    }
}