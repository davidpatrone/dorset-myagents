/**
 * 
 */
package patrone.david.dorset.agent.zork;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This kicks off a process for this game, and manages the in/out streams to/from it.
 * 
 *
 */
public class ZorkWrapper {
    private final Logger logger = LoggerFactory.getLogger(ZorkWrapper.class);

    protected ProcessBuilder processBuilder;
    protected Process process;
    protected InputStreamReader reader;
    protected BufferedWriter writer;
    protected boolean hasEnded;

    /**
     * 
     * @param zorkLocation full path the the local 'zork' executable
     */
    public ZorkWrapper(String zorkLocation) {
        this.processBuilder = new ProcessBuilder(zorkLocation);
        this.hasEnded = false;
    }

    public boolean hasEnded() {
        return hasEnded;
    }

    public String go() throws IOException {
        process = processBuilder.start();
        reader = new InputStreamReader(process.getInputStream());
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        return getResponse();
    }

    public String sendCommand(String input) {
        try {
            // it doesn't like empty requests.
            if (!input.matches(".*\\w.*")) {
                return "You didn't say anything.";
            }
            writer.write(input + "\n");
            writer.flush();
            return getResponse();
        } catch (Exception ex) {
            shutDown();
            return "I'm sorry, but your game seems to have been quit. You can start another game if you want.";
        }
    }

    public void shutDown() {
        logger.debug("Zork shut down.");

        if (process != null) {
            process.destroyForcibly();
        }
        reader = null;
        writer = null;
        hasEnded = true;
    }

    protected String getResponse() {
        final StringBuilder sb = new StringBuilder();

        // Kick off a thread that runs for 3 sec MAX to gather output)
        ExecutorService executor = Executors.newFixedThreadPool(1);

        executor.submit(() -> {
            try {
                int lastChar = 10;
                int character = 0;
                try {
                    while ((reader != null) && !(lastChar == 10 && character == 62)
                                    && !sb.toString().endsWith("Do you wish to leave the game?")) {
                        lastChar = character;
                        character = reader.read();
                        if (character == -1) {
                            sb.append("Your game has ended.");
                            break; // end of stream
                        }
                        sb.append((char) character);

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } catch (Exception e) {
                throw new IllegalStateException("task interrupted", e);
            }
        });

        try {
            executor.shutdown();
            // if for some reason, reading the output stream from
            // zork doesn't supply one of our end conditions, this ensures
            // we don't hang waiting for more output that isn't coming.
            executor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            logger.info("Problem terminating executator", ie);
        }

        String returnString = sb.toString();
        // if this ends with the '>' character, indicating it is
        // waiting for more input, strip that off since the client
        // can handle that however they want.
        if (returnString.endsWith(">")) {
            return returnString.substring(0, returnString.length() - 1);
        } else {
            return sb.toString();
        }
    }

}
