package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class UDP_peerconnection extends PeerConnection{

    protected  String hostadd=Configuration.getConfigurationValue("advertisedName");
    protected  int hostPort=Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
    private InetAddress address;
    private DatagramPacket dp_send = null;
    private DatagramSocket ds;
    private int TimeoutInverval = Integer.parseInt(Configuration.getConfigurationValue("UDPtimeoutMS"));
    private int Retry = Integer.parseInt(Configuration.getConfigurationValue("UDPretry"));

    protected static ArrayList<UDP_peerconnection> waitingForHandshakeConnections = new ArrayList<>();

    protected InetAddress getInetAddr(){
        return address;
    }


    public UDP_peerconnection(DatagramSocket ds, InetAddress address, int port) {
        super();
        this.ds=ds;
        this.fileSystemObserver=ServerMain.getInstance();
        this.address = address;
        this.remoteAddress = this.address.toString().replace("/","");
        this.remotePort = port;
    }

    public static void AddPeerToWaitingList(UDP_peerconnection peer){
        if(peer != null && !UDP_peerconnection.waitingForHandshakeConnections.contains(peer))
            UDP_peerconnection.waitingForHandshakeConnections.add(peer);
    }

    public static void RemovePeerToWaitingList(UDP_peerconnection peer){
        if(peer != null && UDP_peerconnection.waitingForHandshakeConnections.contains(peer))
            UDP_peerconnection.waitingForHandshakeConnections.remove(peer);
    }

    public void sendHS(){
        String Cmesg = JSON_process.HANDSHAKE_REQUEST(hostadd, hostPort);
        send(Cmesg);
        UDP_peerconnection.AddPeerToWaitingList(this);
    }

    @Override
    public boolean equals(Object peer) {
        UDP_peerconnection p = (UDP_peerconnection) peer;
        if(p.getInetAddr().equals(this.getInetAddr()) && p.getPort() == this.getPort())
            return true;
        else
            return false;
    }

    @Override
    public void send(String JSON_msg) {
        try {
            byte[] mes = JSON_msg.getBytes("utf-8");
            dp_send = new DatagramPacket(mes, mes.length, address, remotePort);

            Timer timer = new Timer();
            int begin = 0;
            int timeInterval = this.TimeoutInverval;
            timer.schedule(new TimerTask() {
                int counter = 0;
                int retry = Integer.parseInt(Configuration.getConfigurationValue("UDPretry"));
                @Override
                public void run() {
                    try {
                        ds.send(dp_send);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    counter++;
                    if (counter >= retry){
                        timer.cancel();
                    }
                }
            }, begin, timeInterval);
            log.info("UDP peer sent message to host: " + address.toString() + " port: " + remotePort + " msg:" + JSON_msg);
            log.info("sent message length = " + mes.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void CloseConnection() {
        UDP_peerconnection.RemovePeerToWaitingList(this);
    }

    @Override
    protected boolean SendCloseMessage() {
        return false;
    }
}