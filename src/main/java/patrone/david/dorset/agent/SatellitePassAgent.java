/**
 *
 */
package patrone.david.dorset.agent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.jhuapl.dorset.agents.AbstractAgent;
import edu.jhuapl.dorset.agents.AgentRequest;
import edu.jhuapl.dorset.agents.AgentResponse;
import edu.jhuapl.dorset.http.HttpClient;
import edu.jhuapl.dorset.http.HttpRequest;
import edu.jhuapl.dorset.http.HttpResponse;
import edu.jhuapl.dorset.http.apache.ApacheHttpClient;

/**
 * Answers questions about satellite passes based upon data from http://www.heavens-above.com/
 */
public class SatellitePassAgent extends AbstractAgent {

    protected SimpleDateFormat dateFormat;
    protected SimpleDateFormat timeFormat;

    /**
     * Asking for what/when is the next visible satellite for our location. e.g. "When is the next
     * visible satellite?"
     */
    private static final String NEXT_SAT_REGEX = ".*next\\s+satellite.*";

    /**
     * Support all satellites visible for the next n [min|minutes|hours] e.g. "What satellites are
     * visible in the next 30 minutes?"
     */
    private static final String FUTURE_RANGE_REGEX =
                    ".*next\\s+(\\d+)\\s+(min|minute|hour|hours).*";

    /**
     * Support all satellites visible at some time in the future. in n [min|minutes|hours] e.g.
     * "What satellites will be visible in 30 minutes?"
     */
    private static final String FUTURE_TIME_REGEX =
                    ".*\\s+in\\s+(\\d+)\\s+(min|minute|hour|hours).*";

    protected TimeZone localTimeZone;


    protected double defaultLat;
    protected double defaultLon;
    protected int defaultAlt;

    /**
     * Provide the default lat, lon, alt of the server if the query doesn't include a location.
     *
     * @param lat default latitude in degrees
     * @param lon default longitude in degrees
     * @param alt default altitude in meters
     */
    public SatellitePassAgent(double lat, double lon, int alt) {
        this.localTimeZone = TimeZone.getDefault();
        dateFormat = new SimpleDateFormat("hh:mm:ss a z 'on' EEEE, MMMM dd, yyyy");
        dateFormat.setTimeZone(localTimeZone);

        timeFormat = new SimpleDateFormat("hh:mm:ss a z");
        timeFormat.setTimeZone(localTimeZone);

        this.defaultLat = lat;
        this.defaultLon = lon;
        this.defaultAlt = alt;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.jhuapl.dorset.agents.Agent#process(edu.jhuapl.dorset.agents.AgentRequest)
     */
    @Override
    public AgentResponse process(AgentRequest request) {

        // TODO enable location detection in the request
        List<SatRecord> data = getData(defaultLat, defaultLon, defaultAlt);

        if (data == null) {
            return new AgentResponse("I'm sorry, but I can't connect to my data source right now.");
        }

        String query = request.getText();
        if (isNextSatelliteQuery(query)) {
            // next visible satellite that appears after now
            return getNextSatelliteResponse(data);
        }

        TimeRange timeRange = getFutureRangeValues(query);
        if (timeRange != null) {
            // all satellites that appear between now and time range
            long millis = System.currentTimeMillis() + timeRange.getMillis();
            Date futureEndDate = new Date(millis);
            return getFutureRangeResponse(data, futureEndDate);
        }

        timeRange = getFutureTimeValues(query);
        if (timeRange != null) {
            // all satellites that are visible at (now + timeRange)
            long millis = System.currentTimeMillis() + timeRange.getMillis();
            Date futureDate = new Date(millis);
            return getFutureTimeResponse(data, futureDate);
        }

        // all satellites that are visible now.
        return getSatellitesOverheadNowResponse(data);
    }

    protected AgentResponse getFutureRangeResponse(List<SatRecord> data, Date futureEndDate) {
        StringBuilder sb = new StringBuilder();
        Date now = new Date();

        List<SatRecord> overhead = getRecordsWithinRange(now, futureEndDate, data);
        if (overhead.size() == 0) {
            sb.append("There will be no visible satellites overhead between now and ");
            sb.append(timeFormat.format(futureEndDate)).append(".\n");
            return new AgentResponse(sb.toString());

        } else if (overhead.size() == 1) {
            sb.append("There will be 1 visible satellite overhead between now and ");
            sb.append(timeFormat.format(futureEndDate)).append(".\n");

        } else {
            sb.append("There will be ").append(overhead.size());
            sb.append(" visible satellites overhead between now and ");
            sb.append(timeFormat.format(futureEndDate)).append(".\n");
        }

        for (SatRecord r : overhead) {
            sb.append(r.getName()).append(" (").append(r.getNoradID());
            long timeDiff = r.getStartTime().getTime() - now.getTime();
            if (timeDiff < 0) {
                sb.append(") arrived ").append(formatTimeDuration(-timeDiff));
                sb.append(" ago");
            } else {
                sb.append(") is expected in ").append(formatTimeDuration(timeDiff));
            }
            sb.append(" at ").append(timeFormat.format(r.getStartTime()));
            sb.append(", moving from ").append(r.getStartAzAlt());
            sb.append(" to ").append(r.getEndAzAlt()).append(", and will be gone at ");
            sb.append(dateFormat.format(r.getStopTime())).append(".\n\n");
        }

        return new AgentResponse(sb.toString());
    }

    protected AgentResponse getFutureTimeResponse(List<SatRecord> data, Date futureDate) {
        StringBuilder sb = new StringBuilder();

        List<SatRecord> overhead = getRecordsWithinTime(futureDate, data);
        if (overhead.size() == 0) {
            sb.append("There will be no visible satellites overhead at ");
            sb.append(timeFormat.format(futureDate)).append(".\n");
            return new AgentResponse(sb.toString());

        } else if (overhead.size() == 1) {
            sb.append("There will be 1 visible satellite overhead at ");
            sb.append(timeFormat.format(futureDate)).append(".\n");

        } else {
            sb.append("There will be ").append(overhead.size());
            sb.append(" visible satellites overhead at ");
            sb.append(timeFormat.format(futureDate)).append(".\n");

        }

        long now = System.currentTimeMillis();
        for (SatRecord r : overhead) {
            sb.append(r.getName()).append(" (").append(r.getNoradID());
            long timeDiff = r.getStartTime().getTime() - now;
            if (timeDiff < 0) {
                sb.append(") arrived ").append(formatTimeDuration(-timeDiff));
                sb.append(" ago");
            } else {
                sb.append(") is expected in ").append(formatTimeDuration(timeDiff));
            }
            sb.append(" at ").append(timeFormat.format(r.getStartTime()));
            sb.append(", moving from ").append(r.getStartAzAlt());
            sb.append(" to ").append(r.getEndAzAlt()).append(", and will be gone at ");
            sb.append(dateFormat.format(r.getStopTime())).append(".\n\n");
        }

        return new AgentResponse(sb.toString());
    }


    protected List<SatRecord> getData(double lat, double lon, int alt) {
        HttpClient client = new ApacheHttpClient();
        client.setUserAgent("custom");
        String url = generateURL(lat, lon, alt, localTimeZone.getDisplayName(false, TimeZone.SHORT,
                        Locale.getDefault(Locale.Category.DISPLAY)));
        HttpResponse httpResponse = client.execute(HttpRequest.get(url));
        if (httpResponse == null) {
            return null;
        }
        List<SatRecord> data = parseData(httpResponse.asString());
        return data;
    }

    protected AgentResponse getSatellitesOverheadNowResponse(List<SatRecord> data) {
        StringBuilder sb = new StringBuilder();

        Date now = new Date(System.currentTimeMillis());
        List<SatRecord> overhead = getRecordsWithinTime(now, data);
        if (overhead.size() == 0) {
            return new AgentResponse("There are no visible satellites overhead right now.");
        } else if (overhead.size() == 1) {
            sb.append("There is 1 visible satellite overhead right now.\n");
        } else {
            sb.append("There are ").append(overhead.size());
            sb.append(" visible satellites overhead right now.\n");
        }
        for (SatRecord r : overhead) {
            sb.append(r.getName()).append(" (").append(r.getNoradID()).append(") is moving from ");
            sb.append(r.getStartAzAlt());
            sb.append(" to ").append(r.getEndAzAlt()).append(", and will be gone in ");
            sb.append(formatTimeDuration(r.getStopTime().getTime() - now.getTime())).append(".\n");
        }
        return new AgentResponse(sb.toString());
    }

    protected AgentResponse getNextSatelliteResponse(List<SatRecord> data) {
        if (data.isEmpty()) {
            return new AgentResponse("No visible satellites are expected in the near future.");
        } else {
            Date now = new Date();
            SatRecord r = null;
            for (SatRecord record : data) {
                // assumes sorted order. Find the first start time after now.
                if (record.getStartTime().after(now)) {
                    r = record;
                    break;
                }
            }
            if (r == null) {
                return new AgentResponse("No visible satellites are expected in the near future.");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(r.getName()).append(" (").append(r.getNoradID())
                                .append(") is expected in ");
                sb.append(formatTimeDuration(r.getStartTime().getTime() - now.getTime()))
                                .append(", moving from ").append(r.getStartAzAlt());
                sb.append(" to ").append(r.getEndAzAlt()).append(", and will be gone at ");
                sb.append(dateFormat.format(r.getStopTime())).append(".\n");
                return new AgentResponse(sb.toString());
            }
        }
    }


    protected boolean isNextSatelliteQuery(String query) {
        Pattern pattern = Pattern.compile(NEXT_SAT_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);

        return matcher.find();
    }

    protected TimeRange getFutureRangeValues(String query) {
        TimeRange result = null;
        Pattern pattern = Pattern.compile(FUTURE_RANGE_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            if ((matcher.group(1) != null) && (matcher.group(2) != null)) {
                result = new TimeRange(Integer.parseInt(matcher.group(1).trim()),
                                matcher.group(2).trim());
            }
        }
        return result;
    }

    protected TimeRange getFutureTimeValues(String query) {
        TimeRange result = null;
        Pattern pattern = Pattern.compile(FUTURE_TIME_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            if ((matcher.group(1) != null) && (matcher.group(2) != null)) {
                result = new TimeRange(Integer.parseInt(matcher.group(1).trim()),
                                matcher.group(2).trim());
            }
        }
        return result;
    }

    protected static String formatTimeDuration(long timeInMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(timeInMillis);
        long minutes = TimeUnit.MILLISECONDS
                        .toMinutes(timeInMillis - TimeUnit.HOURS.toMillis(hours));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis - TimeUnit.HOURS.toMillis(hours)
                        - TimeUnit.MINUTES.toMillis(minutes));

        StringBuilder sb = new StringBuilder();
        if (hours > 1) {
            sb.append(hours).append(" hours, ");
        } else if (hours == 1) {
            sb.append(hours).append(" hour, ");
        }

        if (minutes == 1) {
            sb.append(minutes).append(" minute, ");
        } else {
            sb.append(minutes).append(" minutes, ");
        }

        if (seconds == 1) {
            sb.append(seconds).append(" second");
        } else {
            sb.append(seconds).append(" seconds");
        }

        return sb.toString();
    }

    public String generateURL(double lat, double lon, int alt) {
        return "http://www.heavens-above.com/AllSats.aspx?lat=" + lat + "&lng=" + lon;

    }

    public String generateURL(double lat, double lon, int alt, String timezone) {
        return "http://www.heavens-above.com/AllSats.aspx?lat=" + lat + "&lng=" + lon
                        + "&loc=00000&alt=" + alt + "&tz=" + timezone;
    }


    public List<SatRecord> getRecordsWithinRange(Date startTime, Date endTime,
                    List<SatRecord> data) {
        if (startTime.after(endTime)) {
            // ensure start is before end
            Date tmp = endTime;
            endTime = startTime;
            startTime = tmp;
        }

        ArrayList<SatRecord> results = new ArrayList<>();
        for (SatRecord r : data) {
            if (((r.getStartTime().after(startTime) && r.getStartTime().before(endTime)))
                            || ((r.getStopTime().after(startTime)
                                            && r.getStopTime().before(endTime)))
                            || ((r.getStartTime().before(startTime)
                                            && r.getStopTime().after(endTime)))) {
                results.add(r);
            }
        }
        return results;
    }

    public List<SatRecord> getRecordsWithinTime(Date time, List<SatRecord> data) {
        ArrayList<SatRecord> results = new ArrayList<>();
        for (SatRecord r : data) {
            if (r.getStartTime().before(time) && r.getStopTime().after(time)) {
                results.add(r);
            }
        }
        return results;
    }

    public List<SatRecord> parseData(String html) {
        ArrayList<SatRecord> data = new ArrayList<>();

        Scanner s = new Scanner(html);
        while (s.hasNextLine()) {
            String line = s.nextLine();
            // move down to the data table.
            if (line.contains("standardTable")) {
                break;
            }
        }

        while (s.hasNextLine()) {
            String line = s.nextLine();
            // stop at end of table.
            if (line.contains("tbody")) {
                break;
            }
            data.add(parseLine(line));
        }

        s.close();
        return data;
    }

    protected SatRecord parseLine(String s) {
        Matcher noradMatcher = Pattern.compile("satid=([^\\&]*)").matcher(s);
        noradMatcher.find();
        int noradId = Integer.parseInt(noradMatcher.group(1));

        Matcher m = Pattern.compile("<td>([^<]*)</td>")
                        .matcher(s.replaceAll(" align=\"center\"", ""));

        m.find();
        String satName = m.group(1);
        m.find();
        String brightness = m.group(1);
        m.find();
        String a = m.group(1);
        String[] arrivalTime = m.group(1).split(":");
        m.find();
        String arrivalAlt = m.group(1);
        m.find();
        String arrivalAz = m.group(1);
        m.find();
        // highest Time
        String highestAlt = m.group(1);
        m.find();
        String highestAz = m.group(1);
        m.find();
        String[] departTime = m.group(1).split(":");;
        m.find();
        String departAlt = m.group(1);
        m.find();
        String departAz = m.group(1);

        Calendar c = Calendar.getInstance();
        c.setTimeZone(localTimeZone);
        Date now = new Date();
        // just the time... add the date with the calendar
        c.setTime(now);
        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(arrivalTime[0]));
        c.set(Calendar.MINUTE, Integer.parseInt(arrivalTime[1]));
        c.set(Calendar.SECOND, Integer.parseInt(arrivalTime[2]));

        Date start = c.getTime();

        c.setTime(now);
        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(departTime[0]));
        c.set(Calendar.MINUTE, Integer.parseInt(departTime[1]));
        c.set(Calendar.SECOND, Integer.parseInt(departTime[2]));

        Date end = c.getTime();

        SatRecord r = new SatRecord(satName, noradId, Double.parseDouble(brightness), start,
                        arrivalAlt + arrivalAz, highestAlt + highestAz, end, departAlt + departAz);
        return r;
    }

    /**
     * Either mins or hours.
     */
    protected class TimeRange {
        /**
         * @return the val
         */
        public double getVal() {
            return val;
        }

        /**
         * @return the units
         */
        public String getUnits() {
            return units;
        }

        /**
         * @param val
         * @param units
         */
        public TimeRange(double val, String units) {
            super();
            this.val = val;
            this.units = units;
        }

        protected double val;
        protected String units;

        public long getMillis() {
            if (units.startsWith("min")) {
                return TimeUnit.MINUTES.toMillis((long) val);
            } else {
                return TimeUnit.HOURS.toMillis((long) val);
            }
        }
    }

    protected static class SatRecord {
        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "SatRecord [name=" + name + ", noradID=" + noradID + ", brightness=" + brightness
                            + ", startTime=" + startTime + ", startAzAlt=" + startAzAlt
                            + ", highestAzAlt=" + highestAzAlt + ", stopTime=" + stopTime
                            + ", endAzAlt=" + endAzAlt + "]";
        }

        protected String name;
        protected double brightness;
        protected Date startTime;
        protected String startAzAlt;
        protected String highestAzAlt;
        protected Date stopTime;
        protected String endAzAlt;
        protected int noradID;

        /**
         * @param satRecord
         * @param name
         * @param brightness
         * @param startTime
         * @param startAzAlt
         * @param highestAzAlt
         * @param stopTime
         * @param endAzAlt
         */
        public SatRecord(String name, int noradID, double brightness, Date startTime,
                        String startAzAlt, String highestAzAlt, Date stopTime, String endAzAlt) {
            super();
            this.name = name;
            this.noradID = noradID;
            this.brightness = brightness;
            this.startTime = startTime;
            this.startAzAlt = startAzAlt;
            this.highestAzAlt = highestAzAlt;
            this.stopTime = stopTime;
            this.endAzAlt = endAzAlt;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the noradID
         */
        public int getNoradID() {
            return noradID;
        }

        /**
         * @return the brightness
         */
        public double getBrightness() {
            return brightness;
        }

        /**
         * @return the startTime
         */
        public Date getStartTime() {
            return startTime;
        }

        /**
         * @return the startAzAlt
         */
        public String getStartAzAlt() {
            return startAzAlt;
        }

        /**
         * @return the highestAzAlt
         */
        public String getHighestAzAlt() {
            return highestAzAlt;
        }

        /**
         * @return the stopTime
         */
        public Date getStopTime() {
            return stopTime;
        }

        /**
         * @return the endAzAlt
         */
        public String getEndAzAlt() {
            return endAzAlt;
        }
    }
}
