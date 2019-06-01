package unimelb.bitbox;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import unimelb.bitbox.util.CmdLineArgs;
import unimelb.bitbox.Encryption;
import unimelb.bitbox.JSON_process;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.ServerMain;
import unimelb.bitbox.TCP_Client;

import java.io.*;

public class ClientServer implements Runnable {

    public DatagramSocket datagramSocket = null;

    private static Logger log = Logger.getLogger(Client.class.getName());

    protected static void CloseConnection(Socket socket, BufferedWriter outputStream, BufferedReader inputStream) {
        log.info("Closing Connection");
        try {
            inputStream.close();
        } catch (Exception e) {
        }
        try {
            outputStream.close();
        } catch (Exception e) {
        }
        try {
            socket.close();
        } catch (Exception e) {
        }
        socket = null;
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

    private String sharedKey = "";

    private int serverPort = 0;
    private ServerSocket serverSocket = null;
    private boolean isStopped = false;
    private Thread runningThread = null;

    Socket tempServerSocket = null;

    String Smesg, readmesg, host;
    long port;

    protected ExecutorService threadPool = Executors.newFixedThreadPool(10);

    ClientServer(int port) {
        log.info("Starting ClinetServer, listening for BitBox Client on port: " + port);
        this.serverPort = port;
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
                        CloseConnection(tempServerSocket, outputStream, inputStream);
                    }
                } else {
                    log.warning("error with encryption, closing socket");
                    CloseConnection(tempServerSocket, outputStream, inputStream);
                }
            } else {
                log.warning("message is null or wrong");
                CloseConnection(tempServerSocket, outputStream, inputStream);
            }
        } catch (ParseException e) {
            log.warning(e.toString());
            CloseConnection(tempServerSocket, outputStream, inputStream);
            return false;
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
                JSONObject Msg = (JSONObject) parser.parse(decryptMessage);
                String jsonCommand = (String) Msg.get("command");
                switch (jsonCommand) {
                    case "LIST_PEERS_REQUEST":
                        ArrayList<PeerConnection> connections = ServerMain.getInstance().getlist();
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
//                        String host =(String) jsonMsg.get("host");
//                        Long port = (Long) jsonMsg.get("port");
//                        boolean alreadyConnected = false;
//                        if (Configuration.getConfigurationValue("mode").equals("tcp") ||
//                                Configuration.getConfigurationValue("mode").equals("TCP")){
//                            for(PeerConnection peer: ServerMain.getInstance().getlist()){
//                                if(peer.getAddr().equals(host) && peer.getPort() == port){
//                                    alreadyConnected = true;
//                                    break;
//                                }
//                            }
//                            if(alreadyConnected){
//                                log.info("already connected");
//                                String encryptedMsg = Encryption.encryptMessage(JSON_process.CONNECT_PEER_RESPONSE(host, port,false),sharedKey);
//                                send(encryptedMsg,outputStream);
//                                //CloseConnection(tempServerSocket,outputStream,inputStream);
//                            }
//                            else{
//                                Socket try2connect = new Socket(host,port);
//                                TCP_Client newConnection = new TCP_Client(try2connect);
//
//                                if(newConnection.SendHandshake()){
//                                    send(Encryption.encryptSharedKey(JSON_process.CONNECT_PEER_RESPONSE(host,port,true),sharedKey),outputStream);
//                                    //CloseConnection(tempServerSocket,outputStream, inputStream);
//                                }else{
//                                    log.warning("connect failed");
//                                    send(Encryption.encryptMessage(JSON_process.CONNECT_PEER_RESPONSE(host, port,false),sharedKey),outputStream);
//                                    //CloseConnection(tempServerSocket,outputStream,inputStream);
//                                }
//
//                            }
//                        }else{
//                            for(PeerConnection peer: ServerMain.getInstance().getlist()){
//                                if(peer.getAddr().equals(host) && peer.getPort() == port){
//                                    alreadyConnected = true;
//                                    break;
//                                }
//                            }
//                            if(alreadyConnected){
//                                log.info("already connected");
//                                send(Encryption.encryptMessage(JSON_process.CONNECT_PEER_RESPONSE(host,port,false),sharedKey),outputStream);
//                                //CloseConnection(tempServerSocket,outputStream,inputStream);
//                            }
//                            else{
//                                //boolean connected = false;
//
//                                //connect in udp mode and default!
//                                //if(){}else{}
//                                UDP_peerconnection udpPeer = new UDP_peerconnection(datagramSocket, InetAddress.getByName(host), port);
//                                udpPeer.sendHS();
//
//                                send(Encryption.encryptMessage(JSON_process.CONNECT_PEER_RESPONSE(host,port,true),sharedKey),outputStream);
//                                //CloseConnection(tempServerSocket, outputStream, inputStream);
//
//                            }
//                        }
//
//                        break;
                    default:
                        log.info("in client server default branch");
                        break;
                }
            } else {
                log.warning("Payload is null");
            }
        } catch (ParseException e) {
            log.warning(e.toString());
        }
        return false;
    }

//                    case "DISCONNECT_PEER_REQUEST":
//                        host =(String) jsonMsg.get("host");
//                        port = (int) jsonMsg.get("port");
//                        PeerConnection foundPeer = null;
//                        alreadyConnected = false;
//                        for (PeerConnection peer : ServerMain.getInstance().getlist()) {
//                            if (peer.getAddr().equals(host) && peer.getPort() == port) {
//                                alreadyConnected = true;
//                                foundPeer = peer;
//                                break;
//                            }
//                        }
//                        if (alreadyConnected) {
//                            //disconnect
//                            foundPeer.CloseConnection();
//                            send(Encryption.encryptMessage(JSON_process.DISCONNECT_PEER_RESPONSE(host, port, true), sharedKey), outputStream);
//                            log.info("payload sent");
//                            //CloseConnection(tempServerSocket,outputStream,inputStream);
//                        } else {
//                            log.warning("not connected yet, disconnect false");
//                            send(Encryption.encryptMessage(JSON_process.DISCONNECT_PEER_RESPONSE(host,port,false),sharedKey),outputStream);
//                            //CloseConnection(tempServerSocket,outputStream,inputStream);
//                        }
//
//                        break;
//                    default:
//                        log.warning("No such a command");
//                        //CloseConnection(tempServerSocket,outputStream,inputStream);


    private String encryptedSharedKey;

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
                    CloseConnection(tempServerSocket, outputStream, inputStream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop() {
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }


    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + Integer.toString(this.serverPort), e);
        }
    }
}
