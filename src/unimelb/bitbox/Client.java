package unimelb.bitbox;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
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

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Client {

    protected static void CloseConnection(Socket socket, BufferedWriter outputStream, BufferedReader inputStream) {
        log.info("Closing Connection");
        try {
            inputStream.close();
        } catch (Exception e) {}
        try {
            outputStream.close();
        } catch (Exception e) {}
        try {
            socket.close();
        } catch (Exception e) {}
    }

    public static boolean send(String message, BufferedWriter outputStream) {
        try {
            outputStream.write(message + "\n");
            outputStream.flush();
            log.info("Peer sent message: " + message);
            return true;
        } catch (Exception e) {
            log.warning("Peer encountered ERROR when sending message: " + message);
            return false;
        }
    }

    private static Logger log = Logger.getLogger(Client.class.getName());

    private static void InputInvalid(){
        log.warning("Input invalid\n" +
                "please input like this: java -cp bitbox.jar unimelb.bitbox.Client -c [command] -s [serverHost:port] -p [peerHost:port]");
    }


    public static void main(String [] args){
        //Object that will store the parsed command line arguments
        CmdLineArgs argsBean = new CmdLineArgs();

        //Parser provided by args4j
        CmdLineParser parser = new CmdLineParser(argsBean);
        try {
            //Parse the arguments
            parser.parseArgument(args);

            //After parsing, the fields in argsBean have been updated with the given
            //command line arguments
            String serverHost = argsBean.getServerHost();
            int serverPort = argsBean.getServerPort();
            String peerHost = argsBean.getPeerHost();
            int peerPort = argsBean.getPeerPort();
            String command = argsBean.getCommand();
            String identity = argsBean.getIdentity();
            //String encryptedSharedKey = Encryption.encryptSharedKey(identity);
            log.info("try to connect \nhost:" + serverHost + " port: " + serverPort);
            Socket socket = new Socket(serverHost, serverPort);
            log.info("connected!");
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            switch (command){
                case "list_peer":
                    if (!send(JSON_process.AUTH_REQUEST(identity), outputStream)) {
                        log.warning("authority send failed");
                         CloseConnection(socket,outputStream,inputStream);
                    }else{
                        String Auth_response = inputStream.readLine();
                        JSONParser JSparser =  new JSONParser();
                        JSONObject obj = (JSONObject) JSparser.parse(Auth_response);
                        if(Auth_response != null && obj.get("command").equals("AUTH_RESPONSE")){

                            String encryptedSharedKey = (String) obj.get("AES128");
                            //String encryptedSharedKey = null;
                            String sharedKey = Encryption.decryptSharedKey(encryptedSharedKey, "id_rsa");
                            //System.out.println(sharedKey);
                            String Msg = JSON_process.LIST_PEER_REQUEST();
                            log.info("message ready to send: " + Msg);
                            String encryptMSG = Encryption.encryptMessage(Msg, sharedKey);
                            if (!send(JSON_process.Payload(encryptMSG), outputStream)){
                                log.warning("payload send failed");
                                CloseConnection(socket, outputStream,inputStream);}
                            else{
                                log.info("send payload successfully");
                                String payloadMsg = inputStream.readLine();
                                //System.out.println(payloadMsg);
                                String list_peer = Encryption.decryptMessage(payloadMsg, sharedKey);
                                System.out.println(list_peer);
                                JSONObject obj1 = (JSONObject) JSparser.parse(list_peer);
                                if(obj1.get("command").equals("LIST_PEERS_RESPONSE")){
                                    JSONArray peers =(JSONArray) obj1.get("peers");
                                    if(peers.size()!=0){
                                        for (int i = 0; i < peers.size();i++) {
                                            JSONObject peer = (JSONObject) peers.get(i);
                                            String host = (String) peer.get("host");
                                            long port = (long) peer.get("port");
                                            System.out.println("peer: "+ host + ":"+ port);
                                        }
                                    }else{
                                        System.out.println("No peers in list");
                                        CloseConnection(socket,outputStream,inputStream);
                                        return;
                                    }

                                    CloseConnection(socket, outputStream,inputStream);
                                }else{
                                    log.warning("msg received wrong or null, closing connection");
                                    CloseConnection(socket, outputStream,inputStream);
                                }

                            }
                        }else{
                            log.warning("msg received is null or wrong, closing connection");
                            CloseConnection(socket,outputStream,inputStream);
                        }
                    }

                    break;
                case "connect_peer":
                    if (!send(JSON_process.AUTH_REQUEST(identity), outputStream)) {
                        log.warning("authority send failed");
                        CloseConnection(socket,outputStream,inputStream);
                    }else{
                        String Auth_response = inputStream.readLine();
                        JSONParser JSparser =  new JSONParser();
                        JSONObject obj = (JSONObject) JSparser.parse(Auth_response);
                        if(Auth_response != null && obj.get("command").equals("AUTH_RESPONSE")) {
                            String encryptedSharedKey = (String) obj.get("AES128");
                            //String encryptedSharedKey = null;
                            String sharedKey = Encryption.decryptSharedKey(encryptedSharedKey, "id_rsa");
                            String Msg = JSON_process.CONNECT_PEER_REQUEST(peerHost,peerPort);
                            //server reads its host and port itself
                            String encryptMSG = Encryption.encryptMessage(Msg, sharedKey);
                            if (!send(JSON_process.Payload(encryptMSG), outputStream)){
                                log.warning("payload send failed");
                                CloseConnection(socket, outputStream,inputStream);}
                            else {
                                log.info("payload send successfully");
                                String payloadMsg = inputStream.readLine();
                                String connect_response = Encryption.decryptMessage(payloadMsg, sharedKey);
                                JSONObject obj1 = (JSONObject) JSparser.parse(connect_response);
                                //boolean status =(boolean) obj1.get("status");
                                String msg = (String) obj1.get("message");
                                log.info(msg);
                                CloseConnection(socket, outputStream,inputStream);
                            }
                        }else {
                            log.warning("msg received is null or wrong, closing connection");
                            CloseConnection(socket,outputStream,inputStream);
                        }
                    }
                    break;
                case "disconnect_peer":
                    if (!send(JSON_process.AUTH_REQUEST(identity), outputStream)) {
                        log.warning("authority send failed");
                        CloseConnection(socket,outputStream,inputStream);
                    }else{
                        String Auth_response = inputStream.readLine();
                        JSONParser JSparser =  new JSONParser();
                        JSONObject obj = (JSONObject) JSparser.parse(Auth_response);
                        if(Auth_response != null && obj.get("command").equals("AUTH_RESPONSE")) {
                            String encryptedSharedKey = (String) obj.get("AES128");
                            //String encryptedSharedKey = null;
                            String sharedKey = Encryption.decryptSharedKey(encryptedSharedKey, "id_rsa");
                            String Msg = JSON_process.DISCONNECT_PEER_REQUEST(peerHost,peerPort);
                            //server reads its host and port itself
                            String encryptMSG = Encryption.encryptMessage(Msg, sharedKey);
                            if (!send(JSON_process.Payload(encryptMSG), outputStream)){
                                log.warning("payload send failed");
                                CloseConnection(socket, outputStream,inputStream);}
                            else{
                                log.info("send payload successfully");
                                String payloadMsg = inputStream.readLine();
                                String disconnect_response = Encryption.decryptMessage(payloadMsg, sharedKey);
                                JSONObject obj1 = (JSONObject) JSparser.parse(disconnect_response);
                                //boolean status =(boolean) obj1.get("status");
                                String msg = (String) obj1.get("message");
                                log.info(msg);
                                CloseConnection(socket, outputStream,inputStream);
                            }

                        }
                        else {
                            log.warning("msg received is null or wrong ,closing connection");
                            CloseConnection(socket, outputStream,inputStream);
                        }
                    }

                    //send(JSON_process.DISCONNECT());
                    break;
                default:
                    log.warning("No such a command");
                    break;
            }
        } catch (CmdLineException e) {

            System.err.println(e.getMessage());

            //Print the usage to help the user understand the arguments expected
            //by the program
            parser.printUsage(System.err);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }


    }

}
