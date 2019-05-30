package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TCP_peerconnection extends PeerConnection implements Runnable {

    public TCP_peerconnection(Socket socket) {
        super(socket);
    }

    @Override
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
                if (this.ErrorEncountered)
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
