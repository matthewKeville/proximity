package keville.meetup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.PrintStream;
import java.io.FileOutputStream;

import java.time.Instant;

import org.apache.commons.text.StringEscapeUtils;

public class MeetupHarUtil {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MeetupHarUtil.class);

    public static String extractEventsJson(String harString) {

      String jsonRaw = "";

      // Extract unescaped json event data
      try {
        // compliments of https://regex101.com -> java 8 
        final String regex = "(?<=www.googletagmanager.com\\\\\\\"/><script type=\\\\\\\"application/ld\\+json\\\\\\\">).*?(?=</script>)";
        final Pattern pattern = Pattern.compile(regex);
        Matcher mat = pattern.matcher(harString);
        if (mat.find()) {
          LOG.debug("found one match");
          jsonRaw=mat.group(0);
        } else {
          LOG.warn("full regex match not found");
        }
      } catch (Exception e ) {
        LOG.error("unexpected har data");
        LOG.error(e.getMessage());
        try {
          //TODO this file path should not be hardcoded ( as EventbriteHarUtils will need similar flow )
          PrintStream filePrintStream = new PrintStream(new FileOutputStream("logs/meetup-failure."+Instant.now().toString()+".har"));
          filePrintStream.print(harString);
          filePrintStream.close();
        } catch (Exception e2) {
          LOG.error("error trying to save HAR file to LFS");
        }
      }

      // Unescape json data

      return StringEscapeUtils.unescapeJson(jsonRaw);
      

  }

}
