package unimelb.bitbox;

import java.io.IOException;

import java.net.DatagramSocket;
import java.net.Socket;

import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;


public class Peer 
{
	private static Logger log = Logger.getLogger(Peer.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
    	
    	System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();

        String mode=Configuration.getConfigurationValue("mode");

        String all_peers = Configuration.getConfigurationValue("peers").replaceAll("\\s+","");
        String[] array_of_peers = all_peers.split(",");

        if(mode.equals("tcp")) {
			String port_string = Configuration.getConfigurationValue("port").replaceAll("\\s+","");
			int port = Integer.parseInt(port_string);
			EntryPointServer tcp_server = new EntryPointServer(port);
			new Thread(tcp_server).start();

			String[] peer_pair;
			Socket outGoingSocket = null;
			TCP_Client outGoingConnection = null;
			Thread connectionThread = null;
			int triedPeerCnt = 0;
			ArrayList<PeerConnection> connect = ServerMain.getInstance().getlist();
			//String host; int port;

			for (String peer_string : array_of_peers) {
				peer_pair = peer_string.split(":");
				log.info("Trying to connect peer client to: " +peer_pair[0] + ":" + peer_pair[1]);
				if(connect.size()!= 0){
					for(int i = 0; i< connect.size(); i++){
						if(!peer_pair[0].equals(((TCP_peerconnection)connect.get(i)).socket.getRemoteSocketAddress().toString())){
							try{
								outGoingSocket = new Socket(peer_pair[0], Integer.parseInt(peer_pair[1]));
								outGoingConnection = new TCP_Client(outGoingSocket);
								connectionThread = new Thread(outGoingConnection);
								connectionThread.start();
								log.info("Connected to "+"host: "+peer_pair[0]+" port: "+peer_pair[1]);
							}
							catch (Exception e){
								triedPeerCnt++;
								log.info("Can't connect to: " + peer_pair[0] + ":" + peer_pair[1]);
								log.info("Try connecting to another peer");
							}
						}
						else log.info("Already connected to " + "host: "+ peer_pair[0] + " port: " + peer_pair[1]+ ":) ");
					}
				} else{
					try{
						outGoingSocket = new Socket(peer_pair[0], Integer.parseInt(peer_pair[1]));
						outGoingConnection = new TCP_Client(outGoingSocket);
						connectionThread = new Thread(outGoingConnection);
						connectionThread.start();
						log.info("Connected to "+"host: "+peer_pair[0]+" port: "+peer_pair[1]);
					}
					catch (Exception e){
						triedPeerCnt++;
						log.info("Can't connect to: " + peer_pair[0] + ":" + peer_pair[1]);
						log.info("Try connecting to another peer");
					}
				}

			}

			if(triedPeerCnt == array_of_peers.length){
				log.warning("Tried to connect to all peers in peer list in config file. Failed to connect to any of them.");
				log.warning("Update your peer list in configuration.properties file and try again");
			}
		}
//-------------------UDP--------------------------------------------------------------------------------------------

		else {
			String port_string = Configuration.getConfigurationValue("udpPort").replaceAll("\\s+","");
			int port = Integer.parseInt(port_string);

			DatagramSocket ds = new DatagramSocket(port);

			UDP_entry udp_server = new UDP_entry(ds);
			new Thread(udp_server).start();

			String[] peer_pair;

			for (String peer_string : array_of_peers) {
				peer_pair = peer_string.split(":");
				log.info("Trying to connect peer client to: " + peer_pair[0] + ":" + peer_pair[1]);
				UDP_peerconnection udpPeer = new UDP_peerconnection(ds, peer_pair[0], Integer.parseInt(peer_pair[1]));
				udpPeer.sendHS();
				log.info("UDP handshake sent to " + "host: " + peer_pair[0] + " port: " + peer_pair[1]);
			}
		}
    }
}
