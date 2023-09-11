package keville.AllEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import java.time.Instant;

import java.net.URLDecoder;

import org.apache.commons.text.StringEscapeUtils;

public class AllEventsHarUtil {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllEventsHarUtil.class);

    public static String extractEventsJson(String harString) {

      String rawHTML = "";

      // extract contents of "text" field 
      boolean firstMatch = false;
      
      try {
        final String regex = "(?<=\\\"mimeType\\\": \\\"application\\/x-www-form-urlencoded\\\",\\n            \\\"text\\\": \\\").*?(?=\\\",\\n            \\\"params\\\")";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher mat = pattern.matcher(harString);
        if (mat.find()) {
          LOG.debug("found match for urlencoded json data container");
          rawHTML=mat.group(0);
          firstMatch = true;
        } else {
          LOG.warn("did not find match for urlencoded json data container");
        }
      } catch (Exception e ) {
        LOG.error("unexpected har data");
        LOG.error(e.getMessage());
      }

      if ( !firstMatch ) {
        try {
          PrintStream filePrintStream = new PrintStream(new FileOutputStream("logs/AllEvents-failure."+Instant.now().toString()+".har"));
          filePrintStream.print(harString);
          filePrintStream.close();
        } catch (Exception e2) {
          LOG.error("error trying to save HAR file to LFS");
        }
        return null;
      }

      // urlDecode json data
      String unencodedHTML = "";
      try {
        unencodedHTML = URLDecoder.decode(rawHTML, StandardCharsets.UTF_8.toString() );
      } catch (Exception e) {
        LOG.error("unable to unencode");
        LOG.error(e.getMessage());
      }



      //  carve out json array 
      String json = "";
      boolean secondMatch = false;

      try {
        final String regex2 = "(?<=JSON-LD]=\\[).*?(?=,\\{\\\"@context\\\":\\\"https:\\/\\/schema.org\\\",\\\"@type\\\":\\\"BreadcrumbList)";
        final Pattern pattern2 = Pattern.compile(regex2, Pattern.MULTILINE);
        Matcher mat2 = pattern2.matcher(unencodedHTML);
        if (mat2.find()) {
          LOG.debug("found match for json data array");
          json=mat2.group(0);
          secondMatch = true;
        } else {
          LOG.warn("did not find match for json data array");
        }
      } catch (Exception e ) {
        LOG.error("unexpected har data");
        LOG.error(e.getMessage());
      }

      if ( !secondMatch ) {
        try {
          PrintStream filePrintStream = new PrintStream(new FileOutputStream("logs/AllEvents-failure."+Instant.now().toString()+".har"));
          filePrintStream.print(harString);
          filePrintStream.close();
        } catch (Exception e2) {
          LOG.error("error trying to save HAR file to LFS");
        }
        return null;
      }

      LOG.info("json start");
      LOG.info(json);
      LOG.info("json end");

      return json;

  }

}
