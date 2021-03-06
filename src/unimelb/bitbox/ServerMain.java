package unimelb.bitbox;

import java.io.IOException;

import java.security.NoSuchAlgorithmException;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
    private static Logger log = Logger.getLogger(ServerMain.class.getName());
    public FileSystemManager fileSystemManager;
    private CopyOnWriteArrayList<PeerConnection> connections = new CopyOnWriteArrayList<>();

    private ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
        fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path").trim(), this);
    }

    private static ServerMain Single_instance = null;

    public static ServerMain getInstance() {
        if (Single_instance == null)
            try {
                Single_instance = new ServerMain();
            } catch (Exception e) {
                e.printStackTrace();
            }
        return Single_instance;
    }

    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        for (PeerConnection connection : connections)
            connection.send(FileSystemEventToJSON(fileSystemEvent));
    }

    public String FileSystemEventToJSON(FileSystemEvent fileSystemEvent) {
        switch (fileSystemEvent.event) {
            case FILE_CREATE:
                return JSON_process.FILE_CREATE_REQUEST(fileSystemEvent.fileDescriptor.md5,
                        fileSystemEvent.fileDescriptor.lastModified, fileSystemEvent.fileDescriptor.fileSize,
                        fileSystemEvent.pathName);
            case FILE_DELETE:
                return JSON_process.FILE_DELETE_REQUEST(fileSystemEvent.fileDescriptor.md5,
                        fileSystemEvent.fileDescriptor.lastModified, fileSystemEvent.fileDescriptor.fileSize,
                        fileSystemEvent.pathName);
            case FILE_MODIFY:
                return JSON_process.FILE_MODIFY_REQUEST(fileSystemEvent.fileDescriptor.md5,
                        fileSystemEvent.fileDescriptor.lastModified, fileSystemEvent.fileDescriptor.fileSize,
                        fileSystemEvent.pathName);
            case DIRECTORY_CREATE:
                return JSON_process.DIRECTORY_CREATE_REQUEST(fileSystemEvent.pathName);
            case DIRECTORY_DELETE:
                return JSON_process.DIRECTORY_DELETE_REQUEST(fileSystemEvent.pathName);
        }
        throw new IllegalArgumentException("the file system event is invalid");
    }

    public void add(PeerConnection peerConnection) {
        if (!connections.contains(peerConnection))
            this.connections.add(peerConnection);
    }

    public void remove(PeerConnection peerConnection) {
        if (connections.contains(peerConnection)) {
            this.connections.remove(peerConnection);
            if (TCP_Server.class.isInstance(peerConnection))
                TCP_Server.numberOfConnections--;
        }
    }

    public CopyOnWriteArrayList<PeerConnection> getlist() {
        return connections;
    }
}
