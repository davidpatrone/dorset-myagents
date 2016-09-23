/**
 * 
 */
package patrone.david.dorset.client;

import edu.jhuapl.dorset.components.tools.CommandLineClient;
import patrone.david.dorset.agent.zork.ZorkAgent;

/**
 * Example Zork client for dorset, assuming zork is installed at /usr/local/bin.
 * 
 */
public class ZorkClient {

    /**
     * Starts the command line client
     * 
     * @param args - not used
     */
    public static void main(String[] args) {
        CommandLineClient c = new CommandLineClient(new ZorkAgent("/usr/local/bin/zork"));
        c.go();
    }

}
