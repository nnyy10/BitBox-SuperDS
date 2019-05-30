package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class UDP_peerconnection extends PeerConnection implements Runnable{

    public UDP_peerconnection(DatagramSocket dsocket) {

        super(dsocket);
    }



    @Override
    public void run() {
            String line="";
            synchronous();


        String udpport= Configuration.getConfigurationValue("port");
        DatagramSocket dSocket = null;
        DatagramPacket dPacket =null;
        try {
            dSocket = new DatagramSocket(Integer.parseInt(udpport));
            byte[] by = new byte[1472];

            while (true) {
                dPacket = new DatagramPacket(by, by.length);
                dSocket.receive(dPacket);
                byte by1[] = dPacket.getData();
                line=by1.toString();
                log.info("Peer recieved message: " + line);
                if (line != null) {
                    handleMessage(line);
                } else {
                    log.info("The recieved message is null, closing connection.");
                    CloseConnection();
                    break;
                }
                if (this.ErrorEncountered)
                    break;
            }
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            dSocket.close();
        }
    }

    @Override
    public void send(String message) {
        try {
            int udpport =this.dsocket.getPort();
            InetAddress IAddress=this.dsocket.getLocalAddress();
            byte[] by = message.getBytes("utf-8");
            dsocket.send(new DatagramPacket(by, by.length, IAddress, udpport));
            log.info("Peer sent message: " + message);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}