package unimelb.bitbox;

//A Java program for a Server 
import java.net.*;
import java.security.NoSuchAlgorithmException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import unimelb.bitbox.ServerMain;
import unimelb.bitbox.PeerConnection;
import unimelb.bitbox.JSON_process;
import unimelb.bitbox.util.HostPort;


public class Client extends PeerConnection implements Runnable {
	public Client(Socket socket) {
		super(socket);
		this.fileSystemObserver.add(this);
		
		
		try {

			String Cmesg = JSON_process.HANDSHAKE_REQUEST(this.socket.getLocalAddress().toString(), this.socket.getLocalPort());
			outputStream.write(Cmesg+"\n");
			outputStream.flush();
			System.out.println("Client sent: " + Cmesg);
		
			String temp = inputStream.readLine();
			System.out.println("Client recieved:"+temp);
			try {
				JSONParser parser = new JSONParser();
				JSONObject jsonMsg = (JSONObject) parser.parse(temp);
				String jsonCommand = (String) jsonMsg.get("command");
				JSONArray peers = new JSONArray();
				int port;
				String host;
				Socket outGoingSocket = null;
				Client outGoingConnection = null;
				Thread connectionThread = null;
				switch(jsonCommand){
					case "INVALID_PROTOCOL":
						//JSONObject jsonHostPort = (JSONObject) jsonMsg.get("hostPort");
						//String jsonHost = (String) jsonHostPort.get("host");
						//String jsonPort = (String) jsonHostPort.get("host");
                		String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL();
                		outputStream.write(invalidProtocolMsg+"\n");
                		outputStream.flush();
                		System.out.println("Handshake response invalid, closing socket.");
                		socket.shutdownInput();
					case "CONNECTION_REFUSED":
						JSONObject obj = new JSONObject();
						peers = (JSONArray) jsonMsg.get("peers");
						for(int i = 0; i< peers.size();i++){
							obj = (JSONObject) peers.get(i);
							host = (String) obj.get("host");
							port = (int) obj.get("port");
							//HostPort newConnect = new HostPort(host, port);
							outGoingSocket = new Socket(host, port);
							try{
								outGoingConnection = new Client(outGoingSocket);
								connectionThread = new Thread(outGoingConnection);
								connectionThread.start();
								System.out.println("reconnect!!! "+"host: "+host+"port: "+port+"\n");
								break;
							}
							catch (Exception e){}
						}




				}

			}
			catch (Exception e){
				String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL();
        		outputStream.write(invalidProtocolMsg+"\n");
            	System.out.println("Handshake response invalid, closing socket.");
            	socket.shutdownInput();
            }
		} catch (IOException e) {
			System.out.println("Connection FAILED.");
			this.CloseConnection();
		}
		System.out.println("client successfully connected to " + socket.getRemoteSocketAddress().toString());
	}
	
	protected void finalize() throws Throwable {
		this.CloseConnection();
	}
	
	/*protected void CloseConnection(BufferedReader inputStream, BufferedWriter outputStream, Socket socket){
		System.out.println("Closing New Socket Connection");
		try{
        	inputStream.close();
        	outputStream.close();
        	socket.close();
        } catch(Exception e){
        	e.printStackTrace();
        }
	}*/

}
