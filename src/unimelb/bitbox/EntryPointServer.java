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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class EntryPointServer implements Runnable{

    protected int serverPort = 0;
    protected ServerSocket serverSocket  = null;
    protected boolean isStopped     = false;
    protected Thread runningThread = null;
    private int ThreadCount = 0;
    
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
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            	BufferedReader input  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            	BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
            	readmesg = input.readLine();
            	System.out.println(readmesg);
            	
            //	JSON_process.HANDSHAKE_RESPONSE("localhost", 7000)
            	
            	
            	if(ThreadCount<=10) {
            		JSONParser parser = new JSONParser();
            		long port;
            		try {
                    JSONObject obj = (JSONObject) parser.parse(readmesg);
                    JSONObject hostPort = (JSONObject) obj.get("hostPort");
                    host = (String) hostPort.get("host");
                    port = (long) hostPort.get("port");
            		}catch (Exception e){
            			JSONObject obj = null;
            			e.printStackTrace();
            		}
            		ThreadCount++;
            		
            	} 	
            	else {
            		JSON_process.CONNECTION_REFUSED(clientSocket.getInetAddress(), clientSocket.getPort(), );
            	}
            	
            	
            	
           
        
            	if("str"==readmesg){
            		output.write("accepted"+"\n");
            		output.flush();
            		System.out.println("accepted client");
            		this.threadPool.execute(new Server(clientSocket));
            	} else {
            		System.out.println("rejected client");
            		input.close();
            		output.close();
            		clientSocket.close();
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