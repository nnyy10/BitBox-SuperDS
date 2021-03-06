package unimelb.bitbox;

import java.io.*;
import java.net.*;

import java.util.concurrent.*;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

public class UDP_entry implements Runnable {

    private boolean isStopped = false;
    private Thread runningThread = null;
    private static Logger log = Logger.getLogger(PeerConnection.class.getName());
    public DatagramSocket ds;
    private DatagramPacket dp_receive = null;

    private int maxConnection = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));

    private ServerMain fileSystemObserver = null;
    protected ExecutorService threadPool = Executors.newFixedThreadPool(10);
    protected int hostPort = Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
    private String hostAddr = Configuration.getConfigurationValue("advertisedName");

    protected ScheduledExecutorService exec = null;

    public void synchronouspeers() {
        int synTime = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));


        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
            for (FileSystemManager.FileSystemEvent event : this.fileSystemObserver.fileSystemManager.generateSyncEvents()) {
                for (PeerConnection peerconnection : ServerMain.getInstance().getlist()) {
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
                byte[] buffer = new byte[blocksize];
                dp_receive = new DatagramPacket(buffer, buffer.length);
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

    private void handlePacket(DatagramPacket datagramPacket) {
        String message;
        InetAddress receieveAddr;
        int receivePort;
        byte[] data = new byte[datagramPacket.getLength()];
        System.arraycopy(datagramPacket.getData(), datagramPacket.getOffset(), data, 0, datagramPacket.getLength());
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
                if(ServerMain.getInstance().getlist().size() < maxConnection) {
                    String responseMsg = JSON_process.HANDSHAKE_RESPONSE(hostAddr, hostPort);
                    UDP_peerconnection udpPeer = new UDP_peerconnection(ds, receieveAddr, receivePort);
                    udpPeer.send(responseMsg); // send handshake response
                    this.fileSystemObserver.add(udpPeer); // add the new peer connection to all connected peer list
                    // Sync 2 peers after connection establishment
                    for (FileSystemManager.FileSystemEvent event : this.fileSystemObserver.fileSystemManager.generateSyncEvents()) {
                        String syncMessage = ServerMain.getInstance().FileSystemEventToJSON(event);
                        udpPeer.send(syncMessage);
                    }
                } else{
                    CopyOnWriteArrayList<PeerConnection> connections = ServerMain.getInstance().getlist();
                    String listPeerResponse = "";
                    if (connections.size()!=0) {
                        String[] tempIPlist = new String[connections.size()];
                        int[] tempPrlist = new int[connections.size()];
                        for (int i = 0; i < connections.size(); i++) {
                            tempIPlist[i] = connections.get(i).getAddr();
                            tempPrlist[i] = connections.get(i).getPort();
                        }
                        listPeerResponse = JSON_process.CONNECTION_REFUSED(tempIPlist, tempPrlist);
                    }else
                        listPeerResponse = JSON_process.CONNECTION_REFUSED(null, null);
                    try {
                        byte[] mes = listPeerResponse.getBytes("utf-8");
                        DatagramPacket dp_send = new DatagramPacket(mes, mes.length, receieveAddr, receivePort);
                        ds.send(dp_send);
                    }catch (Exception e){
                        log.warning(e.toString());
                    }
                }
                break;
            case "HANDSHAKE_RESPONSE":
                UDP_peerconnection.ThreadResponsePair threadResponsePair = null;
                for (UDP_peerconnection peer : UDP_peerconnection.waitingForHandshakeConnections) {
                    if (peer.getPort() == receivePort && peer.getInetAddr().equals(receieveAddr)) {
                        for (UDP_peerconnection.ThreadResponsePair trp : UDP_peerconnection.waitingForResponseThreads) {
                            System.out.println(trp.addr + ":" + receieveAddr);
                            System.out.println(trp.port + ":" + receivePort);
                            System.out.println(JSON_process.RESPONSE_EQUALS(trp.JSON_Response, message));
                            if (trp.addr.equals(receieveAddr) && trp.port == receivePort && JSON_process.RESPONSE_EQUALS(trp.JSON_Response, message)) {
                                trp.timer.cancel(); // If there is an active timer thread waiting for this response, stop this timer thread
                                threadResponsePair = trp;
                                break;
                            }
                        }
                        this.fileSystemObserver.add(peer);
                        UDP_peerconnection.RemovePeerToWaitingList(peer);
                        new Thread(() -> {
                            for (FileSystemManager.FileSystemEvent event : this.fileSystemObserver.fileSystemManager.generateSyncEvents()) {
                                String syn;
                                syn = ServerMain.getInstance().FileSystemEventToJSON(event);
                                peer.send(syn);
                            }
                        }).run();
                        break;
                    }
                }
                if (threadResponsePair != null){
                    UDP_peerconnection.waitingForResponseThreads.remove(threadResponsePair);
                }
                break;
            case "CONNECTION_REFUSED":
                try {
                    JSONArray peers = (JSONArray) obj.get("peers");
                    CopyOnWriteArrayList<PeerConnection> connect = ServerMain.getInstance().getlist();
                    for (int i = 0; i < peers.size(); i++) {
                        obj = (JSONObject) peers.get(i);
                        String host = (String) obj.get("host");
                        int port = (int) obj.get("port");
                        for (int j = 0; j < connect.size(); j++) {
                            if (!host.equals(connect.get(j).getAddr()) && port != connect.get(j).getPort()) {
                                UDP_peerconnection udpPeer = new UDP_peerconnection(ds, InetAddress.getByName(host), port);
                                udpPeer.sendHS();
                            }
                        }
                    }
                } catch (Exception e){
                    log.warning("Handling connection refused failed");
                    log.warning(e.toString());
                }
                break;
            default:
                System.out.println(ServerMain.getInstance().getlist().size());
                for (PeerConnection peer : ServerMain.getInstance().getlist()) {
                    UDP_peerconnection udpPeerConnection = (UDP_peerconnection) peer;
                    if (udpPeerConnection.getPort() == receivePort && udpPeerConnection.getInetAddr().equals(receieveAddr)) {
                        if (UDP_peerconnection.isResponseMessage(message)) {
                            UDP_peerconnection.ThreadResponsePair foundThreadPair = null;
                            for (UDP_peerconnection.ThreadResponsePair trp : UDP_peerconnection.waitingForResponseThreads) {
                                System.out.println(trp.addr + ":" + receieveAddr);
                                System.out.println(trp.port + ":" + receivePort);
                                System.out.println(trp.JSON_Response);
                                System.out.println(message);
                                System.out.println(JSON_process.RESPONSE_EQUALS(trp.JSON_Response, message));
                                if (trp.addr.equals(receieveAddr) && trp.port == receivePort && JSON_process.RESPONSE_EQUALS(trp.JSON_Response, message)) {
                                    trp.timer.cancel(); // If there is an active timer thread waiting for this response, stop this timer thread
                                    foundThreadPair = trp;
                                    System.out.println("stopped timer");
                                    break;
                                }
                            }
                            if (foundThreadPair != null)
                                UDP_peerconnection.waitingForResponseThreads.remove(foundThreadPair);
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