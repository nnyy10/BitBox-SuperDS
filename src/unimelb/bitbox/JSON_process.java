package unimelb.bitbox;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")

public class JSON_process {
    // this part is JSON translation

    public enum problems {
        NO_ERROR, CREATE_ERROR, UNSAFE_PATH, UNABLE_READ, DELETE_ERROR, FILE_EXISTS_WITH_MATCHING,
        MODIFY_ERROR, PATHNAME_NOT_EXIST, CREATE_DIR_ERROR, PATHNAME_EXISTS, DELETE_DIR_ERROR, FILENAME_NOT_EXIST,
        WRITE_NOT_COMPLETE, FILENAME_EXIST, UNKNOWN_PROBLEM
    }

    public static String GENERATE_RESPONSE_MSG(String JSONmsg) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject OriginMsg = (JSONObject) parser.parse(JSONmsg);
            String command = (String) OriginMsg.get("command");
            if (command.contains("REQUEST")) {
                command = command.replace("REQUEST", "RESPONSE");
                OriginMsg.remove("command");
                OriginMsg.put("command", command);
            }
            return OriginMsg.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean RESPONSE_EQUALS(String response1, String response2) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject Response1 = (JSONObject) parser.parse(response1);
            JSONObject Response2 = (JSONObject) parser.parse(response2);
            String cmd1 = (String) Response1.get("command");
            String cmd2 = (String) Response2.get("command");
            if (cmd1.contains("RESPONSE") && cmd2.contains("RESPONSE")) {
                if (cmd1.equals(cmd2)) {
                    if (cmd1.contains("FILE")) {
                        JSONObject fD1, fD2;
                        fD1 = (JSONObject) Response1.get("fileDescriptor");
                        fD2 = (JSONObject) Response2.get("fileDescriptor");
                        if (!Response1.get("pathName").equals(Response2.get("pathName")))
                            return false;
                        if (!fD1.get("md5").equals(fD2.get("md5")))
                            return false;
                        if (!fD1.get("lastModified").equals(fD2.get("lastModified")))
                            return false;
                        if (!fD1.get("fileSize").equals(fD2.get("fileSize")))
                            return false;
                        if (cmd1.equals("FILE_BYTE_REQUEST") && cmd2.equals("FILE_BYTE_REQUEST")) {
                            if (!Response1.get("length").equals(Response2.get("length")))
                                return false;
                            if (!Response1.get("position").equals(Response2.get("position")))
                                return false;
                        }
                        return true;
                    } else if (cmd1.contains("DIRECTORY")) {
                        return Response1.get("pathName").equals(Response2.get("pathName"));
                    } else return cmd1.contains("HANDSHAKE");
                } return false;
            } else return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String INVALID_PROTOCOL(String invalidProtocolMessage) {
        JSONObject obj = new JSONObject();
        obj.put("command", "INVALID_PROTOCOL");
        obj.put("message", invalidProtocolMessage);
        return obj.toString();
    }

    public static String CONNECTION_REFUSED(String[] host, int[] port) {
        JSONObject obj = new JSONObject();
        obj.put("message", "connection limit reached");
        obj.put("command", "CONNECTION_REFUSED");
        JSONArray list = new JSONArray();
        if (host != null && port != null) {
            for (int i = 0; i < host.length; i++) {
                JSONObject obj2 = new JSONObject();
                obj2.put("host", host[i]);
                obj2.put("port", port[i]);
                list.add(obj2);
            }
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

    public static String HANDSHAKE_RESPONSE(String host, int port) {
        JSONObject obj = new JSONObject();
        obj.put("command", "HANDSHAKE_RESPONSE");
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

    public static String FILE_CREATE_REQUEST(String md5, long timestamp, long size, String path) {
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_CREATE_REQUEST");
        fileDescriptor(md5, timestamp, size, path, obj);
        return obj.toString();
    }

    public static String FILE_CREATE_RESPONSE(String md5, long timestamp, long size, String path, problems prob) {
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_CREATE_RESPONSE");
        fileDescriptor(md5, timestamp, size, path, obj);
        if (prob == problems.NO_ERROR) {
            obj.put("message", "file loader ready");
            obj.put("status", true);
        } else if (prob == problems.CREATE_ERROR) {
            obj.put("message", "there was a problem creating the file");
            obj.put("status", false);
        } else if (prob == problems.UNSAFE_PATH) {
            obj.put("message", "pathname already exists");
            obj.put("status", false);
        } else {
            obj.put("message", "File create error");
            obj.put("status", false);
        }
        return obj.toString();
    }

    public static String FILE_BYTES_REQUEST(String md5, long timestamp, long size, String pathName,
                                            long position, long length) {
        //needs to call the File System Manager to read the requested bytes and package them into a response message
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_BYTES_REQUEST");
        fileDescriptor(md5, timestamp, size, pathName, obj);
        obj.put("position", position);
        obj.put("length", length);
        return obj.toString();
    }

    public static String FILE_BYTES_RESPONSE(String md5, long timestamp, long size, String path,
                                             long position, long length, String content, problems prob) {
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_BYTES_RESPONSE");
        fileDescriptor(md5, timestamp, size, path, obj);

        obj.put("position", position);
        obj.put("length", length);
        obj.put("content", content);
        if (prob == problems.NO_ERROR) {
            obj.put("message", "successful read");
            obj.put("status", true);
        } else if (prob == problems.UNABLE_READ) {
            obj.put("message", "unsuccessful read");
            obj.put("status", false);
        }
        return obj.toString();
    }

    public static String FILE_DELETE_REQUEST(String md5, long timestamp, long size, String path) {
        // there should be another edition for receiving severs
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_DELETE_REQUEST");
        fileDescriptor(md5, timestamp, size, path, obj);
        return obj.toString();
    }

    public static String FILE_DELETE_RESPONSE(String md5, long timestamp, long size, String path, problems prob) {
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_DELETE_RESPONSE");
        fileDescriptor(md5, timestamp, size, path, obj);
        if (prob == problems.NO_ERROR) {
            obj.put("message", "file deleted");
            obj.put("status", true);
        } else if (prob == problems.UNSAFE_PATH) {
            obj.put("message", "unsafe pathname given");
            obj.put("status", false);
        } else if (prob == problems.DELETE_ERROR) {
            obj.put("message", "there was a problem deleting the file");
            obj.put("status", false);
        } else if (prob == problems.FILENAME_NOT_EXIST) {
            obj.put("message", "file doesn't exist");
            obj.put("status", false);
        } else {
            obj.put("message", "there was a problem deleting the file");
            obj.put("status", false);
        }
        return obj.toString();
    }

    public static String FILE_MODIFY_REQUEST(String md5, long timestamp, long size, String path) {
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_MODIFY_REQUEST");
        fileDescriptor(md5, timestamp, size, path, obj);
        return obj.toString();
    }

    public static String FILE_MODIFY_RESPONSE(String md5, long timestamp, long size, String path, problems prob) {
        JSONObject obj = new JSONObject();
        obj.put("command", "FILE_MODIFY_RESPONSE");
        fileDescriptor(md5, timestamp, size, path, obj);
        if (prob == problems.NO_ERROR) {
            obj.put("message", "File successfully modified");
            obj.put("status", true);
        } else if (prob == problems.UNSAFE_PATH) {
            obj.put("message", "unsafe pathname given");
            obj.put("status", false);
        } else if (prob == problems.MODIFY_ERROR) {
            obj.put("message", "there was a problem modifying the file");
            obj.put("status", false);
        } else if (prob == problems.FILE_EXISTS_WITH_MATCHING) {
            obj.put("message", "file already exists with matching content");
            obj.put("status", false);
        } else if (prob == problems.PATHNAME_NOT_EXIST) {
            obj.put("message", "pathname does not exist");
            obj.put("status", false);
        }
        return obj.toString();
    }

    public static String DIRECTORY_CREATE_REQUEST(String path) {
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_CREATE_REQUEST");
        obj.put("pathName", path);
        return obj.toString();
    }

    public static String DIRECTORY_CREATE_RESPONSE(String path, problems prob) {
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_CREATE_RESPONSE");
        obj.put("pathName", path);
        if (prob == problems.NO_ERROR) {
            obj.put("message", "directory created");
            obj.put("status", true);
        } else if (prob == problems.UNSAFE_PATH) {
            obj.put("message", "unsafe pathname given");
            obj.put("status", false);
        } else if (prob == problems.CREATE_DIR_ERROR) {
            obj.put("message", "there was a problem creating the directory");
            obj.put("status", false);
        } else if (prob == problems.PATHNAME_EXISTS) {
            obj.put("message", "pathname already exists");
            obj.put("status", false);
        }
        return obj.toString();
    }

    public static String DIRECTORY_DELETE_REQUEST(String path) {
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_DELETE_REQUEST");
        obj.put("pathName", path);
        return obj.toString();
    }

    public static String DIRECTORY_DELETE_RESPONSE(String path, problems prob) {
        JSONObject obj = new JSONObject();
        obj.put("command", "DIRECTORY_DELETE_RESPONSE");
        obj.put("pathName", path);
        if (prob == problems.NO_ERROR) {
            obj.put("message", "directory deleted");
            obj.put("status", true);
        } else if (prob == problems.UNSAFE_PATH) {
            obj.put("message", "unsafe pathname given");
            obj.put("status", false);
        } else if (prob == problems.PATHNAME_NOT_EXIST) {
            obj.put("message", "pathname does not exist");
            obj.put("status", false);
        } else if (prob == problems.DELETE_DIR_ERROR) {
            obj.put("message", "there was a problem deleting the directory");
            obj.put("status", false);
        }
        return obj.toString();
    }

    public static String LIST_PEER_REQUEST() {
        JSONObject obj = new JSONObject();
        obj.put("command", "LIST_PEERS_REQUEST");
        return obj.toString();
    }

    public static String LIST_PEERS_RESPONSE(String[] host, int[] port) {
        JSONObject obj = new JSONObject();
        obj.put("command", "LIST_PEERS_RESPONSE");
        JSONArray list = new JSONArray();
        if (host != null && port != null) {
            for (int i = 0; i < host.length; i++) {
                JSONObject obj2 = new JSONObject();
                obj2.put("host", host[i]);
                obj2.put("port", port[i]);
                list.add(obj2);
            }
        }
        obj.put("peers", list);
        return obj.toString();
    }

    public static String AUTH_REQUEST(String identity) {
        JSONObject obj = new JSONObject();
        obj.put("command", "AUTH_REQUEST");
        obj.put("identity", identity);
        return obj.toString();
    }

    public static String AUTH_RESPONSE(boolean status, String SecretKey) {
        JSONObject obj = new JSONObject();
        obj.put("command", "AUTH_RESPONSE");
        if (status) {
            obj.put("message", "public key found");
            obj.put("status", status);
            obj.put("AES128", SecretKey);
            //not like above one;
            // "AES128" : [BASE64 ENCODED, ENCRYPTED SECRET KEY]

            /**
             * how to solve null point Exception?
             */
        } else {
            obj.put("status", status);
            obj.put("message", "public key not found");
        }
        return obj.toString();
    }

    public static String Payload(String str) {
        JSONObject obj = new JSONObject();
        obj.put("payload", str);
        return obj.toString();
    }

    public static String DISCONNECT_PEER_REQUEST(String host, int port) {
        JSONObject obj = new JSONObject();
        obj.put("command", "DISCONNECT_PEER_REQUEST");
        obj.put("host", host);
        obj.put("port", port);
        return obj.toString();
    }

    public static String DISCONNECT_PEER_RESPONSE(String host, int port, boolean status) {
        JSONObject obj = new JSONObject();
        obj.put("command", "DISCONNECT_PEER_RESPONSE");
        obj.put("host", host);
        obj.put("port", port);
        if (status) {
            obj.put("status", status);
            obj.put("message", "Disconnected from peer");
        } else {
            obj.put("message", "disconnected failed");
        }
        return obj.toString();
    }

    public static String CONNECT_PEER_REQUEST(String host, int port) {
        JSONObject obj = new JSONObject();
        obj.put("command", "CONNECT_PEER_REQUEST");
        obj.put("host", host);
        obj.put("port", port);
        return obj.toString();
    }

    public static String CONNECT_PEER_RESPONSE(String host, int port, boolean status) {
        JSONObject obj = new JSONObject();
        obj.put("command", "CONNECT_PEER_RESPONSE");
        obj.put("host", host);
        obj.put("port", port);
        if (status) {
            obj.put("status", status);
            obj.put("message", "connected to peer");
        } else {
            obj.put("message", "connected failed");
        }
        return obj.toString();
    }

//    public static void main(String[] args) {
//        try {
//            String testTxt = FILE_BYTES_REQUEST("sss", 4343, 65, "hahaha", 1, 34);
//            String NewTxt = GENERATE_RESPONSE_MSG(testTxt);
//            System.out.println(NewTxt);
//            System.out.println("two different text:" +RESPONSE_EQUALS(NewTxt, testTxt));
//            System.out.println("two same text:" + RESPONSE_EQUALS(NewTxt,NewTxt));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}