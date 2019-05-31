package unimelb.bitbox;

import java.util.logging.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import unimelb.bitbox.util.CmdLineArgs;
import unimelb.bitbox.Encryption;
import unimelb.bitbox.JSON_process;

public class Client {
    private static Logger log = Logger.getLogger(Client.class.getName());
    private static boolean finishRead = false;


    private static void InputInvalid(){
        log.warning("Input invalid\n" +
                "please input like this: java -cp bitbox.jar unimelb.bitbox.Client -c [command] -s [serverHost:port] -p [peerHost:port]");
    }


    public static void main(String [] args){


        //Object that will store the parsed command line arguments
        CmdLineArgs argsBean = new CmdLineArgs();

        //Parser provided by args4j
        CmdLineParser parser = new CmdLineParser(argsBean);
        try {
            //Parse the arguments
            parser.parseArgument(args);

            //After parsing, the fields in argsBean have been updated with the given
            //command line arguments
            String serverHost = argsBean.getServerHost();
            int serverPort = argsBean.getServerPort();
            String peerHost = argsBean.getPeerHost();
            int peerPort = argsBean.getPeerPort();
            String command = argsBean.getCommand();
            String identity = argsBean.getIdentity();

            String encryptedSharedKey = Encryption.encryptSharedKey(identity);

            switch (command){
                case "list_peers":
                    //send(JSON_process.LIST_PEER_REQUEST());
                    break;
                case "connect_peer":
                    //send(JSON_process.AUTH_REQUEST(encryptedSharedKey));
                    break;
                case "disconnect_peer":
                    //send(JSON_process.DISCONNECT());
                    break;
                default:
                    log.warning("No such a command");
                    break;
            }
//            System.out.println("server Host: " + argsBean.getServerHost());
//            System.out.println("server Port: " + argsBean.getServerPort());
//            System.out.println("peer Host: "+ argsBean.getPeerHost());
//            System.out.println("peer Port: "+ argsBean.getPeerPort());
//            System.out.println("identity: "+ argsBean.getIdentity());
        } catch (CmdLineException e) {

            System.err.println(e.getMessage());

            //Print the usage to help the user understand the arguments expected
            //by the program
            parser.printUsage(System.err);
        }






    }

}
