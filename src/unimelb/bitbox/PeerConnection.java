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
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;;


public class PeerConnection implements Runnable {

	private static Logger log = Logger.getLogger(PeerConnection.class.getName());
	
    protected Socket socket = null;
    protected BufferedReader inputStream = null;
    protected BufferedWriter outputStream = null;
    protected ServerMain fileSystemObserver = null;
    protected boolean ErrorEncountered = false;
    protected ScheduledExecutorService exec = null;
    
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
    	exec.shutdown();
    	ErrorEncountered = true;
        log.info("Closing Connection");
        this.fileSystemObserver.remove(this);
        try {
            this.inputStream.close();
        } catch (Exception e) {}
        try {
            this.outputStream.close();
        } catch (Exception e) {}
        try {
            this.socket.close();
        } catch (Exception e) {}
    }

    public void run() {
        String line = "";

        int synTime = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));

        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
            for (FileSystemEvent event : this.fileSystemObserver.fileSystemManager.generateSyncEvents()) {
                String syn;
                syn = ServerMain.getInstance().FileSystemEventToJSON(event);
                send(syn);
            }
        }, 0, synTime, TimeUnit.SECONDS);

        while (true) {
            try {
                line = inputStream.readLine();
                log.info("Recieved Message is: " + line);
                if (line != null) {
                    handleMessage(line);
                } else {
                    log.info("The recieved message is null, closing connection.");
                    this.CloseConnection();
                    break;
                }
                if(this.ErrorEncountered)
                	break;
            } catch (Exception e) {
                this.CloseConnection();
                break;
            }
        }
        this.CloseConnection();
    }

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

    public void handleMessage(String str) {
        JSONParser parser = new JSONParser();

        try {
            String md5 = "", host = "", msg = "", pathName = "", content = "";
            long size = 0;
            long port = 0;
            long position = 0;
            long length = Long.parseLong(Configuration.getConfigurationValue("blockSize").trim());
            long timestamp = 0;
            boolean status;
            JSONObject obj = (JSONObject) parser.parse(str);
            JSONObject fileDescriptor;
            String command = (String) obj.get("command");
            log.info("Peer recieved command: " + command);
            
            switch (command) {
                case "FILE_CREATE_REQUEST":
                    System.out.println("in file create");
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    size = (long) fileDescriptor.get("fileSize");
                    pathName = (String) obj.get("pathName");

                    if (this.fileSystemObserver.fileSystemManager.isSafePathName(pathName)) {
                        if (!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName)) {
                            try {
                                this.fileSystemObserver.fileSystemManager.createFileLoader(pathName, md5, size, timestamp);
                                if (!this.fileSystemObserver.fileSystemManager.checkShortcut(pathName)) {
                                    long readLength;
                                    if (size==0){
                                    	this.fileSystemObserver.fileSystemManager.checkWriteComplete(pathName);
                                    	break;
                                    }
                                    else if (size <= length)
                                        readLength = size;
                                    else
                                        readLength = length;
                                    send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, 0, readLength));
                                } else {
                                    this.fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
                                    send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                                }
                            } catch (Exception e) {
                            	log.warning("file create failed");
                            	this.CloseConnection();
                            	break;
                            }
                        } else if (!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName, md5)) {
                            try {
                                boolean modify_fileloader = this.fileSystemObserver.fileSystemManager.modifyFileLoader(pathName, md5, timestamp);
                                if(!modify_fileloader)
                                    break;
                                if (!this.fileSystemObserver.fileSystemManager.checkShortcut(pathName)) {
                                    long readLength;
                                    if (size <= length)
                                        readLength = size;
                                    else
                                        readLength = length;
                                    send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, 0, readLength));
                                } else {
                                    this.fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
                                    send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                                }
                            } catch (Exception e) {
                            	log.warning("file create failed");
                            	this.CloseConnection();
                            	break;
                            }
                        } else{
                        	send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
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
                    	if(!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName)) {
                        	try {
                                this.fileSystemObserver.fileSystemManager.createFileLoader(pathName, md5, size, timestamp);
                                if (!this.fileSystemObserver.fileSystemManager.checkShortcut(pathName)) {
                                    long readLength;
                                    if (size==0){
                                    	this.fileSystemObserver.fileSystemManager.checkWriteComplete(pathName);
                                    	break;
                                    }
                                    else if (size <= length)
                                        readLength = size;
                                    else
                                        readLength = length;
                                    send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, 0, readLength));
                                } else {
                                    this.fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
                                    send(JSON_process.FILE_MODIFY_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                                }
                            } catch (Exception e) {
                            	log.warning("file create failed");
                            	this.CloseConnection();
                            	break;
                            }
                        }else if (!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName, md5)) {
                            try {
                                boolean modify_fileloader = this.fileSystemObserver.fileSystemManager.modifyFileLoader(pathName, md5, timestamp);
                                System.out.println("modify loader created:" + modify_fileloader);
                                if (!this.fileSystemObserver.fileSystemManager.checkShortcut(pathName)) {
                                    long readLength;
                                    if (size <= length)
                                        readLength = size;
                                    else
                                        readLength = length;
                                    System.out.println("file_byte request sent");
                                    send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, 0, readLength));
                                } else {
                                    this.fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
                                    System.out.println("file modify response sent1");
                                    send(JSON_process.FILE_MODIFY_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                                }
                            } catch (Exception e) {
                            	log.warning("file modify failed");
                            	this.CloseConnection();
                            	break;
                            }
                        }
                        else{
                        	send(JSON_process.FILE_MODIFY_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                        }
                    } else {
                        send(JSON_process.FILE_MODIFY_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.UNSAFE_PATH));
                    }
                    break;
                case "FILE_BYTES_RESPONSE":
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    size = (long) fileDescriptor.get("fileSize");
                    pathName = (String) obj.get("pathName");
                    content = (String) obj.get("content");
                    position = (long) obj.get("position");
                    ByteBuffer src = ByteBuffer.wrap(java.util.Base64.getDecoder().decode(content));
                    Boolean write_file = this.fileSystemObserver.fileSystemManager.writeFile(pathName, src, position);
                    if (!this.fileSystemObserver.fileSystemManager.checkWriteComplete(pathName)) {
                        System.out.println("file check NOT complete:" + pathName);
                        long readLength;
                        if (position + length + length <= size)
                            readLength = length;
                        else
                            readLength = size - (position + length + length);
                        send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, position + length, readLength));
                    } else {
                        System.out.println("file check already complete:" + pathName);
                        boolean cancel_fileloader = fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
                        if (cancel_fileloader) 
                            System.out.println("cancel file loader sucessfull");
                        send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                    }
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
                    if(position + length <= size){
	                    byte[] byteContent = fileSystemObserver.fileSystemManager.readFile(md5, position, length).array();
	                    content = java.util.Base64.getEncoder().encodeToString(byteContent);
	                    String fileBytesResponse = JSON_process.FILE_BYTES_RESPONSE(md5, timestamp, size, pathName, position, length, content, JSON_process.problems.NO_ERROR);
	                    send(fileBytesResponse);
	                    System.out.println("response sent: " + fileBytesResponse);
                    }else{
                    	log.warning("The read length is bigger than file size, closing connection");
                    	this.CloseConnection();
                    }
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
                            Boolean wether_ldelete = this.fileSystemObserver.fileSystemManager.deleteFile(pathName, timestamp, md5);
                            System.out.println("wheather delete:" + wether_ldelete);
                            send(JSON_process.FILE_DELETE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                        } else 
                            send(JSON_process.FILE_DELETE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.FILENAME_NOT_EXIST));
                    } else 
                        send(JSON_process.FILE_DELETE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.UNSAFE_PATH));
                    break;
                case "DIRECTORY_CREATE_REQUEST":
                    System.out.println("in directory create");
                    pathName = (String) obj.get("pathName");
                    System.out.println("dictonary creat pathname:" + pathName);
                    if (this.fileSystemObserver.fileSystemManager.makeDirectory(pathName)) 
                        send(JSON_process.DIRECTORY_CREATE_RESPONSE(pathName, JSON_process.problems.NO_ERROR));
                    else 
                        send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.CREATE_DIR_ERROR));
                    break;
                case "DIRECTORY_DELETE_REQUEST":
                    pathName = (String) obj.get("pathName");
                    boolean successfully_deleted = this.fileSystemObserver.fileSystemManager.deleteDirectory(pathName);
                    System.out.println(successfully_deleted);
                    if (successfully_deleted) 
                        send(JSON_process.DIRECTORY_DELETE_RESPONSE(pathName, JSON_process.problems.NO_ERROR));
                    else 
                        send(JSON_process.DIRECTORY_DELETE_RESPONSE(pathName, JSON_process.problems.DELETE_DIR_ERROR));
                    break;
                case "FILE_CREATE_RESPONSE":
                    msg = (String) obj.get("message");
                    status = (boolean) obj.get("status");
                    pathName = (String) obj.get("pathName");
                    log.info("FILE_CREATE_RESPONSE path: " + pathName);
                    log.info("FILE_CREATE_RESPONSE message: " + msg);
                    log.info("FILE_CREATE_RESPONSE status: " + status);
                    break;
                case "FILE_DELETE_RESPONSE":
                    msg = (String) obj.get("message");
                    status = (boolean) obj.get("status");
                    pathName = (String) obj.get("pathName");
                    log.info("FILE_DELETE_RESPONSE path: " + pathName);
                    log.info("FILE_DELETE_RESPONSE message: " + msg);
                    log.info("FILE_DELETE_RESPONSE status: " + status);
                    break;
                case "FILE_MODIFY_RESPONSE":
                    msg = (String) obj.get("message");
                    status = (boolean) obj.get("status");
                    pathName = (String) obj.get("pathName");
                    log.info("FILE_MODIFY_RESPONSE path: " + pathName);
                    log.info("FILE_MODIFY_RESPONSE message: " + msg);
                    log.info("FILE_MODIFY_RESPONSE delete response status: " + status);
                    break;
                case "DIRECTORY_CREATE_RESPONSE":
                    msg = (String) obj.get("message");
                    status = (boolean) obj.get("status");
                    pathName = (String) obj.get("pathName");
                    log.info("DIRECTORY_CREATE_RESPONSE path: " + pathName);
                    log.info("DIRECTORY_CREATE_RESPONSE message: " + msg);
                    log.info("DIRECTORY_CREATE_RESPONSE status: " + status);
                    break;
                case "DIRECTORY_DELETE_RESPONSE":
                    msg = (String) obj.get("message");
                    status = (boolean) obj.get("status");
                    pathName = (String) obj.get("pathName");
                    log.info("DIRECTORY_DELETE_RESPONSE path: " + pathName);
                    log.info("DIRECTORY_DELETE_RESPONSE message: " + msg);
                    log.info("Directory delete response status: " + status);
                    break;
                case "INVALID_PROTOCOL":
                	log.warning("Invalid protol recieved, closing connection");
                	CloseConnection();
                	break;
                default:
                    send(JSON_process.INVALID_PROTOCOL("Invalid command recieved, closing connection"));
                    break;
            }
        } catch (Exception e) {
            log.warning("Error encountered parsing json");
            this.CloseConnection();
        }

    }
}
