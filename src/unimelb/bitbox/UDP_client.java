//package unimelb.bitbox;
//
////A Java program for a TCP_Server
//import java.net.*;
//import java.util.ArrayList;
//import java.util.logging.Logger;
//
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//
//import java.io.*;
//
//
//public class UDP_client extends UDP_peerconnection {
//
//    private static Logger log = Logger.getLogger(TCP_Client.class.getName());
//
//    public UDP_client(DatagramSocket dsocket, String address, int port) {
//        super(dsocket, address, port);
//
//        try {
//
//            String Cmesg = JSON_process.HANDSHAKE_REQUEST(this.dsocket.getLocalAddress().toString(), this.dsocket.getLocalPort());
//            send(Cmesg);
//
//            String temp = inputStream.readLine();
//            try {
//                JSONParser parser = new JSONParser();
//                JSONObject jsonMsg = (JSONObject) parser.parse(temp);
//                String jsonCommand = (String) jsonMsg.get("command");
//                JSONArray peers;
//                String host;
//                Socket outGoingSocket = null;
////                UDP_Client outGoingConnection = null;
////                Thread connectionThread = null;
//
//                switch(jsonCommand){
//                    case "HANDSHAKE_RESPONSE":
//                        break;
//                    case "CONNECTION_REFUSED":
//                        JSONObject obj;
//                        peers = (JSONArray) jsonMsg.get("peers");
//                        ArrayList<PeerConnection> connect = ServerMain.getInstance().getlist();
//                        for(int i = 0; i< peers.size();i++){
//                            obj = (JSONObject) peers.get(i);
//                            host = (String) obj.get("host");
//                            port = (int) obj.get("port");
////                            outGoingSocket = new Socket(host, port);
//                            for (int j = 0; j< connect.size(); j++) {
//                                if(!host.equals(connect.get(j).dsocket.getRemoteSocketAddress().toString())){
//                                    log.info("Trying to connect peer client to: " + host + ":" + port);
//                                    try {
////                                        outGoingConnection = new TCP_Client(outGoingSocket);
////                                        connectionThread = new Thread(outGoingConnection);
////                                        connectionThread.start();
//                                        log.info("Reconnected to: " + "host: " + host + "port: " + port);
//                                        break;
//                                    } catch (Exception e) {
//                                        log.info("Can't connect to: " + host + ":" + port);
//                                        log.info("Try connecting to another peer");
//                                    }
//                                }
//                                else log.info("Already connected to " + "host: "+ host + "port: " +port + ":) ");
//                            }
//                        }
//                    default:
//                        log.info("Handshake response invalid, closing socket.");
//                        this.CloseConnection();
//                        return;
//                }
//            }
//            catch (Exception e){
//                String message = "Handshake response invalid, closing connection.";
//                String invalidProtocolMsg = JSON_process.INVALID_PROTOCOL(message);
//                log.info(message);
//                send(invalidProtocolMsg);
//                CloseConnection();
//                return;
//            }
//        } catch (IOException e) {
//            log.info("Connection FAILED.");
//            this.CloseConnection();
//            return;
//        }
//
//        this.fileSystemObserver.add(this);
//
//        log.info("client successfully connected to " + this.dsocket.getRemoteSocketAddress().toString());
//    }
//
//    public void switch_jsonCommand(){}
//    protected void finalize() throws Throwable {
//        this.CloseConnection();
//    }
//}
