package unimelb.bitbox;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class EntryPointServer implements Runnable{

    protected int serverPort = 0;
    protected ServerSocket serverSocket = null;
    protected boolean isStopped = false;
    protected Thread runningThread = null;
    
    //Variables for reading JSON messages
    String Smesg,readmesg,host; long port;
    
    protected ExecutorService threadPool =Executors.newFixedThreadPool(10);

    public EntryPointServer(int port){
        this.serverPort = port;
    }

    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(! isStopped()){
            Socket tempServerSocket = null;
            try {
            	tempServerSocket = this.serverSocket.accept();
            	BufferedReader inputStream  = new BufferedReader(new InputStreamReader(tempServerSocket.getInputStream(), "UTF-8"));
            	BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(tempServerSocket.getOutputStream(), "UTF-8"));
            	readmesg = inputStream.readLine();
            	System.out.println("Server recieved: "+ readmesg);
            	
            	try {
            		JSONParser parser = new JSONParser();
            		JSONObject jsonMsg = (JSONObject) parser.parse(readmesg);
                    String jsonCommand = (String) jsonMsg.get("command");
                    JSONObject jsonHostPort = (JSONObject) jsonMsg.get("hostPort");
                    String jsonHost = (String) jsonHostPort.get("host");
                    String jsonPort = (String) jsonHostPort.get("host");
                    if(jsonHost == null || jsonPort==null || jsonCommand==null|| !jsonCommand.equals("HANDSHAKE_REQUEST")) {
                    	String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL();
                    	outputStream.write(invalidProtocolMsg+"\n");
                    	outputStream.flush();
                    	this.CloseConnection(inputStream, outputStream, tempServerSocket);
                    	System.out.println("Handshake request invalid, closing socket.");
                    	continue;
                    }
                    
                	System.out.println("Handshake Request Valid");
                    
            	}catch (Exception e){
            		String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL();
                	outputStream.write(invalidProtocolMsg+"\n");
                	this.CloseConnection(inputStream, outputStream, tempServerSocket);
                	System.out.println("Handshake request invalid, closing socket.");
                	continue;
                }
            
            	if(Server.numberOfConnections<10) {
                    String handshakeReponseMsg=JSON_process.HANDSHAKE_RESPONSE(tempServerSocket.toString(), tempServerSocket.getPort());
                    try{
                		outputStream.write(handshakeReponseMsg+"\n");
                		outputStream.flush();
                		System.out.println("Client accepted, sending response message: " + handshakeReponseMsg);
                		this.threadPool.execute(new Server(tempServerSocket));
                	} catch(Exception e){
                		System.out.println("Client accepted but error sending handshake response, closing connection");
                		this.CloseConnection(inputStream, outputStream, tempServerSocket);
                		continue;
            		}
            	} 	
            	else {
            		System.out.println("Max connection of " + Server.numberOfConnections+ " limit reached. Sending connection refused message.");
            		ArrayList<PeerConnection> connections = ServerMain.getInstance().getlist();
            		String [] tempIPlist = new String[connections.size()];
            		int [] tempPrlist = new int [connections.size()];
            		
            		for(int i= 0; i<ServerMain.getInstance().getlist().size(); i++){
            			tempIPlist[i]=(connections.get(i).socket.getRemoteSocketAddress().toString());
            			tempPrlist[i]=(connections.get(i).socket.getPort());
            		}
            		JSON_process.CONNECTION_REFUSED(tempIPlist, tempPrlist);
            	}
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    break;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
            
        }
        this.threadPool.shutdown();
        System.out.println("Server Stopped.") ;
    }

	protected void CloseConnection(BufferedReader inputStream, BufferedWriter outputStream, Socket socket){
		System.out.println("Closing New Socket Connection");
		try{
        	inputStream.close();
        	outputStream.close();
        	socket.close();
        } catch(Exception e){
        	e.printStackTrace();
        }
	}

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + Integer.toString(this.serverPort), e);
        }
    } 
}