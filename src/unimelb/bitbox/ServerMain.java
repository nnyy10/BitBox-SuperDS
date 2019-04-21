package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.PeerConnection;
import unimelb.bitbox.JSON_process;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	private ArrayList<PeerConnection> connections = new ArrayList<>();
	
	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
		System.out.println(Configuration.getConfigurationValue("path"));
	}

	
	
	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// TODO: process events
		switch (fileSystemEvent.event) {
			case FILE_CREATE:
				JSON_process.FILE_CREATE_REQUEST(fileSystemEvent.fileDescriptor.md5
						,fileSystemEvent.fileDescriptor.lastModified,fileSystemEvent.fileDescriptor.fileSize
						,fileSystemEvent.path);
				break;

			case FILE_DELETE:
				JSON_process.FILE_DELETE_REQUEST(fileSystemEvent.fileDescriptor.md5
						,fileSystemEvent.fileDescriptor.lastModified
						,fileSystemEvent.fileDescriptor.fileSize
						,fileSystemEvent.path
						);
				break;

			case FILE_MODIFY:
				JSON_process.FILE_MODIFY_REQUEST(fileSystemEvent.fileDescriptor.md5
						,fileSystemEvent.fileDescriptor.lastModified
						,fileSystemEvent.fileDescriptor.fileSize
						,fileSystemEvent.path
				);
				break;

			case DIRECTORY_CREATE:
				JSON_process.DIRECTORY_DELETE_REQUEST(fileSystemEvent.path);
				break;

			case DIRECTORY_DELETE:
				JSON_process.DIRECTORY_DELETE_REQUEST(fileSystemEvent.path);
				break;
		}
		for (PeerConnection connection:connections)
		{

			}
		}

		}
	

