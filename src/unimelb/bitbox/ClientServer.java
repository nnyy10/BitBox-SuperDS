package unimelb.bitbox;

import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
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

import java.io.*;
import java.net.Socket;

public class ClientServer implements Runnable{
    private static Logger log = Logger.getLogger(Client.class.getName());

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


    protected int serverPort = 0;
    protected ServerSocket serverSocket = null;
    protected boolean isStopped = false;
    protected Thread runningThread = null;

    Socket tempServerSocket = null;

    String Smesg,readmesg,host; long port;

    protected ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public ClientServer(int port){
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
                BufferedReader inputStream = new BufferedReader(new InputStreamReader(tempServerSocket.getInputStream(), "UTF-8"));
                BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(tempServerSocket.getOutputStream(), "UTF-8"));
                readmesg = inputStream.readLine();
                try{
                    JSONParser parser = new JSONParser();
                    JSONObject jsonMsg = (JSONObject) parser.parse(readmesg);
                    String jsonCommand = (String) jsonMsg.get("command");
                    




                }
                catch (Exception e){
                    e.printStackTrace();
                }
            } catch(Exception e){
                e.printStackTrace();
            }

        }
    }
    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
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
