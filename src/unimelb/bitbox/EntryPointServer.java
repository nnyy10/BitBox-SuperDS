package unimelb.bitbox;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class EntryPointServer implements Runnable{

    protected int serverPort = 0;
    protected ServerSocket serverSocket = null;
    protected boolean isStopped = false;
    protected Thread runningThread = null;
    private static Logger log = Logger.getLogger(PeerConnection.class.getName());
    
    Socket tempServerSocket = null;
    
    String Smesg,readmesg,host; long port;
    
    protected ExecutorService threadPool =Executors.newFixedThreadPool(10);

    public EntryPointServer(int port){
        this.serverPort = port;
    }

    public void run(){
    	log.info("Starting TCP server");
    	
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }

        openServerSocket();
        while(! isStopped()){
            tempServerSocket = null;
            try {
            	tempServerSocket = this.serverSocket.accept();
            	BufferedReader inputStream  = new BufferedReader(new InputStreamReader(tempServerSocket.getInputStream(), "UTF-8"));
            	BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(tempServerSocket.getOutputStream(), "UTF-8"));
            	readmesg = inputStream.readLine();
            	
            	try {
            		JSONParser parser = new JSONParser();
            		JSONObject jsonMsg = (JSONObject) parser.parse(readmesg);
                    String jsonCommand = (String) jsonMsg.get("command");
                    JSONObject jsonHostPort = (JSONObject) jsonMsg.get("hostPort");
                    String jsonHost = (String) jsonHostPort.get("host");
                    String jsonPort = (String) jsonHostPort.get("host");
                    if(jsonHost == null || jsonPort==null || jsonCommand==null|| !jsonCommand.equals("HANDSHAKE_REQUEST")) {
                    	String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL("HANDSHAKE_REQUEST invalid");
                    	outputStream.write(invalidProtocolMsg+"\n");
                    	outputStream.flush();
                    	this.CloseConnection(inputStream, outputStream, tempServerSocket);
                    	log.warning("Handshake request invalid, closing socket.");
                    	continue;
                    }
                    
            	}catch (Exception e){
            		String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL("HANDSHAKE_REQUEST invalid");
                	outputStream.write(invalidProtocolMsg+"\n");
                	this.CloseConnection(inputStream, outputStream, tempServerSocket);
                	log.warning("Handshake request invalid, closing socket.");
                	continue;
                }
            
<<<<<<< Updated upstream
            	if(PeerServer.numberOfConnections<10) {
=======
            	if(TCP_Server.numberOfConnections<10) {
>>>>>>> Stashed changes
                    String handshakeReponseMsg=JSON_process.HANDSHAKE_RESPONSE(tempServerSocket.toString(), tempServerSocket.getPort());
                    try{
                		outputStream.write(handshakeReponseMsg+"\n");
                		outputStream.flush();
<<<<<<< Updated upstream
                		log.info("Client accepted, sending response message: " + handshakeReponseMsg);
                		this.threadPool.execute(new PeerServer(tempServerSocket));
=======
                		log.info("TCP_Client accepted, sending response message: " + handshakeReponseMsg);
                		this.threadPool.execute(new TCP_Server(tempServerSocket));
>>>>>>> Stashed changes
                	} catch(Exception e){
                		log.warning("TCP_Client accepted but error sending handshake response, closing connection");
                		this.CloseConnection(inputStream, outputStream, tempServerSocket);
                		continue;
            		}
            	} 	
            	else {
<<<<<<< Updated upstream
            		log.warning("Max connection of " + PeerServer.numberOfConnections+ " limit reached. Sending connection refused message.");
=======
            		log.warning("Max connection of " + TCP_Server.numberOfConnections+ " limit reached. Sending connection refused message.");
>>>>>>> Stashed changes
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
                	log.info("TCP_Server Stopped.") ;
                    break;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
        }
        this.threadPool.shutdown();
        log.info("TCP_Server Stopped.") ;
    }

	protected void CloseConnection(BufferedReader inputStream, BufferedWriter outputStream, Socket socket){
		log.info("Closing New Socket Connection");
		
		try{
        	inputStream.close();
		} catch(Exception e){}
		try{
			outputStream.close();
		} catch(Exception e){}
		try{
			socket.close();
		} catch(Exception e){}
		tempServerSocket = null;
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