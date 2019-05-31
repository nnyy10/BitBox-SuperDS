package unimelb.bitbox;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import unimelb.bitbox.Encryption;

@SuppressWarnings("unchecked")

public class JSON_process {
    // this part is JSON translation
	
    public enum problems{
        NO_ERROR, CREATE_ERROR, UNSAFE_PATH, UNABLE_READ, DELETE_ERROR, FILE_EXISTS_WITH_MATCHING,
        MODIFY_ERROR, PATHNAME_NOT_EXIST, CREATE_DIR_ERROR,PATHNAME_EXISTS, DELETE_DIR_ERROR,FILENAME_NOT_EXIST,
        WRITE_NOT_COMPLETE,FILENAME_EXIST, UNKNOWN_PROBLEM
    }

    public static String INVALID_PROTOCOL(String invalidProtocolMessage){
        JSONObject obj = new JSONObject();
        obj.put("command", "INVALID_PROTOCOL");
        obj.put("message", invalidProtocolMessage);
        return obj.toString();
    }
    public static String CONNECTION_REFUSED(String [] host, int [] port){
        JSONObject obj = new JSONObject();
        obj.put("message",  "connection limit reached");
        obj.put("command", "CONNECTION_REFUSED");
        JSONArray list = new JSONArray();
        for (int i = 0; i< host.length;i++){
            JSONObject obj2 = new JSONObject();
            obj2.put("host", host[i]);
            obj2.put("port", port[i]);
            list.add(obj2);
        }
        obj.put("peers", list);
        return obj.toString();
    }
    public static String HANDSHAKE_REQUEST(String host, int port) {
        JSONObject obj = new JSONObject();
        obj.put("command", "HANDSHAKE_REQUEST");
        JSONObject obj2 = hostPort(host, port);
        obj.put("hostPort", obj2);
        return obj.toString();
    }
    public static String HANDSHAKE_RESPONSE(String host, int port){
        JSONObject obj = new JSONObject();
        obj.put("command", "HANDSHAKE_RESPONSE");
        obj.put("host", host);
        obj.put("port", port);
        JSONObject obj2 = hostPort(host, port);
        obj.put("hostPort", obj2);
        return obj.toString();
    }

    private static JSONObject hostPort(String host, int port) {
        JSONObject obj = new JSONObject();
        obj.put("host", host);
        obj.put("port", port);
        return obj;
    }

    private static void fileDescriptor(String md5, long timestamp, long size, String pathName, JSONObject obj) {
        obj.put("pathName", pathName);
        JSONObject obj2 = new JSONObject();
        obj2.put("md5", md5);
        obj2.put("lastModified", timestamp);
        obj2.put("fileSize", size);
        obj.put("fileDescriptor", obj2);
    }

    public static String FILE_CREATE_REQUEST(String md5, long timestamp, long size, String path){
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_CREATE_REQUEST");
        fileDescriptor(md5, timestamp, size, path, obj);
        return obj.toString();
    }

    public static String FILE_CREATE_RESPONSE(String md5, long timestamp, long size, String path, problems prob){
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
        } else{
            obj.put("message", "File create error");
            obj.put("status", false);
        }
        return obj.toString();
    }

    public static String FILE_BYTES_REQUEST(String md5, long timestamp, long size, String pathName,
                                          long position, long length){
        //needs to call the File System Manager to read the requested bytes and package them into a response message
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_BYTES_REQUEST");
        fileDescriptor(md5, timestamp, size, pathName, obj);
        obj.put("position", position);
        obj.put("length", length);
        return obj.toString();
    }

    public static String FILE_BYTES_RESPONSE(String md5, long timestamp, long size, String path,
                                           long position, long length, String content, problems prob){
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
        return obj.toString();
    }

    public static String FILE_DELETE_REQUEST(String md5, long timestamp, long size, String path){
        // there should be another edition for receiving severs
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_DELETE_REQUEST");
        fileDescriptor(md5, timestamp, size, path, obj);
        return obj.toString();
    }

    public static String FILE_DELETE_RESPONSE(String md5, long timestamp, long size, String path, problems prob){
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
        else if(prob == problems.FILENAME_NOT_EXIST){
            obj.put("message", "file doesn't exist");
            obj.put("status", false);
        } else {
            obj.put("message", "there was a problem deleting the file");
            obj.put("status", false);
        }
        return obj.toString();
    }

    public static String FILE_MODIFY_REQUEST(String md5, long timestamp, long size, String path){
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_MODIFY_REQUEST");
        fileDescriptor(md5, timestamp, size, path, obj);
        return obj.toString();
    }

    public static String FILE_MODIFY_RESPONSE(String md5, long timestamp, long size, String path, problems prob){
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_MODIFY_RESPONSE");
        fileDescriptor(md5, timestamp, size, path, obj);
        if(prob == problems.NO_ERROR){
            obj.put("message",   "File successfully modified");
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
        return obj.toString();
    }

    public static String DIRECTORY_CREATE_REQUEST(String path){
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_CREATE_REQUEST");
        obj.put("pathName", path);
        return obj.toString();
    }

    public static String DIRECTORY_CREATE_RESPONSE(String path, problems prob){
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_CREATE_RESPONSE");
        obj.put("pathName" ,path);
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
        return obj.toString();
    }

    public static String DIRECTORY_DELETE_REQUEST(String path){
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_DELETE_REQUEST");
        obj.put("pathName" , path);
        return obj.toString();
    }

    public static String DIRECTORY_DELETE_RESPONSE(String path, problems prob){
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_DELETE_RESPONSE");
        obj.put("pathName" , path); 
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
        return obj.toString();
    }

    public static String LIST_PEER_REQUEST(){
        JSONObject obj = new JSONObject();
        obj.put("command", "LIST_PEERS_REQUEST");
        return obj.toString();
    }

    public static String LIST_PEERS_RESPONSE(String [] host, int [] port){
        JSONObject obj = new JSONObject();
        obj.put("command", "LIST_PEERS_RESPONSE");
        JSONArray list = new JSONArray();
        for (int i = 0; i< host.length;i++){
            JSONObject obj2 = new JSONObject();
            obj2.put("host", host[i]);
            obj2.put("port", port[i]);
            list.add(obj2);
        }
        obj.put("peers", list);
        return obj.toString();
    }

    public static String AUTH_REQUEST(String publicKey){
        JSONObject obj = new JSONObject();
        obj.put("command", "AUTH_REQUEST");
        obj.put("identity", publicKey);
        return obj.toString();
    }

    public static String AUTH_RESPONSE(boolean status, String SecretKey){
        JSONObject obj = new JSONObject();
        obj.put("command", "AUTH_RESPONSE");
        if(status){
            obj.put("message", "public key found");
            obj.put("status", status);
            obj.put("AES128", SecretKey);
            //not like above one;
            // "AES128" : [BASE64 ENCODED, ENCRYPTED SECRET KEY]

            /**
             * how to solve null point Exception?
             */
        }
        else{
            obj.put("status", status);
            obj.put("message", "public key not found");
        }
        return obj.toString();
    }

    public static JSONObject Payload(String str){
        JSONObject obj = new JSONObject();
        obj.put("payload", str);
        return obj;
    }

    public static JSONObject DISCONNECT(){
        JSONObject obj = new JSONObject();
        obj.put("command", "DISCONNECTION");
        return obj;
    }
}