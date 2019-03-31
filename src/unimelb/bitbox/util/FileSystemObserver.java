package unimelb.bitbox.util;

import main.java.unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public interface FileSystemObserver {
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent);
}
