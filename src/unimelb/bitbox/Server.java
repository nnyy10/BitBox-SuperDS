package unimelb.bitbox;

//A Java program for a Server 
import java.net.*; 
import java.io.*;


public class Server implements Runnable
{ 
    //initialize socket and input stream 
    private Socket          socket   = null; 
    private ServerSocket    server   = null; 
    private DataInputStream in       =  null; 
  
    // constructor with port 
    public Server(int port) 
    { 
        // starts server and waits for a connection 
        try
        { 
            server = new ServerSocket(port); 
            System.out.println("Server: Server started"); 
  
            System.out.println("Server: Waiting for a client ..."); 
  
            socket = server.accept(); 
            System.out.println("Server: Client accepted"); 
  
            // takes input from the client socket 
            in = new DataInputStream( 
                new BufferedInputStream(socket.getInputStream())); 
  
            

        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        } 
    } 
    
    public void run() {
    	String line = ""; 
    	  
        // reads message from client until "Over" is sent 
        while (!line.equals("Over")) 
        { 
            try
            { 
                line = in.readUTF();
                handleIncomingMessage(line);
                //System.out.println(line); 

            } 
            catch(IOException i) 
            { 
                System.out.println(i); 
            } 
        } 
        System.out.println("Closing connection"); 
        
        // close connection 
        try {
			socket.close();
			in.close(); 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    }
    
    private void handleIncomingMessage(String message){
    	switch(message) {
    	   case "mkdir" :
    	      System.out.println("making directory");
    	      break; // optional
    	   
    	   case "deldir":
    		   System.out.println("deleting directory");
    	      break; // optional
    	   
    	   // You can have any number of case statements.
    	   default : // Optional
    	      System.out.println("message invalid");
    	}
    }
    
}