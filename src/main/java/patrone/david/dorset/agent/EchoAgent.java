/**
 * 
 */
package patrone.david.dorset.agent;

import edu.jhuapl.dorset.agents.AbstractAgent;
import edu.jhuapl.dorset.agents.AgentRequest;
import edu.jhuapl.dorset.agents.AgentResponse;

/**
 * Simple agent that just returns your text query.
 * 
 * @author davidpatrone
 *
 */
public class EchoAgent extends AbstractAgent {

    @Override
    public AgentResponse process(AgentRequest request) {
        AgentResponse r = new AgentResponse(request.getText());
        return r;
    }
}
