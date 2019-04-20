package unimelb.bitbox;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;

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
    public static JSONObject CONNECTION_REFUSED(String host, int port){
        JSONObject obj = new JSONObject();
        // how to do a peer list ????
        obj.put("message",  "connection limit reached");
        obj.put("command", "CONNECTION_REFUSED");
        JSONArray list = new JSONArray();
        JSONObject obj2 = new JSONObject();
        obj2.put("host", host);
        obj2.put("port", port);
        JSONObject obj3 = new JSONObject();
        obj3.put("host", host);
        obj3.put("port", port);
        list.add(obj2);
        list.add(obj3);
        obj.put("peer", list);
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
        int port = 0 , postion = 0, length = 0;
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
                JSONArray data =(JSONArray) obj.get("peer");
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
                JSONObject fileDescriptor = (JSONObject) obj.get("fileDescriptor");
                md5 = (String) fileDescriptor.get("md5");
                //System.out.println("md5: "+ md5);
                timestamp = (String) fileDescriptor.get("lastModified");
                size = (long) fileDescriptor.get("fileSize");
                pathName = (String) obj.get("pathName");
                break;
            case "FILE_BYTES_REQUEST":
                //do switch case work or do i need write in different functions
                md5 = (String) obj.get("md5");
                timestamp = (String) obj.get("lastModified");
                size = (long) obj.get("fileSize");
                pathName = (String) obj.get("pathName");
                postion = (int) obj.get("position");
                length = (int) obj.get("length");
                break;
            case "FILE_DELETE_REQUEST":
                md5 = (String) obj.get("md5");
                timestamp = (String) obj.get("lastModified");
                size = (long) obj.get("fileSize");
                pathName = (String) obj.get("pathName");
                postion = (int) obj.get("position");
                break;
            case "FILE_MODIFY_REQUEST":
                md5 = (String) obj.get("md5");
                timestamp = (String) obj.get("lastModified");
                size = (long) obj.get("fileSize");
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
        if (postion != 0) {
            System.out.println("postion: " + postion);
        }
        if(!timestamp.equals(" ")){
            System.out.println("timestamp: " + Integer.parseInt(timestamp));
        }
        if (!pathName.equals(" ")) {
            System.out.println("pathName: " + pathName);
        }

    }


    public static void main(String [] args){
        // test
        JSONObject obj1 = INVALID_PROTOCOL();
        JSONObject obj2 = CONNECTION_REFUSED("Unimelb", 8111);
        JSONObject obj3 = HANDSHAKE_REQUEST("Unimelb", 8111);
        JSONObject obj4 = HANDSHAKE_RESPONSE("Unimelb", 8111);
        //not finish due to too late.
        JSONObject obj = FILE_CREATE_RESPONSE("074195d72c47315efae797b69393e5e5",
                "1553417607000",  45787, "test.jpg", problems.NO_ERROR);
        //System.out.println(obj);
        System.out.println("1:");
        getMessage(obj);
        System.out.println("2:");
        getMessage(obj1);
        System.out.println("3:");
        getMessage(obj2);
        System.out.println("4:");
        getMessage(obj3);
        System.out.println("5:");
        getMessage(obj4);


    }
}