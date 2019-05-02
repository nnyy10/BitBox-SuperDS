package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	public FileSystemManager fileSystemManager;
	private ArrayList<PeerConnection> connections = new ArrayList<>();

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
		// TODO: process events

		for (PeerConnection connection : connections) {
			connection.send(toJSON(fileSystemEvent));
			System.out.println("in");
		}
	}

	public String toJSON(FileSystemEvent fileSystemEvent) {
		switch (fileSystemEvent.event) {
		case FILE_CREATE:
			return JSON_process.FILE_CREATE_REQUEST(fileSystemEvent.fileDescriptor.md5,
					fileSystemEvent.fileDescriptor.lastModified, fileSystemEvent.fileDescriptor.fileSize,
					fileSystemEvent.path);

		case FILE_DELETE:
			return JSON_process.FILE_DELETE_REQUEST(fileSystemEvent.fileDescriptor.md5,
					fileSystemEvent.fileDescriptor.lastModified, fileSystemEvent.fileDescriptor.fileSize,
					fileSystemEvent.path);

		case FILE_MODIFY:
			return JSON_process.FILE_MODIFY_REQUEST(fileSystemEvent.fileDescriptor.md5,
					fileSystemEvent.fileDescriptor.lastModified, fileSystemEvent.fileDescriptor.fileSize,
					fileSystemEvent.path);

		case DIRECTORY_CREATE:
			return JSON_process.DIRECTORY_CREATE_REQUEST((fileSystemEvent.path));

		case DIRECTORY_DELETE:
			return JSON_process.DIRECTORY_DELETE_REQUEST(fileSystemEvent.path);
		}
		return "";
	}

	public void add(PeerConnection peerConnection) {
		if (!connections.contains(peerConnection))
			this.connections.add(peerConnection);
	}

	public void remove(PeerConnection peerConnection) {
		if (connections.contains(peerConnection))
			this.connections.remove(peerConnection);
	}
	
	public  ArrayList<PeerConnection> getlist() {
		return connections;
	}
}
