package unimelb.bitbox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.Server;
import unimelb.bitbox.Client;


public class Peer 
{
	private static Logger log = Logger.getLogger(Peer.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {	
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        
        log.info(System.getProperty("user.home"));
        
        new File(Paths.get(System.getProperty("user.home"), "BitBox").toString()).mkdirs();
        
//        Configuration.getConfiguration();
//        new ServerMain();
        
        Server s = new Server(80);
        Thread trs = new Thread(s);
        trs.start();
        
        Client c0 = new Client("10.12.189.122", 80);
        Thread tr0 = new Thread(c0);
        tr0.start();
        


    }
    
    
//       
//        		
//        		
//        s.listen(this.listen())
//        
//        
//        FileSystemObserver.listen_local()
//        
//    }
//    
//    private void listen_remote(msg){
//    	if(msg.eventType == msgType.delete){
//    		this.handleDelete();
//    	} 
//    	else if
//    }
//    
//    private void handleDelete(){
//    	FileSystemManager.delete(msg.file.name)
//    }
//    
//    private void handleCreate(){
//    	
//    }
    
}
