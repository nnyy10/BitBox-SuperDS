package unimelb.bitbox;

import java.io.IOException;

import java.net.Socket;

import java.security.NoSuchAlgorithmException;

import java.util.Scanner;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.EntryPointServer;
import unimelb.bitbox.Client;


public class Peer 
{
	private static Logger log = Logger.getLogger(Peer.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
    	
    	System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();
        
        String port_string = Configuration.getConfigurationValue("port").replaceAll("\\s+","");
        String all_peers = Configuration.getConfigurationValue("peers").replaceAll("\\s+","");
        String[] array_of_peers = all_peers.split(","); 

        int port = Integer.parseInt(port_string);
		
    	EntryPointServer server = new EntryPointServer(port);
    	new Thread(server).start();

        String[] peer_pair;
        Socket outGoingSocket = null;
        Client outGoingConnection = null;
        Thread connectionThread = null; 
        int triedPeerCnt = 0;
        for (String peer_string : array_of_peers) {
			peer_pair = peer_string.split(":");
			log.info("Trying to connect peer client to: " +peer_pair[0] + ":" + peer_pair[1]);
			try{
	    		outGoingSocket = new Socket(peer_pair[0], Integer.parseInt(peer_pair[1]));
	        	outGoingConnection = new Client(outGoingSocket);
	        	connectionThread = new Thread(outGoingConnection);
	        	connectionThread.start();
	        	log.info("Connected to "+"host: "+peer_pair[0]+"port: "+peer_pair[1]+"\n");
			}
			catch (Exception e){
				triedPeerCnt++;
				log.info("Can't connect to: " + peer_pair[0] + ":" + peer_pair[1]);
				log.info("Try connecting to another peer");
			}
        }
        
        if(triedPeerCnt == array_of_peers.length){
        	log.warning("Tried to connect to all peers in peer list in config file. Failed to connect to any of them.");
        	log.warning("Update your peer list in configuration.properties file and try again");
        }
    }
}
