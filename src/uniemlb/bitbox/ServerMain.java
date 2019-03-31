package uniemlb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import main.java.unimelb.bitbox.util.Configuration;
import main.java.unimelb.bitbox.util.FileSystemManager;
import main.java.unimelb.bitbox.util.FileSystemObserver;
import main.java.unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	
	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// TODO: process events
	}
	
}
