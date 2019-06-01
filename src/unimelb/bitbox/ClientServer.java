package unimelb.bitbox;

import java.net.*;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unimelb.bitbox.util.Configuration;

import java.io.*;

public class ClientServer implements Runnable {

    public DatagramSocket datagramSocket = null;

    private static Logger log = Logger.getLogger(Client.class.getName());

    private String sharedKey = "";

    private int serverPort;
    private ServerSocket serverSocket = null;
    private boolean isStopped = false;
    private Thread runningThread = null;
    private Socket tempServerSocket = null;
    private String readmesg;
    int port;
    private String encryptedSharedKey;

    ClientServer(int port) {
        log.info("Starting ClinetServer, listening for BitBox Client on port: " + port);
        this.serverPort = port;
    }

    public void run() {
        log.info("Starting TCP server");

        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while (!isStopped()) {
            tempServerSocket = null;
            try {
                tempServerSocket = this.serverSocket.accept();
                log.info("ClientServer accepted new client");
                BufferedReader inputStream = new BufferedReader(new InputStreamReader(tempServerSocket.getInputStream(), "UTF-8"));
                BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(tempServerSocket.getOutputStream(), "UTF-8"));
                readmesg = inputStream.readLine();
                JSONParser parser = new JSONParser();
                JSONObject jsonMsg = (JSONObject) parser.parse(readmesg);

                if (HandleAuthentication(readmesg, outputStream, inputStream)) {
                    readmesg = inputStream.readLine();
                    HandlePayload(readmesg, outputStream, inputStream);
                }
                CloseConnection(tempServerSocket);
            } catch (Exception e) {
                log.warning(e.toString());
            }
            tempServerSocket = null;
        }
    }

    protected static void CloseConnection(Socket socket) {
        log.info("ClientServer Closing Connection to BitBoxClient");
        try {
            if (socket != null) {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
            }
        } catch (Exception e) {}
        log.info("ClientServer Closed Connection to BitBoxClient");
    }

    public static boolean send(String message, BufferedWriter outputStream) {
        try {
            outputStream.write(message + "\n");
            outputStream.flush();
            log.info("PeerClient sent message: " + message);
            return true;
        } catch (Exception e) {
            log.warning("PeerClient encountered ERROR when sending message: " + message);
            return false;
        }
    }

    private boolean HandleAuthentication(String message, BufferedWriter outputStream, BufferedReader inputStream) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonMsg = (JSONObject) parser.parse(message);
            String command = (String) jsonMsg.get("command");
            if (command.equals("AUTH_REQUEST")) {
                log.info("Auth request received");
                String identity = (String) jsonMsg.get("identity");
                sharedKey = Encryption.getSharedKey();
                this.encryptedSharedKey = Encryption.encryptSharedKey(identity, sharedKey);
                String msg = JSON_process.AUTH_RESPONSE(true, encryptedSharedKey);
                if (encryptedSharedKey != null) {
                    if (send(msg, outputStream)) {
                        log.info("send successfully, msg: " + msg);
                        return true;
                    } else {
                        log.warning("send response failed");
                    }
                } else {
                    log.warning("error with encryption, closing socket");
                }
            } else {
                log.warning("message is null or wrong");
            }
        } catch (ParseException e) {
            log.warning(e.toString());
        }
        return false;
    }

    private boolean HandlePayload(String message, BufferedWriter outputStream, BufferedReader inputStream) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonMsg = (JSONObject) parser.parse(message);
            String payloadMSG = (String) jsonMsg.get("payload");

            if (payloadMSG != null) {
                String decryptMessage = Encryption.decryptMessage(payloadMSG, sharedKey);
                System.out.println(decryptMessage);
                JSONObject Msg = (JSONObject) parser.parse(decryptMessage);
                String jsonCommand = (String) Msg.get("command");
                switch (jsonCommand) {
                    case "LIST_PEERS_REQUEST":
                        CopyOnWriteArrayList<PeerConnection> connections = ServerMain.getInstance().getlist();
                        String listPeerResponse = "";
                        if (connections.size()!=0) {
                            String[] tempIPlist = new String[connections.size()];
                            int[] tempPrlist = new int[connections.size()];
                            for (int i = 0; i < connections.size(); i++) {
                                tempIPlist[i] = connections.get(i).getAddr();
                                tempPrlist[i] = connections.get(i).getPort();
                            }
                            listPeerResponse = JSON_process.LIST_PEERS_RESPONSE(tempIPlist, tempPrlist);
                        } else
                            listPeerResponse = JSON_process.LIST_PEERS_RESPONSE(null, null);

                        send(Encryption.encryptMessage(listPeerResponse, sharedKey), outputStream);
                        return true;
                    case "CONNECT_PEER_REQUEST":
                        String host =(String) Msg.get("host");
                        port = ((Long) Msg.get("port")).intValue();
                        boolean alreadyConnected = false;
                        for(PeerConnection peer: ServerMain.getInstance().getlist()){
                            if(peer.getAddr().equals(host) && peer.getPort() == port){
                                alreadyConnected = true;
                                break;
                            }
                        }
                        String mode = Configuration.getConfigurationValue("mode");
                        if (mode.equals("tcp") || mode.equals("TCP")){
                            if(alreadyConnected){
                                log.info("already connected");
                                String msg = JSON_process.CONNECT_PEER_RESPONSE(host, port,false);
                                send(Encryption.encryptMessage(msg, sharedKey),outputStream);
                            }
                            else{
                                Socket try2connect = new Socket(host,port);
                                TCP_Client newConnection = new TCP_Client(try2connect);
                                if(newConnection.SendHandshake()){
                                    send(Encryption.encryptMessage(JSON_process.CONNECT_PEER_RESPONSE(host,port,true),sharedKey),outputStream);
                                    Thread connectionThread = new Thread(newConnection);
                                    connectionThread.start();
                                    return true;
                                }else{
                                    log.warning("connect failed");
                                    send(Encryption.encryptMessage(JSON_process.CONNECT_PEER_RESPONSE(host, port,false),sharedKey),outputStream);
                                }
                            }
                        } else{
                            if(alreadyConnected){
                                log.info("already connected");
                                send(Encryption.encryptMessage(JSON_process.CONNECT_PEER_RESPONSE(host,port,false),sharedKey),outputStream);
                            }
                            else{
                                send(Encryption.encryptMessage(JSON_process.CONNECT_PEER_RESPONSE(host,port,true),sharedKey),outputStream);
                                UDP_peerconnection udpPeer = new UDP_peerconnection(datagramSocket, InetAddress.getByName(host), port);
                                udpPeer.sendHS();
                            }
                        }
                        break;
                    case "DISCONNECT_PEER_REQUEST":
                        host =(String) Msg.get("host");
                        port = ((Long) Msg.get("port")).intValue();
                        PeerConnection foundPeer = null;
                        alreadyConnected = false;
                        for (PeerConnection peer : ServerMain.getInstance().getlist()) {
                            if (peer.getAddr().contains(host) && peer.getPort() == port) {
                                alreadyConnected = true;
                                foundPeer = peer;
                                break;
                            }
                        }
                        if (alreadyConnected) {
                            //disconnect
                            send(Encryption.encryptMessage(JSON_process.DISCONNECT_PEER_RESPONSE(host, port, true), sharedKey), outputStream);
                            foundPeer.CloseConnection();
                            log.info("ClientServer connection closed to " + host + ":" +port);
                            return true;
                        } else {
                            log.warning("not connected yet, disconnect false");
                            send(Encryption.encryptMessage(JSON_process.DISCONNECT_PEER_RESPONSE(host,port,false),sharedKey),outputStream);
                        }
                        break;
                    default:
                        log.info("No such command");
                        break;
                }
            } else {
                log.warning("Payload is null");
            }
        } catch (Exception e) {
            log.warning(e.toString());
            e.printStackTrace();
        }
        return false;
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + Integer.toString(this.serverPort), e);
        }
    }
}
