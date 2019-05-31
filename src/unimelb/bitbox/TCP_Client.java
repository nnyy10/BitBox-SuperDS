package unimelb.bitbox;


//A Java program for a TCP_Server
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;


public class TCP_Client extends TCP_peerconnection implements Runnable {
	
	private static Logger log = Logger.getLogger(TCP_Client.class.getName());
	
	public TCP_Client(Socket socket) {
		super(socket);
	}

	public boolean SendHandshake(){
		try {
			String Cmesg = JSON_process.HANDSHAKE_REQUEST(this.socket.getLocalAddress().toString(), this.socket.getLocalPort());
			outputStream.write(Cmesg+"\n");
			outputStream.flush();
		
			String temp = inputStream.readLine();
			try {
				JSONParser parser = new JSONParser();
				JSONObject jsonMsg = (JSONObject) parser.parse(temp);
				String jsonCommand = (String) jsonMsg.get("command");
				JSONArray peers;
				int port;
				String host;
				Socket outGoingSocket = null;
				TCP_Client outGoingConnection = null;
				Thread connectionThread = null;

				switch(jsonCommand){
				case "HANDSHAKE_RESPONSE":
					this.fileSystemObserver.add(this);
					log.info("client successfully connected to " + socket.getRemoteSocketAddress().toString());
					return true;
				case "CONNECTION_REFUSED":
					JSONObject obj;
					peers = (JSONArray) jsonMsg.get("peers");
					ArrayList<PeerConnection> connect = ServerMain.getInstance().getlist();
					for(int i = 0; i< peers.size();i++){
						obj = (JSONObject) peers.get(i);
						host = (String) obj.get("host");
						port = (int) obj.get("port");
						outGoingSocket = new Socket(host, port);
						for (int j = 0; j< connect.size(); j++) {
							if(!host.equals(((TCP_peerconnection)connect.get(j)).socket.getRemoteSocketAddress().toString())){
								log.info("Trying to connect peer client to: " + host + ":" + port);
								try {
									outGoingConnection = new TCP_Client(outGoingSocket);
									if(outGoingConnection.SendHandshake()) {
										connectionThread = new Thread(outGoingConnection);
										connectionThread.start();
										log.info("Reconnected to: " + "host: " + host + "port: " + port);
									}
									break;
								} catch (Exception e) {
									log.info("Can't connect to: " + host + ":" + port);
									log.info("Try connecting to another peer");
								}
							}
							else log.info("Already connected to " + "host: "+ host + "port: " +port + ":) ");
						}
					}
				default:
            		log.info("Handshake response invalid, closing socket.");
            		this.CloseConnection();
            		return false;
				}
			}
			catch (Exception e){
				String message = "Handshake response invalid, closing connection.";
				String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL(message);
        		log.info(message);
        		outputStream.write(invalidProtocolMsg+"\n");
        		outputStream.flush();
            	this.CloseConnection();
            	return false;
            }
		} catch (IOException e) {
			log.info("Connection FAILED.");
			this.CloseConnection();
			return false;
		}
	}
}
