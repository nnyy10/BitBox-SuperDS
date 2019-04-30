package unimelb.bitbox;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.SocketHandler;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.EntryPointServer;
import unimelb.bitbox.Client;
import unimelb.bitbox.PeerConnection;


public class Peer 
{
	private static Logger log = Logger.getLogger(Peer.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {	

    	Scanner sc = new Scanner(System.in);
    	String s = sc.nextLine();
    	
    	if(s.equals("client")||s.equals("client")){
    		try{
    			System.out.println("starting client");
	    		Socket sc0 = new Socket("localhost",7000);
	        	Client c0 = new Client(sc0);
	        	Thread tr0 = new Thread(c0);
	        	tr0.start();
    		} catch(Exception e){
    			e.printStackTrace();
    		}
    	}else if(s.equals("server")||s.equals("Server")){
    		System.out.println("starting server");
        	EntryPointServer server = new EntryPointServer(7000);
        	new Thread(server).start();
    	}
    }
}
