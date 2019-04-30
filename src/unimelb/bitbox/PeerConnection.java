package unimelb.bitbox;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import unimelb.bitbox.util.FileSystemManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.io.File;

import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;;


public class PeerConnection implements Runnable{
	
	protected Socket socket = null;
	protected BufferedReader inputStream = null;
	protected BufferedWriter outputStream = null;
	protected ServerMain fileSystemObserver = null;
	
    public PeerConnection(Socket socket) {

        this.socket = socket;
        this.fileSystemObserver = ServerMain.getInstance();
        try {
        	inputStream  = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
        	outputStream = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), "UTF-8"));
		} catch (IOException e) {
			this.CloseConnection();
		}
    }

	protected void CloseConnection(){
		System.out.println("Closing Connection");
		try{
        	this.inputStream.close();
        	this.outputStream.close();
        	this.socket.close();
        } catch(Exception e){
        	e.printStackTrace();
        }
	}
	
    public void run() {
        String line = "";
		FileSystemManager.FileSystemEvent file_event;
        // reads message from client until "Over" is sent 
        while (true) 
        {
            try
            {
                line = inputStream.readLine();
//				send(JSON_process.getInformation(line));
	        } catch (Exception e) {
	        	this.CloseConnection();
	        	break;
	        }
    	}
    }
    
    public void send(String message){
    	try{
    		outputStream.write(message+"\n");
    		outputStream.flush();
    	} catch(Exception e){
    		System.out.println("cant print " + message);
    		this.CloseConnection();
    	}
    	
    }
	public void implement(String str, FileSystemManager.FileSystemEvent fileSystemEvent) {
		JSONParser parser = new JSONParser();

		try {
			String md5 = " ", host = " ", msg = " ", pathName = " ";
			long size = 0;
			long port = 0, position = 0, length = 0, timestamp = 0;
			JSONObject obj = (JSONObject) parser.parse(str);
			// first JSONObject need to be deal with, and then use obj as input
			String information = (String) obj.get("command");
			JSONObject fileDescriptor;
//FileSystemEvent fileSystemEvent = fileSystemManager.de.new FileSystemEvent(" "," ",FileSystemManager.EVENT.FILE_CREATE);
			fileDescriptor = (JSONObject) obj.get("fileDescriptor");
			md5 = (String) fileDescriptor.get("md5");
			//System.out.println("md5: "+ md5);
			timestamp = (long) fileDescriptor.get("lastModified");
			size = (long) fileDescriptor.get("fileSize");
			pathName = (String) obj.get("pathName");


			switch (information) {

				case "FILE_CREATE_REQUEST":
					if (this.fileSystemObserver.fileSystemManager.isSafePathName(pathName))
					{	if(!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName)){
							try{
							 if(this.fileSystemObserver.fileSystemManager.createFileLoader(pathName,md5,size,timestamp));
								{if(this.fileSystemObserver.fileSystemManager.checkShortcut(pathName))
									{
										send(JSON_process.FILE_BYTES_REQUEST(md5,timestamp,size,pathName,position,length));
									}

								}
							send(JSON_process.FILE_CREATE_RESPONSE(md5,timestamp,size,pathName,JSON_process.problems.PATHNAME_EXISTS));}
							catch (Exception e){e.printStackTrace();}
//								try{
//								String temp[]=pathName.split("\\\\");
//								String prefix=temp[temp.length-1];
//								String[] strArray = pathName.split("\\.");
//								int suffixIndex = strArray.length -1;
//								String suffix=strArray[suffixIndex];
//								File.createTempFile(prefix,suffix);
//								}catch(Exception e){e.printStackTrace();}

							} else { send(JSON_process.FILE_CREATE_RESPONSE(md5,timestamp,size,pathName,JSON_process.problems.PATHNAME_EXISTS));}}
 					else{send(JSON_process.FILE_CREATE_RESPONSE(md5,timestamp,size,pathName,JSON_process.problems.UNSAFE_PATH));}



				case "FILE_DELETE_REQUEST":
					if (this.fileSystemObserver.fileSystemManager.isSafePathName(pathName))
						{if(!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName))
						{
							this.fileSystemObserver.fileSystemManager.deleteFile(pathName,timestamp,md5);
						}
						}





				case "FILE_MODIFY_REQUEST":
					File local_file = new File(pathName);
					try{
					if (this.fileSystemObserver.fileSystemManager.isSafePathName(pathName))
					{if(!this.fileSystemObserver.fileSystemManager.fileNameExists(pathName))
						{
						this.fileSystemObserver.fileSystemManager.createFileLoader(pathName,md5,size,timestamp);
						if(timestamp>=local_file.length())
						//protocol
						{
						this.fileSystemObserver.fileSystemManager.readFile(md5,position,size);
						this.fileSystemObserver.fileSystemManager.checkShortcut(pathName);
						this.fileSystemObserver.fileSystemManager.modifyFileLoader(pathName,md5,size);
//						this.fileSystemObserver.fileSystemManager.writeFile(pathName,src,position);
						if (this.fileSystemObserver.fileSystemManager.checkWriteComplete(pathName))
								{
									this.fileSystemObserver.fileSystemManager.cancelFileLoader(pathName);
								}
							}
						}
					} }
					catch (Exception e){e.printStackTrace();}


				case "DIRECTORY_DELETE_REQUEST":
					File file = new File(pathName);
					if(file.isDirectory()){
						if(file.list().length==0){
					this.fileSystemObserver.fileSystemManager.deleteDirectory(pathName);}}

				case "DIRECTORY_CREATE_REQUEST":
					file = new File(pathName);
					if(!file.exists())
					{
						try {
							file.createNewFile();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

			}
		}catch(Exception e){e.printStackTrace();}
    }
}
