package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;


public class UDP_peerconnection extends PeerConnection{

    public class ThreadResponsePair{
        Timer timer;
        InetAddress addr;
        int port;
        String JSON_Response;
        public ThreadResponsePair(Timer timer,
                                  InetAddress addr,
                                  int port,
                                  String JSON_response){
            this.timer = timer;
            this.addr= addr;
            this.port = port;
            this.JSON_Response = JSON_response;
        }
    }

    public static CopyOnWriteArrayList<ThreadResponsePair> waitingForResponseThreads= new CopyOnWriteArrayList<>();

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

    public static boolean isResponseMessage(String message){
        return message.contains("_RESPONSE");
    }

    @Override
    public void send(String JSON_msg) {
        try {
            byte[] mes = JSON_msg.getBytes("utf-8");
            dp_send = new DatagramPacket(mes, mes.length, address, remotePort);

            if(!isResponseMessage(JSON_msg)) {
                Timer timer = new Timer();
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
                        if (counter >= retry) {
                            if(JSON_msg.equals("HANDSHAKE_REQUEST"))
                                RemovePeerToWaitingList(UDP_peerconnection.this);
                            fileSystemObserver.remove(UDP_peerconnection.this);
                            timer.cancel();
                        }
                    }
                }, 0, TimeoutInverval); // timer to resend after interval. give up after count has been reached
                // add the timer and the remote peer's IP and port and the expected response so the timer can be stopped when this response is receieved
                waitingForResponseThreads.add(new ThreadResponsePair(timer, this.getInetAddr(), this.getPort(), JSON_process.GENERATE_RESPONSE_MSG(JSON_msg)));
            } else
                ds.send(dp_send); // if the message sent is a response message, don't need to retry
            log.info("UDP peer sent message to host: " + address.toString() + " port: " + remotePort + " msg:" + JSON_msg);
            log.info("sent message length = " + mes.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void CloseConnection() {
        ServerMain.getInstance().remove(this);
        UDP_peerconnection.RemovePeerToWaitingList(this);
    }
}