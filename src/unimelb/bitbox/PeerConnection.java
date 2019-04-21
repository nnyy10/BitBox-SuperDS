package unimelb.bitbox;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class PeerConnection implements Runnable{
	
	private Socket clientSocket = null;
	private DataInputStream input = null;
	private DataOutputStream output = null;
	
    protected String serverText   = null;

    public PeerConnection(Socket clientSocket, String serverText) {
        this.clientSocket = clientSocket;
        this.serverText   = serverText;
        
        try {
			input  = new DataInputStream(this.clientSocket.getInputStream());
			output = new DataOutputStream(this.clientSocket.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void run() {
        try {

            long time = System.currentTimeMillis();
            output.write(("HTTP/1.1 200 OK\n\nWorkerRunnable: " + this.serverText + " - " + time + "").getBytes());
            output.close();
            input.close();
            System.out.println("Request processed: " + time);
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
    }
}