package unimelb.bitbox;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import unimelb.bitbox.util.FileSystemManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.io.File;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.util.Date;

import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;;


public class PeerConnection implements Runnable {

    protected Socket socket = null;
    protected BufferedReader inputStream = null;
    protected BufferedWriter outputStream = null;
    protected ServerMain fileSystemObserver = null;

    public PeerConnection(Socket socket) {

        this.socket = socket;
        this.fileSystemObserver = ServerMain.getInstance();
        try {
            inputStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
            outputStream = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), "UTF-8"));
        } catch (IOException e) {
            this.CloseConnection();
        }
    }

    protected void CloseConnection() {
        System.out.println("Closing Connection");
        try {
            this.inputStream.close();
        } catch (Exception e) {
        }
        try {
            this.outputStream.close();
        } catch (Exception e) {
        }
        try {
            this.socket.close();
        } catch (Exception e) {
        }
    }

    public void run() {
        String line = "";
        System.out.println("in run");
        // reads message from client until "Over" is sent 
        while (true) {
            try {
                line = inputStream.readLine();
                System.out.println("Recieved Message: " + line);
                implement(line);

            } catch (Exception e) {
                this.CloseConnection();
                break;
            }
        }
    }
    public void send(String message){
    	try{
    		outputStream.write(message+"\n");
    		outputStream.flush();
    		System.out.println("Sent message: " + message);
    	} catch(Exception e){
    		System.out.println("cant print " + message);
    		this.CloseConnection();
    	}

    }

    public void implement(String str) {
        JSONParser parser = new JSONParser();


        try {
            String md5 = " ", host = " ", msg = " ", pathName = " ", content = " ";
            long size = 0;
            long port = 0, position = 0, length = 0, timestamp = 0;
            JSONObject obj = (JSONObject) parser.parse(str);
            // first JSONObject need to be deal with, and then use obj as input
            String information = (String) obj.get("command");
            JSONObject fileDescriptor;

            /**
             * there still exists a big problem that what is the pathName look like?
             * does it need to be handled though JSON process or here?
             */

            switch (information) {

                case "FILE_CREATE_REQUEST":
                	System.out.println("in file create");
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    size = (long) fileDescriptor.get("fileSize");
                    pathName = (String) obj.get("pathName");
                    //content is useless here

                    if (this.fileSystemObserver.fileSystemManager.isSafePathName(pathName)) {
                        if (!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName,md5)) {
                            File file=new File(pathName);
                            file.createNewFile();
                            try { if (this.fileSystemObserver.fileSystemManager.createFileLoader(pathName, md5, size, timestamp)) {
                                    if (!this.fileSystemObserver.fileSystemManager.checkShortcut(pathName)) {
                                        this.fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
                                        send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, position, length));
                                    }
                                     else{
                                         send(JSON_process.FILE_CREATE_RESPONSE(md5,timestamp,size,pathName,JSON_process.problems.NO_ERROR));
                                     }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            send(JSON_process.FILE_CREATE_RESPONSE(md5,timestamp,size,pathName,JSON_process.problems.FILENAME_EXIST));
                        }
                    } else {
                        send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.UNSAFE_PATH));
                    }
                    break;


                case "FILE_MODIFY_REQUEST":
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    size = (long) fileDescriptor.get("fileSize");
                    pathName = (String) obj.get("pathName");
                    System.out.println("in  file modify");
                    if (this.fileSystemObserver.fileSystemManager.isSafePathName(pathName)) {
                        if (this.fileSystemObserver.fileSystemManager.fileNameExists(pathName,md5)) {
                            try {
                                File exit_file=new File(pathName);
                                exit_file.createNewFile();
                                long last_timestamp=exit_file.lastModified();
                                this.fileSystemObserver.fileSystemManager.deleteFile(pathName, timestamp, md5);
                                if (this.fileSystemObserver.fileSystemManager.modifyFileLoader(pathName,md5,last_timestamp)) {
                                    if (this.fileSystemObserver.fileSystemManager.checkShortcut(pathName)) {
                                        send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, position, length));
                                        //System.out.println(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, position, length));
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {

                            send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.PATHNAME_NOT_EXIST));
                        }
                    } else {
                        send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.UNSAFE_PATH));
                    }
                    break;
                    // is here need send back a response if checkshortcut or anything else goes wrong?

                case "FILE_BYTES_RESPONSE":
                    System.out.println("----=-----------");
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    size = (long) fileDescriptor.get("fileSize");
                    pathName = (String) obj.get("pathName");
                    content = (String) obj.get("content");
                    ByteBuffer src = ByteBuffer.wrap(content.getBytes());
                    if (this.fileSystemObserver.fileSystemManager.writeFile(pathName, src, position)) {
                        if (!this.fileSystemObserver.fileSystemManager.checkWriteComplete(pathName)) {
                            send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, position, length));
                        } else {
                           fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
                           send(JSON_process.FILE_BYTES_RESPONSE(md5,timestamp,size,pathName,position,length,content,JSON_process.problems.NO_ERROR));
                        }
                        // is there need a "else" to send back a successful response?
                    }
                    break;


                case "FILE_BYTES_REQUEST":
                	System.out.println("in file bytes");
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    size = (long) fileDescriptor.get("fileSize");
                    pathName = (String) obj.get("pathName");

                    //content = (String) obj.get("content");
                    content = fileSystemObserver.fileSystemManager.readFile(md5,position,length).toString();
                    System.out.println("get request");
                    System.out.println(content);
                    System.out.println(JSON_process.FILE_BYTES_RESPONSE(md5,timestamp,size,pathName,position,length,content,JSON_process.problems.NO_ERROR));
                    send(JSON_process.FILE_BYTES_RESPONSE(md5,timestamp,size,pathName,position,length,content,JSON_process.problems.NO_ERROR));
                    System.out.println("response sent");
                    break;


                case "FILE_DELETE_REQUEST":
                	System.out.println("in file delete");
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    pathName = (String) obj.get("pathName");
                    System.out.println("in  file delete");
                    if (this.fileSystemObserver.fileSystemManager.isSafePathName(pathName)) {
                        if (!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName)) {
                            this.fileSystemObserver.fileSystemManager.deleteFile(pathName, timestamp, md5);
                            send(JSON_process.FILE_DELETE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                        } else {
                            send(JSON_process.FILE_DELETE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.FILENAME_NOT_EXIST));
                        }
                    } else {
                        send(JSON_process.FILE_DELETE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.UNSAFE_PATH));
                    }
                    break;


                case "DIRECTORY_DELETE_REQUEST":
                	System.out.println("in directory delete");
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    pathName = (String) obj.get("pathName");
//                    File file = new File(pathName);
                    System.out.println("");
                    boolean successfully_deleted = this.fileSystemObserver.fileSystemManager.deleteDirectory(pathName);
                    System.out.println(successfully_deleted);
                    if (successfully_deleted) {
                        send(JSON_process.DIRECTORY_DELETE_RESPONSE(pathName, JSON_process.problems.NO_ERROR));
                    } else {
                        send(JSON_process.DIRECTORY_DELETE_RESPONSE(pathName,JSON_process.problems.DELETE_DIR_ERROR));
                    }
                    break;


                case "DIRECTORY_CREATE_REQUEST":
                	System.out.println("in directory create");
                    pathName = (String) obj.get("pathName");
                    System.out.println(pathName);
                    if (this.fileSystemObserver.fileSystemManager.makeDirectory(pathName)) {
                        send(JSON_process.DIRECTORY_CREATE_RESPONSE(pathName, JSON_process.problems.NO_ERROR));
                    } else {
                        send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.CREATE_DIR_ERROR));
                    }
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.CloseConnection();
        }

    }
}


//								String temp[]=pathName.split("\\\\");
//								String prefix=temp[temp.length-1];
//								String[] strArray = pathName.split("\\.");
//								int suffixIndex = strArray.length -1;
//								String suffix=strArray[suffixIndex];
//								File.createTempFile(prefix,suffix);