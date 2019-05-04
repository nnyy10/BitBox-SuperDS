package unimelb.bitbox;

//A Java program for a Server 
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import unimelb.bitbox.ServerMain;
import unimelb.bitbox.PeerConnection;
import unimelb.bitbox.JSON_process;
import unimelb.bitbox.util.HostPort;


public class Client extends PeerConnection implements Runnable {
	
	private static Logger log = Logger.getLogger(Client.class.getName());
	
	public Client(Socket socket) {
		super(socket);
		
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
				Client outGoingConnection = null;
				Thread connectionThread = null;
				switch(jsonCommand){
				case "HANDSHAKE_RESPONSE":
					break;
				case "CONNECTION_REFUSED":
					JSONObject obj;
					peers = (JSONArray) jsonMsg.get("peers");
					for(int i = 0; i< peers.size();i++){
						obj = (JSONObject) peers.get(i);
						host = (String) obj.get("host");
						port = (int) obj.get("port");
						outGoingSocket = new Socket(host, port);
						log.info("Trying to connect peer client to: " +host + ":" + port);
						try{
							outGoingConnection = new Client(outGoingSocket);
							connectionThread = new Thread(outGoingConnection);
							connectionThread.start();
							log.info("Reconnected to: "+"host: "+host+"port: "+port+"\n");
							break;
						}
						catch (Exception e){
							log.info("Can't connect to: " + host + ":" + port);
							log.info("Try connecting to another peer");
						}
					}
				default:
            		log.info("Handshake response invalid, closing socket.");
            		this.CloseConnection();
            		return;
				}
			}
			catch (Exception e){
				String message = "Handshake response invalid, closing connection.";
				String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL(message);
        		log.info(message);
        		outputStream.write(invalidProtocolMsg+"\n");
        		outputStream.flush();
            	this.CloseConnection();
            	return;
            }
		} catch (IOException e) {
			log.info("Connection FAILED.");
			this.CloseConnection();
			return;
		}
		
		this.fileSystemObserver.add(this);
		
		log.info("client successfully connected to " + socket.getRemoteSocketAddress().toString());
	}
	
	protected void finalize() throws Throwable {
		this.CloseConnection();
	}
}
