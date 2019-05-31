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
import java.net.*;
import java.io.File;
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
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;;import javax.management.modelmbean.ModelMBean;


public abstract class PeerConnection {

    protected String remoteAddress;
    protected int remotePort;

    public String getAddr(){
        return remoteAddress;
    }

    public int getPort(){
        return remotePort;
    }

    protected static Logger log = Logger.getLogger(PeerConnection.class.getName());

    protected ServerMain fileSystemObserver = null;

    public abstract void send(String s);

    protected abstract void CloseConnection();

    public void handleMessage(String str) {
        JSONParser parser =  new JSONParser();

        try {
            String md5 = "", pathName = "", content = "";
            long size = 0;
            long position = 0;
            long length = Long.parseLong(Configuration.getConfigurationValue("blockSize").trim()) - 2500;
            long timestamp = 0;
            JSONObject obj = (JSONObject) parser.parse(str);
            JSONObject fileDescriptor;
            String command = (String) obj.get("command");
            log.info("Peer recieved command: " + command);

            switch (command) {
                case "FILE_CREATE_REQUEST":
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    size = (long) fileDescriptor.get("fileSize");
                    pathName = (String) obj.get("pathName");

                    if (this.fileSystemObserver.fileSystemManager.isSafePathName(pathName)) {
                        if (!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName)) {
                            try {
                                if(!fileSystemObserver.fileSystemManager.createFileLoader(pathName, md5, size, timestamp)){
                                	send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.UNKNOWN_PROBLEM));
                                	break;
                                }
                                if (!this.fileSystemObserver.fileSystemManager.checkShortcut(pathName)) {
                                    long readLength;
                                    if (size==0){
                                    	send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                                    	fileSystemObserver.fileSystemManager.checkWriteComplete(pathName);
                                    	break;
                                    }
                                    else if (size <= length)
                                        readLength = size;
                                    else
                                        readLength = length;
                                    send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
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
                                if(!fileSystemObserver.fileSystemManager.modifyFileLoader(pathName, md5, timestamp)){
                                	send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.UNKNOWN_PROBLEM));
                                    break;
                                }
                                if (!this.fileSystemObserver.fileSystemManager.checkShortcut(pathName)) {
                                    long readLength;
                                    if (size <= length)
                                        readLength = size;
                                    else
                                        readLength = length;
                                    send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
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
                    
                    if (this.fileSystemObserver.fileSystemManager.isSafePathName(pathName)) {
                    	if(!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName)) {
                        	try {
                                if(!this.fileSystemObserver.fileSystemManager.createFileLoader(pathName, md5, size, timestamp)){
                                	send(JSON_process.FILE_MODIFY_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.UNKNOWN_PROBLEM));
                                	break;
                                }
                                if (!this.fileSystemObserver.fileSystemManager.checkShortcut(pathName)) {
                                    long readLength;
                                    if (size==0){
                                    	send(JSON_process.FILE_MODIFY_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                                    	this.fileSystemObserver.fileSystemManager.checkWriteComplete(pathName);
                                    	break;
                                    }
                                    else if (size <= length)
                                        readLength = size;
                                    else
                                        readLength = length;
                                    send(JSON_process.FILE_MODIFY_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                                    send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, 0, readLength));
                                } else {
                                    fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
                                    send(JSON_process.FILE_MODIFY_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                                }
                            } catch (Exception e) {
                            	log.warning("file modify failed");
                            	this.CloseConnection();
                            	break;
                            }
                        }else if (!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName, md5)) {
                            try {
                                if(!fileSystemObserver.fileSystemManager.modifyFileLoader(pathName, md5, timestamp))
                                    break;
                                if (!this.fileSystemObserver.fileSystemManager.checkShortcut(pathName)) {
                                    long readLength;
                                    if (size <= length)
                                        readLength = size;
                                    else
                                        readLength = length;
                                    send(JSON_process.FILE_MODIFY_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                                    send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, 0, readLength));
                                } else {
                                    this.fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
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
                        long readLength;
                        if (position + length + length <= size)
                            readLength = length;
                        else
                            readLength = size - (position + length);
                        send(JSON_process.FILE_BYTES_REQUEST(md5, timestamp, size, pathName, position + length, readLength));
                    } else
                    	log.info("File transfer complete");
                    break;
                case "FILE_BYTES_REQUEST":
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
                    }else{
                    	log.warning("The read length is bigger than file size, closing connection");
                    	send(JSON_process.INVALID_PROTOCOL("Position + length bigger than file size invalid, closing connection"));
                    	CloseConnection();
                    }
                	break;
                case "FILE_DELETE_REQUEST":
                    fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                    md5 = (String) fileDescriptor.get("md5");
                    timestamp = (long) fileDescriptor.get("lastModified");
                    pathName = (String) obj.get("pathName");
                    if (fileSystemObserver.fileSystemManager.isSafePathName(pathName)) {
                        if (fileSystemObserver.fileSystemManager.fileNameExists(pathName)) {
                            if(fileSystemObserver.fileSystemManager.deleteFile(pathName, timestamp, md5))
                            	send(JSON_process.FILE_DELETE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.NO_ERROR));
                            else
                            	send(JSON_process.FILE_DELETE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.DELETE_ERROR));
                        } else 
                            send(JSON_process.FILE_DELETE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.FILENAME_NOT_EXIST));
                    } else 
                        send(JSON_process.FILE_DELETE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.UNSAFE_PATH));
                    break;
                case "DIRECTORY_CREATE_REQUEST":
                    pathName = (String) obj.get("pathName");
                    if (fileSystemObserver.fileSystemManager.makeDirectory(pathName)) 
                        send(JSON_process.DIRECTORY_CREATE_RESPONSE(pathName, JSON_process.problems.NO_ERROR));
                    else 
                        send(JSON_process.FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, JSON_process.problems.CREATE_DIR_ERROR));
                    break;
                case "DIRECTORY_DELETE_REQUEST":
                    pathName = (String) obj.get("pathName");
                    if (fileSystemObserver.fileSystemManager.deleteDirectory(pathName)) 
                        send(JSON_process.DIRECTORY_DELETE_RESPONSE(pathName, JSON_process.problems.NO_ERROR));
                    else 
                        send(JSON_process.DIRECTORY_DELETE_RESPONSE(pathName, JSON_process.problems.DELETE_DIR_ERROR));
                    break;
                case "FILE_CREATE_RESPONSE":
                    log.info("FILE_CREATE_RESPONSE: " + str);
                    break;
                case "FILE_DELETE_RESPONSE":
                    log.info("FILE_DELETE_RESPONSE: " + str);
                    break;
                case "FILE_MODIFY_RESPONSE":
                    log.info("FILE_MODIFY_RESPONSE: " + str);
                    break;
                case "DIRECTORY_CREATE_RESPONSE":
                	log.info("DIRECTORY_CREATE_RESPONSE: " + str);
                    break;
                case "DIRECTORY_DELETE_RESPONSE":
                	log.info("DIRECTORY_DELETE_RESPONSE: " + str);
                    break;
                case "INVALID_PROTOCOL":
                	log.warning("Invalid protol recieved, closing connection");
                	CloseConnection();
                	break;
                default:
                	log.warning("Recieved command is invalid, closing connection.");
                    send(JSON_process.INVALID_PROTOCOL("Invalid command recieved, closing connection"));
                    CloseConnection();
                    break;
            }
        } catch (Exception e) {
            log.warning("Error encountered parsing json");
            send(JSON_process.INVALID_PROTOCOL("Invalid protocol recieved, closing connection"));
            CloseConnection();
        }

    }
}
