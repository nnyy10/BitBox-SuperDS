package unimelb.bitbox.util;


//Remember to add the args4j jar to your project's build path
import org.kohsuke.args4j.Option;

import java.util.logging.Logger;

//This class is where the arguments read from the command line will be stored
//Declare one field for each argument and use the @Option annotation to link the field
//to the argument name, args4J will parse the arguments and based on the name,
//it will automatically update the field with the parsed argument value
public class CmdLineArgs {
    private static Logger log = Logger.getLogger(CmdLineArgs.class.getName());

    @Option(required = true, name = "-s", aliases = {"--server"}, usage = "Server Host and port")
    private String server;

    @Option(required = false, name = "-p",aliases = {"--peer"}, usage = "Peer Host and port")
    private String peer;

    @Option(required =true,name = "-c", usage = "Command")
    private String command;

    @Option(required = false, name = "-i", usage = "Key identity")
    private String identity;


    public String getServerHost() {
        String Host;
        try{
        String [] str = server.split(":");
        Host = str[0];
        return Host;
        }
        catch (Exception e){
            log.warning("get host failed");
            return null;
        }
    }

    public int getServerPort() {
        int Port;
        try{
        String [] str = server.split(":");
        Port = Integer.parseInt(str[1]);
        return Port;
        }
        catch (Exception e){
            log.warning("get port failed");
            return 0;
        }
    }

    public String getPeerHost(){
        String Host;
        try {
        String [] str = peer.split(":");
        Host = str[0];
        return Host;
        }
        catch (Exception e){
            log.warning("get host failed");
            return null;
        }
    }

    public int getPeerPort(){
        int Port;
        try {
        String [] str = peer.split(":");
        Port = Integer.parseInt(str[1]);
        return Port;
        }
        catch (Exception e){
            log.warning("get port failed");
            return 0;
        }
    }

    public String getCommand(){return command;}

    public String getIdentity(){return  identity;}

}

