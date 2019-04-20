package unimelb.bitbox;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
@SuppressWarnings("unchecked")

public class JSON_process {
    // this part is JSON translation
    public enum problems{
        // ALL error may be meet
        NO_ERROR, CREATE_ERROR, UNSAFE_PATH, UNABLE_READ, DELETE_ERROR, FILE_EXISTS_WITH_MATCHING,
        MODIFY_ERROR, PATHNAME_NOT_EXIST, CREATE_DIR_ERROR,PATHNAME_EXISTS, DELETE_DIR_ERROR
    }
    // There exists a huge problem, which is the out-of-order messages or elements in JSON
    // using JSON-simple package

    public static JSONObject INVALID_PROTOCOL(){
        JSONObject obj = new JSONObject();
        obj.put("command", "INVALID_PROTOCOL");
        obj.put("message", "message must contain a command field as string");
        //System.out.println(obj);
        return obj;
    }
    public static JSONObject CONNECTION_REFUSED(String [] host, int [] port, int hostNumber){
        JSONObject obj = new JSONObject();
        // how to do a peer list ???? is it a list of pairs or two lists of hosts and ports
        obj.put("message",  "connection limit reached");
        obj.put("command", "CONNECTION_REFUSED");
        JSONArray list = new JSONArray();
        for (int i = 0; i< hostNumber;i++){
            JSONObject obj2 = new JSONObject();
            obj2.put("host", host[i]);
            obj2.put("port", port[i]);
            list.add(obj2);
        }
        /*JSONObject obj2 = new JSONObject();
        obj2.put("host", host);
        obj2.put("port", port);
        JSONObject obj3 = new JSONObject();
        obj3.put("host", host);
        obj3.put("port", port);
        list.add(obj2);
        list.add(obj3);*/

        obj.put("peers", list);
        //System.out.println(obj);
        return obj;
        //System.out.println(obj);
    }
    public static JSONObject HANDSHAKE_REQUEST(String host, int port) {
        JSONObject obj = new JSONObject();
        obj.put("command", "HANDSHAKE_REQUEST");
        //obj.put("host", host);
        //obj.put("port", port);
        JSONObject obj2 = hostPort(host, port);
        obj.put("hostPort", obj2);
        return obj;
    }
    public static JSONObject HANDSHAKE_RESPONSE(String host, int port){
        JSONObject obj = new JSONObject();
        obj.put("command", "HANDSHAKE_RESPONSE");
        obj.put("host", host);
        obj.put("port", port);
        JSONObject obj2 = hostPort(host, port);
        obj.put("hostPort", obj2);
        return obj;
    }

    private static JSONObject hostPort(String host, int port) {
        JSONObject obj = new JSONObject();
        obj.put("host", host);
        obj.put("port", port);
        return obj;
        //System.out.println(obj);
    }

    private static void fileDescriptor(String md5, String timestamp, long size, String pathName, JSONObject obj) {
        obj.put("pathName", pathName);
        JSONObject obj2 = new JSONObject();
        obj2.put("md5", md5);
        obj2.put("lastModified", timestamp);
        obj2.put("fileSize", size);
        obj.put("fileDescriptor", obj2);
        //System.out.println(obj);
    }

    public static JSONObject FILE_CREATE_REQUEST(String md5, String timestamp, long size, String path){
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_CREATE_REQUEST");
        // there should be a blocksize ????
        fileDescriptor(md5, timestamp, size, path, obj);
        //System.out.println(obj);
        return obj;
    }

    public static JSONObject FILE_CREATE_RESPONSE(String md5, String timestamp, long size, String path, problems prob){
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_CREATE_RESPONSE");
        fileDescriptor(md5, timestamp, size, path, obj);
        if (prob == problems.NO_ERROR){
            obj.put("message", "file loader ready");
            obj.put("status", true);
        }
        else if(prob == problems.CREATE_ERROR ){
            obj.put("message", "there was a problem creating the file");
            obj.put("status", false);
        }
        else if(prob == problems.UNSAFE_PATH){
            obj.put("message", "pathname already exists");
            obj.put("status", false);
        }
        // is this solution a good way to meet the requirements
        // what about "else"
        //System.out.println(obj);
        return obj;
    }

    public static JSONObject FILE_BYTES_REQUEST(String md5, String timestamp, long size, String pathName,
                                          int position, int length){
        //needs to call the File System Manager to read the requested bytes and package them into a response message
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_BYTES_REQUEST");
        fileDescriptor(md5, timestamp, size, pathName, obj);
        obj.put("position", position);
        obj.put("length", length);
        return obj;
        //System.out.println(obj);
    }

    public static JSONObject FILE_BYTES_RESPONSE(String md5, String timestamp, long size, String path,
                                           int position, int length, String content, problems prob){
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_BYTES_RESPONSE");
        fileDescriptor(md5, timestamp, size, path, obj);

        obj.put("position", position);
        obj.put("length", length);
        obj.put("content", content);
        if(prob == problems.NO_ERROR){
            obj.put("message",  "successful read");
            obj.put("status", true);
        }
        else if(prob == problems.UNABLE_READ){
            obj.put("message", "unsuccessful read");
            obj.put("status", false);
        }
        // is there need "else"
        return obj;
        //System.out.println(obj);
    }

    public static JSONObject FILE_DELETE_REQUEST(String md5, String timestamp, long size, String path){
        // there should be another edition for receiving severs
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_DELETE_REQUEST");
        fileDescriptor(md5, timestamp, size, path, obj);
        return obj;
        //System.out.println(obj);
    }

    public static JSONObject FILE_DELETE_RESPONSE(String md5, String timestamp, long size, String path, problems prob){
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_DELETE_RESPONSE");
        fileDescriptor(md5, timestamp, size, path, obj);

        if (prob == problems.NO_ERROR){
            obj.put("message",  "file deleted");
            obj.put("status", true);
        }
        else if(prob == problems.UNSAFE_PATH){
            obj.put("message",  "unsafe pathname given");
            obj.put("status", false);
        }
        else if (prob == problems.DELETE_ERROR){
            obj.put("message", "there was a problem deleting the file");
            obj.put("status", false);
        }
        return obj;
        //System.out.println(obj);
    }

    public static JSONObject FILE_MODIFY_REQUEST(String md5, String timestamp, long size, String path){
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_MODIFY_REQUEST");
        fileDescriptor(md5, timestamp, size, path, obj);
        return obj;
        //System.out.println(obj);
    }

    public static JSONObject FILE_MODIFY_RESPONSE(String md5, String timestamp, long size, String path, problems prob){
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_MODIFY_RESPONSE");
        fileDescriptor(md5, timestamp, size, path, obj);
        if(prob == problems.NO_ERROR){
            obj.put("message",   "file loader ready");
            obj.put("status", true);
        }
        else if(prob == problems.UNSAFE_PATH){
            obj.put("message",  "unsafe pathname given");
            obj.put("status", false);
        }
        else if (prob == problems.MODIFY_ERROR){
            obj.put("message",  "there was a problem modifying the file");
            obj.put("status", false);
        }
        else if (prob == problems.FILE_EXISTS_WITH_MATCHING){
            obj.put("message", "file already exists with matching content");
            obj.put("status", false);
        }
        else if (prob ==problems.PATHNAME_NOT_EXIST){
            obj.put("message",  "pathname does not exist");
            obj.put("status", false);
        }
        return obj;
    }

    public static JSONObject DIRECTORY_CREATE_REQUEST(String path){
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_CREATE_REQUEST");
        obj.put("pathName" ,"dir/subdir/" + path); // not sure about this.
        // this needs modify to original path + modify path
        return obj;
    }

    public static JSONObject DIRECTORY_CREATE_RESPONSE(String path, problems prob){
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_CREATE_RESPONSE");
        obj.put("pathName" ,"dir/subdir/"+path);
        // this needs modify to original path + modify path
        if (prob == problems.NO_ERROR){
            obj.put("message", "directory created");
            obj.put("status", true);
        }
        else if (prob == problems.UNSAFE_PATH){
            obj.put("message",  "unsafe pathname given");
            obj.put("status", false);
        }
        else if (prob == problems.CREATE_DIR_ERROR){
            obj.put("message", "there was a problem creating the directory");
            obj.put("status", false);
        }
        else if (prob == problems.PATHNAME_EXISTS){
            obj.put("message", "pathname already exists");
            obj.put("status", false);
        }
        return obj;
    }

    public static JSONObject DIRECTORY_DELETE_REQUEST(String path){
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_DELETE_REQUEST");
        obj.put("pathName" ,"dir/subdir/"+path); // not sure about this.
        // this needs modify to original path + modify path
        return obj;

    }

    public static JSONObject DIRECTORY_DELETE_RESPONSE(String path, problems prob){
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_DELETE_RESPONSE");
        obj.put("pathName" ,"dir/subdir/" + path); // not sure about this.
        // this needs modify to original path + modify path
        if (prob == problems.NO_ERROR){
            obj.put("message", "directory deleted");
            obj.put("status", true);
        }
        else if (prob == problems.UNSAFE_PATH){
            obj.put("message",  "unsafe pathname given");
            obj.put("status", false);
        }
        else if (prob == problems.PATHNAME_NOT_EXIST){
            obj.put("message",  "pathname does not exist");
            obj.put("status", false);
        }
        else if (prob == problems.DELETE_DIR_ERROR){
            obj.put("message", "there was a problem deleting the directory");
            obj.put("status", false);
        }
        return obj;
        //System.out.println(obj);
    }

    /*public enum Command{
        INVALID_PROTOCOL,CONNECTION_REFUSED, HANDSHAKE_REQUEST, HANDSHAKE_RESPONSE, FILE_CREATE_REQUEST,
        FILE_CREATE_RESPONSE, FILE_BYTES_REQUEST, FILE_BYTES_RESPONSE, FILE_DELETE_REQUEST, FILE_DELETE_RESPONSE,
        FILE_MODIFY_REQUEST, FILE_MODIFY_RESPONSE, DIRECTORY_CREATE_REQUEST, DIRECTORY_CREATE_RESPONSE,
        DIRECTORY_DELETE_REQUEST, DIRECTORY_DELETE_RESPONSE
    }*/


    // From this part, it is about get JSON message and transmit to java

    public static void getMessage(JSONObject obj){
        // first JSONObject need to be deal with, and then use obj as input
        String information = (String) obj.get("command");
        String md5 = " ", host = " ", msg = " ", timestamp = " ", pathName = " ";
        long size = 0;
        int port = 0 , position = 0, length = 0;
        JSONObject fileDescriptor;
        switch (information){
            case "INVALID_PROTOCOL":
                // something about socket instead of system
                //System.out.println(" ");
                msg = (String) obj.get("message");
                System.out.println(msg);
                break;
            case "CONNECTION_REFUSED":
                // socket output rather than system
                //System.out.println("  ");
                msg = (String) obj.get("message");
                System.out.println(msg);
                JSONArray data =(JSONArray) obj.get("peers");
                for (Object aData : data) {
                    JSONObject peer = (JSONObject) aData;
                    String sub_host = (String) peer.get("host");
                    int sub_port = (int) peer.get("port");
                    System.out.println("host: " + sub_host);
                    System.out.println("port: " + sub_port);
                }
                break;
            case "HANDSHAKE_REQUEST":
                JSONObject hostPort = (JSONObject) obj.get("hostPort");
                host = (String) hostPort.get("host");
                port = (int) hostPort.get("port");
                break;
            case "FILE_CREATE_REQUEST":
                fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                md5 = (String) fileDescriptor.get("md5");
                //System.out.println("md5: "+ md5);
                timestamp = (String) fileDescriptor.get("lastModified");
                size = (long) fileDescriptor.get("fileSize");
                pathName = (String) obj.get("pathName");
                break;
            case "FILE_BYTES_REQUEST":
                //do switch case work or do i need write in different functions
                fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                md5 = (String) fileDescriptor.get("md5");
                //System.out.println("md5: "+ md5);
                timestamp = (String) fileDescriptor.get("lastModified");
                size = (long) fileDescriptor.get("fileSize");
                pathName = (String) obj.get("pathName");
                position = (int) obj.get("position");
                length = (int) obj.get("length");
                break;
            case "FILE_DELETE_REQUEST":
                fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                md5 = (String) fileDescriptor.get("md5");
                //System.out.println("md5: "+ md5);
                timestamp = (String) fileDescriptor.get("lastModified");
                size = (long) fileDescriptor.get("fileSize");
                //position = (int) obj.get("position");
                pathName = (String) obj.get("pathName");
                break;
            case "FILE_MODIFY_REQUEST":
                fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                md5 = (String) fileDescriptor.get("md5");
                timestamp = (String) fileDescriptor.get("lastModified");
                size = (long) fileDescriptor.get("fileSize");
                pathName = (String) obj.get("pathName");
                break;
            case "DIRECTORY_CREATE_REQUEST":
                pathName = (String) obj.get("pathName");
                break;
            case "DIRECTORY_DELETE_REQUEST":
                pathName = (String) obj.get("pathName");
                break;


            case "HANDSHAKE_RESPONSE":
                JSONObject hostPort1 = (JSONObject) obj.get("hostPort");
                host = (String) hostPort1.get("host");
                port = (int) hostPort1.get("port");
                break;
            case "FILE_CREATE_RESPONSE":
                msg = (String) obj.get("message");
                System.out.println(msg);
                break;
            case "FILE_BYTES_RESPONSE":
                msg = (String) obj.get("message");
                System.out.println(msg);
                break;
            case "FILE_DELETE_RESPONSE":
                msg = (String) obj.get("message");
                System.out.println(msg);
                break;
            case "FILE_MODIFY_RESPONSE":
                msg = (String) obj.get("message");
                System.out.println(msg);
                break;
            case "DIRECTORY_CREATE_RESPONSE":
                msg = (String) obj.get("message");
                System.out.println(msg);
                break;
            case "DIRECTORY_DELETE_RESPONSE":
                msg = (String) obj.get("message");
                System.out.println(msg);
                break;

        }
        if(!host.equals(" ")){
            System.out.println("host: " + host);
        }
        if (size != 0) {
            System.out.println("size: " + size);
        }
        if (length != 0) {
            System.out.println("length: " + length);
        }
        if (port != 0) {
            System.out.println("port:" + port);
        }
        if (!md5.equals(" ")) {
            System.out.println("md5: " + md5);
        }
        if (position != 0) {
            System.out.println("position: " + position);
        }
        if(!timestamp.equals(" ")){
            System.out.println("timestamp: " + timestamp);
        }
        if (!pathName.equals(" ")) {
            System.out.println("pathName: " + pathName);
        }

    }


    public static void main(String [] args){
        // test
        String [] hosts = {" unimelb", "Unimelb"};
        int [] ports = {8111, 8112};
        String md5 = "074195d72c47315efae797b69393e5e5";
        String timestamp = "1553417607000";
        String pathName = "test.jpg";
        int position = 0;
        int length = 6;
        String path = "dir/subdir/etc";

        long size = 45787;
        JSONObject invalid_protocol = INVALID_PROTOCOL();
        JSONObject connection_refused = CONNECTION_REFUSED( hosts, ports,2);
        JSONObject handshake_request = HANDSHAKE_REQUEST("Unimelb", 8111);
        JSONObject handshake_response = HANDSHAKE_RESPONSE("Unimelb", 8111);
        //not finish due to too late.
        JSONObject file_create_response = FILE_CREATE_RESPONSE(md5, timestamp, size, pathName, problems.NO_ERROR);
        JSONObject file_create_request =FILE_CREATE_REQUEST(md5, timestamp, size, pathName);
        JSONObject file_bytes_request = FILE_BYTES_REQUEST(md5, timestamp, size, pathName, position, length);
        JSONObject  file_bytes_response = FILE_BYTES_RESPONSE(md5, timestamp, size, pathName, position, length,
                "aGVsbG8K", problems.UNABLE_READ);
        JSONObject file_delete_request = FILE_DELETE_REQUEST(md5, timestamp, size,pathName);
        JSONObject file_delete_response =FILE_DELETE_RESPONSE(md5, timestamp, size, pathName, problems.UNSAFE_PATH);
        JSONObject file_modify_request = FILE_MODIFY_REQUEST(md5,timestamp,size,pathName);
        JSONObject file_modify_response = FILE_MODIFY_RESPONSE(md5,timestamp,size,pathName,problems.MODIFY_ERROR);
        JSONObject directory_create_request = DIRECTORY_CREATE_REQUEST(path);
        JSONObject directory_delete_request = DIRECTORY_DELETE_REQUEST(path);
        JSONObject directory_create_response= DIRECTORY_CREATE_RESPONSE(path, problems.NO_ERROR);
        JSONObject directory_delete_response = DIRECTORY_DELETE_RESPONSE(path, problems.PATHNAME_NOT_EXIST);

        //followed parts have passed test
        System.out.println("invalid_protocol: ");
        getMessage(invalid_protocol);
        System.out.println("connection_refused: ");
        getMessage(connection_refused);
        System.out.println("handshake_request: ");
        getMessage(handshake_request);
        System.out.println("handshake_response: ");
        getMessage(handshake_response);
        System.out.println("file_create_response: ");
        getMessage(file_create_response);
        System.out.println("file_create_request: ");
        getMessage(file_create_request);
        System.out.println("file_bytes_request: ");
        getMessage(file_bytes_request);
        System.out.println("file_bytes_response: ");
        getMessage(file_bytes_response);
        System.out.println("file_delete_request: ");
        getMessage(file_delete_request);
        System.out.println("file_delete_response: ");
        getMessage(file_delete_response);
        System.out.println("file_modify_request: ");
        getMessage(file_modify_request);
        System.out.println("file_modify_response: ");
        getMessage(file_modify_response);
        System.out.println("directory_create_request: ");
        getMessage(directory_create_request);
        System.out.println("directory_delete_request: ");
        getMessage(directory_delete_request);
        System.out.println("directory_create_response: ");
        getMessage(directory_create_response);
        System.out.println("directory_delete_response: ");
        getMessage(directory_delete_response);





























    }
}