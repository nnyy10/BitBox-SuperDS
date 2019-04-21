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
				System.out.print("");
				break;
			case FILE_DELETE:
				System.out.print("");
				break;
			case FILE_MODIFY:
				System.out.print("");
				break;
			case DIRECTORY_CREATE:
				System.out.print("");
				break;
			case DIRECTORY_DELETE:
				System.out.print("");
				break;
		}
		for (PeerConnection connection:connections)
		{

			}
//			if (connection ==fileSystemManager.EVENT.FILE_CREATE )
//			{
//				System.out.print("--");
//			}


		}
	}
	

