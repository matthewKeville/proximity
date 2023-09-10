package keville.Eventbrite;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventbriteHarUtil {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventbriteHarUtil.class);

    public static List<String> extractEventIds(String harString) {

      List<String> eventIds = new ArrayList<String>();

        Pattern pat = Pattern.compile("(?<=eventbrite_event_id\\\\\":\\\\\").*?(?=\\\\\",\\\\\"start)"); //what an ungodly creation
        Matcher mat = pat.matcher(harString);
        while (mat.find()) {
          eventIds.add(mat.group());
        }

      return eventIds;

  }

}
