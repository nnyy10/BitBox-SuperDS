package unimelb.bitbox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.EntryPointServer;
import unimelb.bitbox.Client;
import unimelb.bitbox.Server;


public class Peer 
{
	private static Logger log = Logger.getLogger(Peer.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {	
    	EntryPointServer server = new EntryPointServer(900);
    	new Thread(server).start();
    	

//    	System.setProperty("java.util.logging.SimpleFormatter.format",
//                "[%1$tc] %2$s %4$s: %5$s%n");
//        log.info("BitBox Peer starting...");
//        
//        log.info(System.getProperty("user.home"));
//        
//        new File(Paths.get(System.getProperty("user.home"), "BitBox").toString()).mkdirs();
//        
//        
//        Client c0 = new Client("10.13.190.79",900);
//        Thread tr0 = new Thread(c0);
//        tr0.start();


    }
}
