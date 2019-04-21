package unimelb.bitbox;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class PeerConnection implements Runnable{
	
	protected Socket socket = null;
	protected DataInputStream input = null;
	protected DataOutputStream output = null;

    public PeerConnection(Socket socket) {
        this.socket = socket;
        
        try {
			input  = new DataInputStream(this.socket.getInputStream());
			output = new DataOutputStream(this.socket.getOutputStream());
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
                line = input.readUTF(); 
                System.out.println(line); 
	        } catch (Exception e) {
	        	try{
		        	this.input.close();
		        	this.output.close();
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
    	
    }
}