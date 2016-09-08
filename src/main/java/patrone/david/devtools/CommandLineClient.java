/**
 * 
 */
package patrone.david.devtools;

import java.util.Scanner;

import edu.jhuapl.dorset.Application;
import edu.jhuapl.dorset.Request;
import edu.jhuapl.dorset.Response;
import edu.jhuapl.dorset.agents.AbstractAgent;
import edu.jhuapl.dorset.routing.Router;
import edu.jhuapl.dorset.routing.SingleAgentRouter;
import patrone.david.agent.MagicEightBallAgent;

/**
 * @author davidpatrone
 *
 */
public class CommandLineClient {

	private Application app;
	
	public CommandLineClient(AbstractAgent agent) {
		this(new SingleAgentRouter(agent));
	}

	public CommandLineClient(Router router) {
		this(new Application(router));
	}
	
	public CommandLineClient(Application app) {
        this.app = app;
	}
	
	public void go() {
		String input = "";
        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            input = in.nextLine();

            if (isQuitString(input)) {
                break;
            }

            Request request = new Request(input);
            Response response = app.process(request);

            System.out.println(response.getText());
        }

        System.out.println("\nBye.");
        in.close();
	}
	
	/**
	 * Override-able method for defining what input string will
	 * stop the system. The default is a single character 'q'.
	 * @param input
	 * @return
	 */
	protected boolean isQuitString(String input) {
		return input.equals("q");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TapAgent agent = new TapAgent(new MagicEightBallAgent());
		CommandLineClient client = new CommandLineClient(agent);
		client.go();        
	}

}
