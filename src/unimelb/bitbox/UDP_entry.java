package unimelb.bitbox;

import java.io.*;
import java.net.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

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

    protected ScheduledExecutorService exec = null;

    public void synchronouspeers() {
        int synTime = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));


        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
            for (FileSystemManager.FileSystemEvent event : this.fileSystemObserver.fileSystemManager.generateSyncEvents()) {
                for(PeerConnection peerconnection:ServerMain.getInstance().getlist())
                {
                    String syn;
                    syn = ServerMain.getInstance().FileSystemEventToJSON(event);
                    peerconnection.send(syn);
                }
            }
        }, 0, synTime, TimeUnit.SECONDS);
    }

    public UDP_entry(DatagramSocket ds) {
        this.ds = ds;
        fileSystemObserver = ServerMain.getInstance();
    }

    public void run() {
        log.info("Starting UDP server");
        synchronouspeers();

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
                new Thread(()-> {
                    for (FileSystemManager.FileSystemEvent event : this.fileSystemObserver.fileSystemManager.generateSyncEvents()) {
                        String syn;
                        syn = ServerMain.getInstance().FileSystemEventToJSON(event);
                        udpPeer.send(syn);
                    }
                }).run();
                break;
            case "HANDSHAKE_RESPONSE":
                for(UDP_peerconnection peer: UDP_peerconnection.waitingForHandshakeConnections){
                    if(peer.getPort() == receivePort && peer.getInetAddr().equals(receieveAddr)){
                        this.fileSystemObserver.add(peer);
                        UDP_peerconnection.RemovePeerToWaitingList(peer);
                        new Thread(()-> {
                            for (FileSystemManager.FileSystemEvent event : this.fileSystemObserver.fileSystemManager.generateSyncEvents()) {
                                String syn;
                                syn = ServerMain.getInstance().FileSystemEventToJSON(event);
                                peer.send(syn);
                            }
                        }).run();
                        break;
                    }
                }
                break;
            default:
                for(PeerConnection peer: ServerMain.getInstance().getlist()){
                    UDP_peerconnection udpPeerConnection = (UDP_peerconnection) peer;
                    if(udpPeerConnection.getPort() == receivePort && udpPeerConnection.getInetAddr().equals(receieveAddr)){
                        if(UDP_peerconnection.isResponseMessage(message)){
                            for(UDP_peerconnection.ThreadResponsePair trp: UDP_peerconnection.waitingForResponseThreads){
                                if(trp.addr.equals(receieveAddr) && trp.port == receivePort && JSON_process.RESPONSE_EQUALS(trp.JSON_Response, message)){
                                    trp.timer.cancel();
                                    break;
                                }
                            }
                        }
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