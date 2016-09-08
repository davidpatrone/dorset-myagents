/**
 * 
 */
package patrone.david.agent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import edu.jhuapl.dorset.Response.Type;
import edu.jhuapl.dorset.agents.AbstractAgent;
import edu.jhuapl.dorset.agents.AgentRequest;
import edu.jhuapl.dorset.agents.AgentResponse;

/**
 * Provides the current date or current time.
 * eg: "What time is it?" "What day/date is it?"
 * 
 * @author davidpatrone
 *
 */
public class CurrentDateTimeAgent  extends AbstractAgent {

	private SimpleDateFormat amPmTimeFormat;
	private SimpleDateFormat utcTimeFormat;
	private SimpleDateFormat dateFormat;
	private SimpleDateFormat dayOfYearFormat;
	
	public CurrentDateTimeAgent() {
		amPmTimeFormat = new SimpleDateFormat("hh:mm.ssa z");
		utcTimeFormat = new SimpleDateFormat("HH:mm.ss z");
		utcTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
		dayOfYearFormat = new SimpleDateFormat("DD");
	}
	
	
	@Override
	public AgentResponse process(AgentRequest request) {
		Date now = new Date();

		String question = request.getText().toLowerCase();
		if (question.contains(" time")) {
			return new AgentResponse("It is currently " + amPmTimeFormat.format(now) 
			+ ", which is " + utcTimeFormat.format(now));
		} else if ((question.contains(" date")) || (question.contains(" day"))) {
			return new AgentResponse("Today is " + dateFormat.format(now) 
			+ ", which is day " + dayOfYearFormat.format(now) + " of this year.");			
		}
		
		return new AgentResponse(Type.ERROR, "I'm not sure what you're asking.", null);
	}

}
