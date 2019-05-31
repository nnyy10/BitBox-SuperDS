package unimelb.bitbox;

import java.io.*;
import java.net.*;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.ServerMain;

public class UDP_entry implements Runnable {

    private boolean isStopped = false;
    private Thread runningThread = null;
    private static Logger log = Logger.getLogger(PeerConnection.class.getName());
    private CopyOnWriteArrayList<List<String>> remember;
    private DatagramSocket ds;
    private DatagramPacket dp_receive = null;
    private DatagramPacket dp_send = null;
    private String hostadd=Configuration.getConfigurationValue("advertisedName");
    private ServerMain fileSystemObserver = null;
    protected ExecutorService threadPool = Executors.newFixedThreadPool(10);
    protected  int hostPort=Integer.parseInt(Configuration.getConfigurationValue("udpPort"));

    public UDP_entry(DatagramSocket ds) {
        this.ds = ds;
        fileSystemObserver = ServerMain.getInstance();
    }

    public void run() {
        log.info("Starting UDP server");

        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }

        String message;
        InetAddress receieveAddr;
        int receivePort;
        int blocksize=Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
        while (!isStopped()) {
            try {
                byte[] buffer= new byte[8192];
                dp_receive = new DatagramPacket(buffer,buffer.length);
                this.ds.receive(dp_receive);
                byte[] data= new  byte[dp_receive.getLength()];
                System.arraycopy(dp_receive.getData(),dp_receive.getOffset(),data,0,dp_receive.getLength());
                receieveAddr = dp_receive.getAddress();
                receivePort = dp_receive.getPort();
                message = new String(data);
                System.out.println("message:"+message);

                JSONParser parser = new JSONParser();
                JSONObject obj = (JSONObject) parser.parse(message.trim());
                String command = (String) obj.get("command");

                switch (command) {
                    case "HANDSHAKE_REQUEST":
                        String responseMsg = JSON_process.HANDSHAKE_RESPONSE(hostadd, hostPort);
                        UDP_peerconnection udpPeer = new UDP_peerconnection(ds, receieveAddr, receivePort);
                        udpPeer.send(responseMsg);
                        this.fileSystemObserver.add(udpPeer);
                        log.info("UDP hs request received, response sent");
                        break;
                    case "HANDSHAKE_RESPONSE":
                        log.info("UDP hs request received, response sent");
                        for(UDP_peerconnection peer: UDP_peerconnection.waitingForHandshakeConnections){
                            if(peer.getPort() == receivePort && peer.getAddr() == receieveAddr){
                                this.fileSystemObserver.add(peer);
                                UDP_peerconnection.RemovePeerToWaitingList(peer);
                                break;
                            }
                        }
                        break;
                    default:
                        log.info("other message receieved");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
//            ds_receive = null;
//            try {
//                getdsocket();
//                int byte_size=Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
//                byte[] buf = new byte[byte_size];
//                dp_receive = new DatagramPacket(buf, byte_size);
//                readmesg = new String(dp_receive.getData());
//                dp_receive.setLength(byte_size);
//                InetAddress iAddress = dp_receive.getAddress();
//                try {
//                    remember.add(Arrays.asList(readmesg, String.valueOf(udpport) ,iAddress.toString()));
//                    JSONParser parser = new JSONParser();
//                    JSONObject jsonMsg = (JSONObject) parser.parse(readmesg);
//                    String jsonCommand = (String) jsonMsg.get("command");
//                    JSONObject jsonHostPort = (JSONObject) jsonMsg.get("hostPort");
//                    String jsonHost = (String) jsonHostPort.get("host");
//                    String jsonPort = (String) jsonHostPort.get("host");
//
//                    if (jsonHost == null || jsonPort == null || jsonCommand == null || !jsonCommand.equals("HANDSHAKE_REQUEST")) {
//                        String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL("HANDSHAKE_REQUEST invalid");
//                        this.send_HS(iAddress,invalidProtocolMsg);
//                        this.CloseConnection(readmesg, udpport, iAddress);
//                        log.warning("UDP Handshake request invalid, closing socket.");
//                        continue;
//                    }
//                } catch (Exception e) {
//                    String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL("HANDSHAKE_REQUEST invalid");
//                    this.send_HS(iAddress,invalidProtocolMsg);
//                    this.CloseConnection(readmesg, udpport, iAddress);
//                    continue;
//                }
//
//                if (UDP_server.numberOfConnections < 10) {
//                    String handshakeReponseMsg = JSON_process.HANDSHAKE_RESPONSE(dp_receive.toString(), dp_receive.getPort());
//                    try {
//                        this.send_HS(iAddress,handshakeReponseMsg);
//                        log.info("UDP_Client accepted, sending response message: " + handshakeReponseMsg);
//                        this.threadPool.execute(new UDP_server(ds_receive));
//                    } catch (Exception e) {
//                        log.warning("UDP_Client accepted but error sending handshake response, closing connection");
//                        this.CloseConnection(readmesg, udpport, iAddress);
//                        continue;
//                    }
//                } else {
//                    //number more than 10.
//                    log.warning("Max connection of " + UDP_server.numberOfConnections + " limit reached. Sending connection refused message.");
//    connection refused
//                    ArrayList<PeerConnection> connections = ServerMain.getInstance().getlist();
//                    String[] tempIPlist = new String[connections.size()];
//                    int[] tempPrlist = new int[connections.size()];
//
//                    for (int i = 0; i < ServerMain.getInstance().getlist().size(); i++) {
//                        tempIPlist[i] = (connections.get(i).socket.getRemoteSocketAddress().toString());
//                        tempPrlist[i] = (connections.get(i).socket.getPort());
//                    }
//
//                    String connection_refused =JSON_process.CONNECTION_REFUSED(tempIPlist, tempPrlist);
//                    send_HS(iAddress,connection_refused);
//                    log.warning(" UDP connection refused.");
//
//                }
//            } catch (IOException e) {
//                if (isStopped()) {
//                    log.info("UDP_Server Stopped.");
//                    break;
//                }
//                throw new RuntimeException("Error accepting client connection", e);
//            }
//        }
//        this.threadPool.shutdown();
//        log.info("UDP_Server Stopped.");
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }
    public synchronized void stop() {
        this.isStopped = true;
        this.ds.close();
    }

}