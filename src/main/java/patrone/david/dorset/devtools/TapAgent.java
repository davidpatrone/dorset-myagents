/**
 *
 */
package patrone.david.dorset.devtools;

import edu.jhuapl.dorset.agents.AbstractAgent;
import edu.jhuapl.dorset.agents.AgentRequest;
import edu.jhuapl.dorset.agents.AgentResponse;
import edu.jhuapl.dorset.agents.Description;
import edu.jhuapl.dorset.agents.EchoAgent;

/**
 * Taps into an agent and allows insertion of code when a request is received and a response is
 * generated. By default, the request and response to/from the agent is printed to standard out.
 *
 * @author davidpatrone
 *
 */
public class TapAgent extends AbstractAgent {

    private static final String SUMMARY =
                    "Wraps an Agent and provides acces to info coming in and out.";
    private static final String EXAMPLE = "All input is passed to the wrapped agent.";

    private AbstractAgent wrappedAgent;

    public TapAgent() {
        this(null);
    }

    /**
     * If agent passed in is null, an EchoAgent is used.
     *
     * @param a
     */
    public TapAgent(AbstractAgent a) {
        setDescription(new Description("Tap", SUMMARY, EXAMPLE));

        if (a == null) {
            a = new EchoAgent();
        }
        wrappedAgent = a;
    }

    @Override
    public AgentResponse process(AgentRequest request) {
        requestMade(request);
        AgentResponse response = wrappedAgent.process(request);
        responseMade(response);
        return response;
    }

    /**
     * This method is called when the agent receives a request.
     *
     * @param request
     */
    protected void requestMade(AgentRequest request) {
        System.out.println(wrappedAgent.getName() + ":   Request>> " + request.getText());
    }

    /**
     * This method is called when the agent generates a response.
     *
     * @param response
     */
    protected void responseMade(AgentResponse response) {
        System.out.println(wrappedAgent.getName() + ": <<Response  " + response.getText()
                        + ", type=" + response.getType() + ", status="
                        + response.getStatus().getCode() + ":" + response.getStatus().getMessage()
                        + ", payload=" + response.getPayload());
    }

}
