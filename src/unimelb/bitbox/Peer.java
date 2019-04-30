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

    	
    	System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();
        
        String port_string = Configuration.getConfigurationValue("port").replaceAll("\\s+","");
        String ip_addr = Configuration.getConfigurationValue("advertisedName").replaceAll("\\s+","");
        String all_peers = Configuration.getConfigurationValue("peers").replaceAll("\\s+","");
        String[] array_of_peers = all_peers.split(","); 
        
        
        String input;
        Scanner s = new Scanner(System.in);
        input = s.nextLine();

        boolean correct = false;
        while(!correct){
	        if(input.equals("s") || input.equals("S")) {
	        	correct = true;
		        int port = Integer.parseInt(port_string);
				System.out.println("starting server");
		    	EntryPointServer server = new EntryPointServer(port);
		    	new Thread(server).start();
	    	}
	        else if(input.equals("c") || input.equals("C")){
	        	correct = true;
		        String[] peer_pair;
		        Socket outGoingSocket = null;
		        Client outGoingConnection = null;
		        Thread connectionThread = null; 
		        for (String peer_string : array_of_peers) {
					System.out.println("starting client");
					peer_pair = peer_string.split(":");
					System.out.println(peer_pair[0]);
					System.out.println(peer_pair[1]);
		    		outGoingSocket = new Socket(peer_pair[0], Integer.parseInt(peer_pair[1]));
		        	outGoingConnection = new Client(outGoingSocket);
		        	connectionThread = new Thread(outGoingConnection);
		        	connectionThread.start();
		        }
	        }else{
	        	System.out.println("enter a valid input: s for server or c for client");
	        	input = s.nextLine();
	        }
        }
    }
}
