package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TCP_peerconnection extends PeerConnection implements Runnable {

    protected Socket socket;
    protected BufferedReader inputStream = null;
    protected BufferedWriter outputStream = null;
    protected boolean ConnectionClosed = false;
    protected ScheduledExecutorService exec = null;

    public void synchronous() {
        int synTime = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));

        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
            for (FileSystemManager.FileSystemEvent event : this.fileSystemObserver.fileSystemManager.generateSyncEvents()) {
                String syn;
                syn = ServerMain.getInstance().FileSystemEventToJSON(event);
                send(syn);
            }
        }, 0, synTime, TimeUnit.SECONDS);
    }

    @Override
    protected void CloseConnection() {
        if(!ConnectionClosed) {
            exec.shutdown();
            ConnectionClosed = true;
            log.warning("Closing Connection : " + this.getAddr() + this.getPort());
            this.fileSystemObserver.remove(this);
            try {
                if (socket != null) {
                    socket.shutdownInput();
                    socket.shutdownOutput();
                    socket.close();
                }
            } catch (Exception e) {
            }
            log.warning("Connection Closed : " + this.getAddr() + this.getPort());
        }
    }

    @Override
    protected boolean SendCloseMessage() {
        return false;
//        if(ConnectionClosed)
//            return false;
//        else{
//            this.send(JSON_process.);
//        }
    }

    public TCP_peerconnection(Socket socket) {
        super();
        this.socket = socket;
        this.remotePort = this.socket.getPort();
        this.remoteAddress = this.socket.getRemoteSocketAddress().toString();
        this.fileSystemObserver = ServerMain.getInstance();
        try{
            inputStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
            outputStream = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), "UTF-8"));
        } catch (IOException e) {
            this.CloseConnection();
        }
    }

    public void run() {
        String line = "";

        synchronous();

        while (true) {
            try {
                line = inputStream.readLine();
                log.info("Peer recieved message: " + line);
                if (line != null) {
                    handleMessage(line);
                } else {
                    log.info("The recieved message is null, closing connection.");
                    this.CloseConnection();
                    break;
                }
                if (this.ConnectionClosed)
                    break;
            } catch (Exception e) {
                this.CloseConnection();
                break;
            }
        }
        this.CloseConnection();
    }

    @Override
    public void send(String message) {
        try {
            outputStream.write(message + "\n");
            outputStream.flush();

            log.info("Peer sent message: " + message);
        } catch (Exception e) {
            log.warning("Peer encountered ERROR when sending message: " + message);
            this.CloseConnection();
        }
    }

}
