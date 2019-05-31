package unimelb.bitbox;

import java.io.*;
import java.net.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.Configuration;

public class UDP_entry implements Runnable {

    private boolean isStopped = false;
    private Thread runningThread = null;
    private static Logger log = Logger.getLogger(PeerConnection.class.getName());
    private DatagramSocket ds;
    private DatagramPacket dp_receive = null;

    private ServerMain fileSystemObserver = null;
    protected ExecutorService threadPool = Executors.newFixedThreadPool(10);
    protected int hostPort=Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
    private String hostAddr=Configuration.getConfigurationValue("advertisedName");

    public UDP_entry(DatagramSocket ds) {
        this.ds = ds;
        fileSystemObserver = ServerMain.getInstance();
    }

    public void run() {
        log.info("Starting UDP server");

        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }

        int blocksize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
        while (!isStopped()) {
            try {
                byte[] buffer= new byte[blocksize];
                dp_receive = new DatagramPacket(buffer,buffer.length);
                this.ds.receive(dp_receive);

                Runnable r = () -> this.handlePacket(dp_receive);
                Thread thread = new Thread(r);
                thread.run();
//                Thread thread = new Thread(r);
//                Runnable r = () -> this.handlePacket(dp_receive);
//                this.threadPool.execute(r);

            } catch (IOException e) {
                log.warning(e.toString());
                this.stop();
            }
        }
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    private void handlePacket(DatagramPacket datagramPacket){
        String message;
        InetAddress receieveAddr;
        int receivePort;
        byte[] data= new byte[datagramPacket.getLength()];
        System.arraycopy(datagramPacket.getData(),datagramPacket.getOffset(),data,0,datagramPacket.getLength());
        receieveAddr = datagramPacket.getAddress();
        receivePort = datagramPacket.getPort();
        message = new String(data);
        log.info("UDP peer received message from host: " + receieveAddr.toString() + " port: " + receivePort + " msg:" + message);
        JSONParser parser = new JSONParser();
        JSONObject obj = null;
        try {
            obj = (JSONObject) parser.parse(message.trim());
        } catch (ParseException e) {
            log.warning(e.toString());
            this.stop();
        }
        String command = (String) obj.get("command");

        switch (command) {
            case "HANDSHAKE_REQUEST":
                String responseMsg = JSON_process.HANDSHAKE_RESPONSE(hostAddr, hostPort);
                UDP_peerconnection udpPeer = new UDP_peerconnection(ds, receieveAddr, receivePort);
                udpPeer.send(responseMsg);
                this.fileSystemObserver.add(udpPeer);
                break;
            case "HANDSHAKE_RESPONSE":
                for(UDP_peerconnection peer: UDP_peerconnection.waitingForHandshakeConnections){
                    if(peer.getPort() == receivePort && peer.getInetAddr().equals(receieveAddr)){
                        this.fileSystemObserver.add(peer);
                        UDP_peerconnection.RemovePeerToWaitingList(peer);
                        break;
                    }
                }
                break;
            default:
                for(PeerConnection peer: ServerMain.getInstance().getlist()){
                    UDP_peerconnection udpPeerConnection = (UDP_peerconnection) peer;
                    if(udpPeerConnection.getPort() == receivePort && udpPeerConnection.getInetAddr().equals(receieveAddr)){
                        udpPeerConnection.handleMessage(message);
                        break;
                    }
                }
                break;
        }
    }
    public synchronized void stop() {
        this.isStopped = true;
        this.ds.close();
    }
}