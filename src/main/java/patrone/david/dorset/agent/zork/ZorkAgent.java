/**
 * 
 */
package patrone.david.dorset.agent.zork;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhuapl.dorset.agents.AbstractAgent;
import edu.jhuapl.dorset.agents.AgentRequest;
import edu.jhuapl.dorset.agents.AgentResponse;

/**
 * Starts a new ZorkWrapper with "I want to play Zork", end by typing a 'q'.
 * 
 * The constructor takes the full path to the zork executable on your local machine.
 * 
 * You can download a C version of zork from https://github.com/devshane/zork Modify the first 2
 * Makefile lines to define where zork should be installed run:
 * 
 * <pre>
 *   make 
 *   make install
 * </pre>
 * 
 * Pass this full path to zork into this ZorkAgent.
 */
public class ZorkAgent extends AbstractAgent {
    private final Logger logger = LoggerFactory.getLogger(ZorkAgent.class);

    protected String zorkLocation;
    protected ZorkWrapper zork;

    public ZorkAgent(String zorkLocation) {
        this.zork = null;
        this.zorkLocation = zorkLocation;
    }

    @Override
    public AgentResponse process(AgentRequest request) {
        // clean up a game that ended first...
        if ((zork != null) && zork.hasEnded) {
            zork.shutDown();
            zork = null;
        }

        if (zork == null) {
            if (request.getText().toLowerCase().indexOf("play zork") >= 0) {
                // create a new session
                zork = new ZorkWrapper(zorkLocation);
                String response;
                try {
                    response = zork.go();
                    return new AgentResponse(response);
                } catch (IOException e) {
                    zork.shutDown();
                    zork = null;
                    e.printStackTrace();
                    return new AgentResponse("It seems like I can't start a new game right now.");
                }
            } else {
                return new AgentResponse(
                                "Say 'I want to play Zork' to start a game, or 'q' to quit.");
            }
        } else {
            // middle of a game session:
            if (zork == null) {
                return new AgentResponse(
                                "I can't find a game for you. Say 'I want to play Zork' to start a new game.");
            }
            String response = zork.sendCommand(request.getText());
            return new AgentResponse(response);
        }
    }

}
