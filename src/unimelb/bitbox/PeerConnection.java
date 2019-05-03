package unimelb.bitbox;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


import unimelb.bitbox.util.Configuration;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.Base64;
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
                if(line != null){
                	implement(line);
                	
                }else{
                	System.out.println("The recieved message is null, closing connection.");
                	this.CloseConnection();
                	break;
                }
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
            long port = 0;
            long position = 0;
            long length = Long.parseLong(Configuration.getConfigurationValue("blockSize").trim());
            long timestamp = 0;
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
                            try {
                                this.fileSystemObserver.fileSystemManager.createFileLoader(pathName, md5, size, timestamp);
                                boolean creat_fileloader=this.fileSystemObserver.fileSystemManager.createFileLoader(pathName, md5, size, timestamp);
                            	if(creat_fileloader)
                                {
                                    System.out.println("successful create file loader");
                                }

                                if (!this.fileSystemObserver.fileSystemManager.checkShortcut(pathName)) {
                                	
                        			long readLength;
                        			if(size <= length)
                        				readLength = size;
                        			else
                        				readLength = length;
                                    send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, 0, readLength));
                                }
                                 else{
                                	 this.fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
                                     send(JSON_process.FILE_CREATE_RESPONSE(md5,timestamp,size,pathName,JSON_process.problems.NO_ERROR));
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
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    size = (long) fileDescriptor.get("fileSize");
                    pathName = (String) obj.get("pathName");
                    content = (String) obj.get("content");
                    position = (long) obj.get("position");
                    ByteBuffer src = ByteBuffer.wrap(java.util.Base64.getDecoder().decode(content));
                    this.fileSystemObserver.fileSystemManager.writeFile(pathName, src, position);
                    
                    if (!this.fileSystemObserver.fileSystemManager.checkWriteComplete(pathName)) {
                        System.out.println("file check NOT complete:" + pathName);
            			long readLength;
            			if(position + length + length <= size)
            				readLength = length;
            			else
            				readLength =size-position +length+length;
                        send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, position+length, readLength));
                    } else {
                        System.out.println("file check already complete:" + pathName);
                       boolean cancel_fileloader=fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
                       if(cancel_fileloader){
                           System.out.println("cancel file loader sucessfull");
                       }
                       send(JSON_process.FILE_CREATE_RESPONSE(md5,timestamp,size,pathName,JSON_process.problems.NO_ERROR));
                    }
                        // is there need a "else" to send back a successful response?
                    break;


                case "FILE_BYTES_REQUEST":
                	System.out.println("send file bytes request :in file bytes");
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    size = (long) fileDescriptor.get("fileSize");
                    pathName = (String) obj.get("pathName");
                    length = (long) obj.get("length");

                    position = (long) obj.get("position");
                	System.out.println("message not recieved correctly" + md5);
                	System.out.println("message not recieved correctly" + length);
                	System.out.println("message not recieved correctly" + position);

//                	System.out.println("readfile_content:"+Base64.getDecoder().decode(fileSystemObserver.fileSystemManager.readFile(md5,position,length).array()));
                	byte[] byteContent = fileSystemObserver.fileSystemManager.readFile(md5,position,length).array();
                    content = java.util.Base64.getEncoder().encodeToString(byteContent);
                    String fileBytesResponse = JSON_process.FILE_BYTES_RESPONSE(md5,timestamp,size,pathName,position,length,content,JSON_process.problems.NO_ERROR);
                    send(fileBytesResponse);
                    System.out.println("response sent: " + fileBytesResponse);
                    break;


                case "FILE_DELETE_REQUEST":
                	System.out.println("in file delete");
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    pathName = (String) obj.get("pathName");
                    System.out.println("in  file delete");
                    if (this.fileSystemObserver.fileSystemManager.isSafePathName(pathName)) {
                        if (this.fileSystemObserver.fileSystemManager.fileNameExists(pathName)) {
                            Boolean wether_ldelete=this.fileSystemObserver.fileSystemManager.deleteFile(pathName, timestamp, md5);
                            System.out.println("wheather delete:"+wether_ldelete);
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
//                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
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
                    System.out.println("dictonary creat pathname:"+pathName);
                    if (this.fileSystemObserver.fileSystemManager.makeDirectory(pathName)) {
                        send(JSON_process.DIRECTORY_CREATE_RESPONSE(pathName, JSON_process.problems.NO_ERROR));
                    } else {
                        send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.CREATE_DIR_ERROR));
                    }
                break;

                default:
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