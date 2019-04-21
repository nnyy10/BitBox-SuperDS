package unimelb.bitbox;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EntryPointServer implements Runnable{

    protected int serverPort = 0;
    protected ServerSocket serverSocket  = null;
    protected boolean isStopped     = false;
    protected Thread runningThread = null;
    private int ThreadCount = 0;
    
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
            	DataInputStream input = new DataInputStream(clientSocket.getInputStream());          	
            	DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
            	String s = input.readUTF();
            	if(s.equals("handshake")){
            		output.writeUTF("accepted");
            		System.out.println("accepted client");
            		this.threadPool.execute(new PeerConnection(clientSocket));
            	} else{
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