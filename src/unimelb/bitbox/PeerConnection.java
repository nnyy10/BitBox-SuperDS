package unimelb.bitbox;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class PeerConnection implements Runnable{
	
	protected Socket socket = null;
	protected BufferedReader inputStream = null;
	protected BufferedWriter outputStream = null;
	protected ServerMain fileSystemObserver = null;
	
    public PeerConnection(Socket socket) {
        this.socket = socket;
        this.fileSystemObserver = ServerMain.getInstance();
        try {
        	inputStream  = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
        	outputStream = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), "UTF-8"));
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
        while (true) 
        {
            try
            {
                line = inputStream.readLine();
                System.out.println(line);
	        } catch (Exception e) {
	        	this.CloseConnection();
	        	break;
	        }
    	}
    }
    
    public void send(String message){
    	try{
    		outputStream.write(message+"\n");
    		outputStream.flush();
    	} catch(Exception e){
    		System.out.println("cant print " + message);
    		this.CloseConnection();
    	}
    	
    }
}