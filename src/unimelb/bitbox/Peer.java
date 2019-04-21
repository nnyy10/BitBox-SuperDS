package unimelb.bitbox;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
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

//    	
//
//    	System.setProperty("java.util.logging.SimpleFormatter.format",
//                "[%1$tc] %2$s %4$s: %5$s%n");
//        log.info("BitBox Peer starting...");
//        
//        log.info(System.getProperty("user.home"));
//        
//        new File(Paths.get(System.getProperty("user.home"), "BitBox").toString()).mkdirs();
//        
    	EntryPointServer server = new EntryPointServer(900);
    	new Thread(server).start();

    	//naiyun: "10.13.190.79"
    	//zhouxuan: "10.13.213.104"
    	//
    	
        Socket sc0 = new Socket("10.13.190.79",900);
        Client c0 = new Client(sc0);
        Thread tr0 = new Thread(c0);
        tr0.start();
    }
}
