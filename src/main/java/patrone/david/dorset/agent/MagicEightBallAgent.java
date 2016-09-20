/**
 * 
 */
package patrone.david.dorset.agent;

import java.util.Random;

import edu.jhuapl.dorset.agents.AbstractAgent;
import edu.jhuapl.dorset.agents.AgentRequest;
import edu.jhuapl.dorset.agents.AgentResponse;

/**
 * No matter what the input is, it returns with one of the Magic Eight Ball responses.
 * 
 * https://en.wikipedia.org/wiki/Magic_8-Ball
 * 
 * @author davidpatrone
 *
 */
public class MagicEightBallAgent extends AbstractAgent {
    private static final String[] RESPONSES = {"It is certain", "It is decidedly so",
                    "Without a doubt", "Yes, definitely", "You may rely on it", "As I see it, yes",
                    "Most likely", "Outlook good", "Yes", "Signs point to yes",
                    "Reply hazy try again", "Ask again later", "Better not tell you now",
                    "Cannot predict now", "Concentrate and ask again", "Don't count on it",
                    "My reply is no", "My sources say no", "Outlook not so good", "Very doubtful"};

    private Random random = new Random();

    @Override
    public AgentResponse process(AgentRequest request) {
        String answer = RESPONSES[random.nextInt(RESPONSES.length)];
        AgentResponse r = new AgentResponse(answer);
        return r;
    }

}
