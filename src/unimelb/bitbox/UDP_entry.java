package unimelb.bitbox;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import unimelb.bitbox.util.Configuration;

public class UDP_entry implements Runnable {

    protected boolean isStopped = false;
    protected Thread runningThread = null;
    private static Logger log = Logger.getLogger(PeerConnection.class.getName());
    private CopyOnWriteArrayList<List<String>> remember;

    DatagramPacket dp_receive = null;
    DatagramPacket dp_sent = null;
    DatagramSocket ds_receive = null;
    DatagramSocket ds_sned = null;
    String Smesg, readmesg, host;
    long udpport;

    protected ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public UDP_entry(int port) {
//????port
        this.udpport = port ;
    }

    public void run() {
        log.info("Starting UDP server");

        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }

        while (!isStopped()) {
            ds_receive = null;
            try {
                getdsocket();
                int byte_size=Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
                byte[] buf = new byte[byte_size];
                dp_receive = new DatagramPacket(buf, byte_size);
                readmesg = new String(dp_receive.getData());
                dp_receive.setLength(byte_size);
                InetAddress iAddress = dp_receive.getAddress();
                try {
                    remember.add(Arrays.asList(readmesg, String.valueOf(udpport) ,iAddress.toString()));
                    JSONParser parser = new JSONParser();
                    JSONObject jsonMsg = (JSONObject) parser.parse(readmesg);
                    String jsonCommand = (String) jsonMsg.get("command");
                    JSONObject jsonHostPort = (JSONObject) jsonMsg.get("hostPort");
                    String jsonHost = (String) jsonHostPort.get("host");
                    String jsonPort = (String) jsonHostPort.get("host");

                    if (jsonHost == null || jsonPort == null || jsonCommand == null || !jsonCommand.equals("HANDSHAKE_REQUEST")) {
                        String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL("HANDSHAKE_REQUEST invalid");
                        this.send_HS(iAddress,invalidProtocolMsg);
                        this.CloseConnection(readmesg, udpport, iAddress);
                        log.warning("UDP Handshake request invalid, closing socket.");
                        continue;
                    }
                } catch (Exception e) {
                    String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL("HANDSHAKE_REQUEST invalid");
                    this.send_HS(iAddress,invalidProtocolMsg);
                    this.CloseConnection(readmesg, udpport, iAddress);
                    continue;
                }

                if (UDP_server.numberOfConnections < 10) {
                    String handshakeReponseMsg = JSON_process.HANDSHAKE_RESPONSE(dp_receive.toString(), dp_receive.getPort());
                    try {
                        this.send_HS(iAddress,handshakeReponseMsg);
                        log.info("UDP_Client accepted, sending response message: " + handshakeReponseMsg);
                        this.threadPool.execute(new UDP_server(ds_receive));
                    } catch (Exception e) {
                        log.warning("UDP_Client accepted but error sending handshake response, closing connection");
                        this.CloseConnection(readmesg, udpport, iAddress);
                        continue;
                    }
                } else {
                    //number more than 10.
                    log.warning("Max connection of " + UDP_server.numberOfConnections + " limit reached. Sending connection refused message.");
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

                }
            } catch (IOException e) {
                if (isStopped()) {
                    log.info("UDP_Server Stopped.");
                    break;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
        }
        this.threadPool.shutdown();
        log.info("UDP_Server Stopped.");
    }


    protected void CloseConnection( String readmesg, long udpport, InetAddress iAddress ) {
        log.info("Closing New Socket Connection");
        List<String> delete = new ArrayList<>(Arrays.asList(readmesg, String.valueOf(udpport) ,iAddress.toString()));
        this.remember.remove(delete);
        dp_receive = null;
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop() {
        this.isStopped = true;
        this.ds_receive.close();
    }


    private void getdsocket() {
        try {
            this.ds_receive = new DatagramSocket(Integer.parseInt(this.host));
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.host, e);
        }
    }

    private void send_HS(InetAddress iAddress, String JSON_msg) throws IOException {
        byte[] mes = JSON_msg.getBytes("utf-8");
        dp_sent=new DatagramPacket(mes, mes.length, iAddress, (int)udpport);
        ds_sned.send(dp_sent);
        log.warning("UDP Handshake request invalid, closing socket.");
    }

}