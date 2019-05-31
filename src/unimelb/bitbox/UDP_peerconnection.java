package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class UDP_peerconnection extends PeerConnection{

    private InetAddress address;
    private int port;
    private DatagramPacket dp_send = null;
    private DatagramSocket ds;

    protected static ArrayList<UDP_peerconnection> waitingForHandshakeConnections = new ArrayList<>();

    protected InetAddress getAddr(){
        return address;
    }

    public int getPort(){
        return port;
    }

    public UDP_peerconnection(DatagramSocket ds, String address, int port) {
        super();
        this.ds=ds;
        this.fileSystemObserver=ServerMain.getInstance();
        try {
            this.address = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            log.warning("address invalid");
            log.warning(e.toString());
        }
        this.port = port;
    }

    public void sendHS(){
        String Cmesg = JSON_process.HANDSHAKE_REQUEST(this.ds.getLocalAddress().toString(), this.ds.getLocalPort());
        send(Cmesg);
        UDP_peerconnection.AddPeerToWaitingList(this);
    }

    public static void AddPeerToWaitingList(UDP_peerconnection peer){
        if(peer != null && !UDP_peerconnection.waitingForHandshakeConnections.contains(peer))
            UDP_peerconnection.waitingForHandshakeConnections.add(peer);
    }

    public static void RemovePeerToWaitingList(UDP_peerconnection peer){
        if(peer != null && UDP_peerconnection.waitingForHandshakeConnections.contains(peer))
            UDP_peerconnection.waitingForHandshakeConnections.remove(peer);
    }

    @Override
    public void send(String JSON_msg) {
        try {
            byte[] mes = JSON_msg.getBytes("utf-8");
            dp_send = new DatagramPacket(mes, mes.length, address, port);
            ds.send(dp_send);
            log.warning("Sending handshake response");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void CloseConnection() {
        UDP_peerconnection.RemovePeerToWaitingList(this);
    }
}