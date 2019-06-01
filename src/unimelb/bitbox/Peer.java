package unimelb.bitbox;

import java.io.IOException;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import java.security.NoSuchAlgorithmException;

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

        int clinetServerPort = Integer.parseInt(Configuration.getConfigurationValue("BBclientTCPport"));

        ClientServer clientServer = new ClientServer(clinetServerPort);



        if(mode.equals("tcp")) {
			String port_string = Configuration.getConfigurationValue("port").replaceAll("\\s+","");
			int port = Integer.parseInt(port_string);
			TCP_entry tcp_server = new TCP_entry(port);
			new Thread(tcp_server).start();

			String[] peer_pair;
			Socket outGoingSocket = null;
			TCP_Client outGoingConnection = null;
			Thread connectionThread = null;
			int triedPeerCnt = 0;
			//String host; int port;
			if(!(array_of_peers.length == 1 && array_of_peers[0].equals(""))) {
				for (String peer_string : array_of_peers) {
					peer_pair = peer_string.split(":");
					log.info("Trying to connect peer client to: " + peer_pair[0] + ":" + peer_pair[1]);
					try {
						outGoingSocket = new Socket(peer_pair[0], Integer.parseInt(peer_pair[1]));
						outGoingConnection = new TCP_Client(outGoingSocket);
						if(outGoingConnection.SendHandshake()) {
							connectionThread = new Thread(outGoingConnection);
							connectionThread.start();
							log.info("Connected to " + "host: " + peer_pair[0] + " port: " + peer_pair[1]);
						} else
							continue;
					} catch (Exception e) {
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
			clientServer.datagramSocket = ds;
			UDP_entry udp_server = new UDP_entry(ds);
			new Thread(udp_server).start();

			String[] peer_pair;

			for (String peer_string : array_of_peers) {
				peer_pair = peer_string.split(":");
				log.info("Trying to connect peer client to: " + peer_pair[0] + ":" + peer_pair[1]);
				UDP_peerconnection udpPeer = new UDP_peerconnection(ds, InetAddress.getByName(peer_pair[0]), Integer.parseInt(peer_pair[1]));
				udpPeer.sendHS();
			}
		}
		new Thread(clientServer).start();
    }
}
