package keville;


import java.util.Map;
import java.util.HashMap;

//https://en.wikipedia.org/wiki/List_of_U.S._state_and_territory_abbreviations
//https://wikitable2csv.ggor.de/ (bless this site)
public class USStateAndTerritoryCodes {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(USStateAndTerritoryCodes.class);
  private static Map<String,USRegion> regions;

  static class USRegion {
    public String Type;
    public String ISO;
    public String ANSIL;
    public String ANSID;

  }

  static {
    regions = new HashMap<String,USRegion>();
    regions.put("Alabama",new USRegion() {{ Type = "State"; ISO = "US-AL"; ANSIL = "AL"; ANSID = "01"; }} );
    regions.put("Alaska",new USRegion() {{ Type = "State"; ISO = "US-AK"; ANSIL = "AK"; ANSID = "02"; }} );
    regions.put("Arizona",new USRegion() {{ Type = "State"; ISO = "US-AZ"; ANSIL = "AZ"; ANSID = "04"; }} );
    regions.put("Arkansas",new USRegion() {{ Type = "State"; ISO = "US-AR"; ANSIL = "AR"; ANSID = "05"; }} );
    regions.put("California",new USRegion() {{ Type = "State"; ISO = "US-CA"; ANSIL = "CA"; ANSID = "06"; }} );
    regions.put("Colorado",new USRegion() {{ Type = "State"; ISO = "US-CO"; ANSIL = "CO"; ANSID = "08"; }} );
    regions.put("Connecticut",new USRegion() {{ Type = "State"; ISO = "US-CT"; ANSIL = "CT"; ANSID = "09"; }} );
    regions.put("Delaware",new USRegion() {{ Type = "State"; ISO = "US-DE"; ANSIL = "DE"; ANSID = "10"; }} );
    regions.put("District of Columbia",new USRegion() {{ Type = "Federal district"; ISO = "US-DC"; ANSIL = "DC"; ANSID = "11"; }} );
    regions.put("Florida",new USRegion() {{ Type = "State"; ISO = "US-FL"; ANSIL = "FL"; ANSID = "12"; }} );
    regions.put("Georgia",new USRegion() {{ Type = "State"; ISO = "US-GA"; ANSIL = "GA"; ANSID = "13"; }} );
    regions.put("Hawaii",new USRegion() {{ Type = "State"; ISO = "US-HI"; ANSIL = "HI"; ANSID = "15"; }} );
    regions.put("Idaho",new USRegion() {{ Type = "State"; ISO = "US-ID"; ANSIL = "ID"; ANSID = "16"; }} );
    regions.put("Illinois",new USRegion() {{ Type = "State"; ISO = "US-IL"; ANSIL = "IL"; ANSID = "17"; }} );
    regions.put("Indiana",new USRegion() {{ Type = "State"; ISO = "US-IN"; ANSIL = "IN"; ANSID = "18"; }} );
    regions.put("Iowa",new USRegion() {{ Type = "State"; ISO = "US-IA"; ANSIL = "IA"; ANSID = "19"; }} );
    regions.put("Kansas",new USRegion() {{ Type = "State"; ISO = "US-KS"; ANSIL = "KS"; ANSID = "20"; }} );
    regions.put("Kentucky",new USRegion() {{ Type = "State"; ISO = "US-KY"; ANSIL = "KY"; ANSID = "21"; }} );
    regions.put("Louisiana",new USRegion() {{ Type = "State"; ISO = "US-LA"; ANSIL = "LA"; ANSID = "22"; }} );
    regions.put("Maine",new USRegion() {{ Type = "State"; ISO = "US-ME"; ANSIL = "ME"; ANSID = "23"; }} );
    regions.put("Maryland",new USRegion() {{ Type = "State"; ISO = "US-MD"; ANSIL = "MD"; ANSID = "24"; }} );
    regions.put("Massachusetts",new USRegion() {{ Type = "State"; ISO = "US-MA"; ANSIL = "MA"; ANSID = "25"; }} );
    regions.put("Michigan",new USRegion() {{ Type = "State"; ISO = "US-MI"; ANSIL = "MI"; ANSID = "26"; }} );
    regions.put("Minnesota",new USRegion() {{ Type = "State"; ISO = "US-MN"; ANSIL = "MN"; ANSID = "27"; }} );
    regions.put("Mississippi",new USRegion() {{ Type = "State"; ISO = "US-MS"; ANSIL = "MS"; ANSID = "28"; }} );
    regions.put("Missouri",new USRegion() {{ Type = "State"; ISO = "US-MO"; ANSIL = "MO"; ANSID = "29"; }} );
    regions.put("Montana",new USRegion() {{ Type = "State"; ISO = "US-MT"; ANSIL = "MT"; ANSID = "30"; }} );
    regions.put("Nebraska",new USRegion() {{ Type = "State"; ISO = "US-NE"; ANSIL = "NE"; ANSID = "31"; }} );
    regions.put("Nevada",new USRegion() {{ Type = "State"; ISO = "US-NV"; ANSIL = "NV"; ANSID = "32"; }} );
    regions.put("New Hampshire",new USRegion() {{ Type = "State"; ISO = "US-NH"; ANSIL = "NH"; ANSID = "33"; }} );
    regions.put("New Jersey",new USRegion() {{ Type = "State"; ISO = "US-NJ"; ANSIL = "NJ"; ANSID = "34"; }} );
    regions.put("New Mexico",new USRegion() {{ Type = "State"; ISO = "US-NM"; ANSIL = "NM"; ANSID = "35"; }} );
    regions.put("New York",new USRegion() {{ Type = "State"; ISO = "US-NY"; ANSIL = "NY"; ANSID = "36"; }} );
    regions.put("North Carolina",new USRegion() {{ Type = "State"; ISO = "US-NC"; ANSIL = "NC"; ANSID = "37"; }} );
    regions.put("North Dakota",new USRegion() {{ Type = "State"; ISO = "US-ND"; ANSIL = "ND"; ANSID = "38"; }} );
    regions.put("Ohio",new USRegion() {{ Type = "State"; ISO = "US-OH"; ANSIL = "OH"; ANSID = "39"; }} );
    regions.put("Oklahoma",new USRegion() {{ Type = "State"; ISO = "US-OK"; ANSIL = "OK"; ANSID = "40"; }} );
    regions.put("Oregon",new USRegion() {{ Type = "State"; ISO = "US-OR"; ANSIL = "OR"; ANSID = "41"; }} );
    regions.put("Pennsylvania",new USRegion() {{ Type = "State"; ISO = "US-PA"; ANSIL = "PA"; ANSID = "42"; }} );
    regions.put("Rhode Island",new USRegion() {{ Type = "State"; ISO = "US-RI"; ANSIL = "RI"; ANSID = "44"; }} );
    regions.put("South Carolina",new USRegion() {{ Type = "State"; ISO = "US-SC"; ANSIL = "SC"; ANSID = "45"; }} );
    regions.put("South Dakota",new USRegion() {{ Type = "State"; ISO = "US-SD"; ANSIL = "SD"; ANSID = "46"; }} );
    regions.put("Tennessee",new USRegion() {{ Type = "State"; ISO = "US-TN"; ANSIL = "TN"; ANSID = "47"; }} );
    regions.put("Texas",new USRegion() {{ Type = "State"; ISO = "US-TX"; ANSIL = "TX"; ANSID = "48"; }} );
    regions.put("Utah",new USRegion() {{ Type = "State"; ISO = "US-UT"; ANSIL = "UT"; ANSID = "49"; }} );
    regions.put("Vermont",new USRegion() {{ Type = "State"; ISO = "US-VT"; ANSIL = "VT"; ANSID = "50"; }} );
    regions.put("Virginia",new USRegion() {{ Type = "State"; ISO = "US-VA"; ANSIL = "VA"; ANSID = "51"; }} );
    regions.put("Washington",new USRegion() {{ Type = "State"; ISO = "US-WA"; ANSIL = "WA"; ANSID = "53"; }} );
    regions.put("West Virginia",new USRegion() {{ Type = "State"; ISO = "US-WV"; ANSIL = "WV"; ANSID = "54"; }} );
    regions.put("Wisconsin",new USRegion() {{ Type = "State"; ISO = "US-WI"; ANSIL = "WI"; ANSID = "55"; }} );
    regions.put("Wyoming",new USRegion() {{ Type = "State"; ISO = "US-WY"; ANSIL = "WY"; ANSID = "56"; }} );
  }


  public static String getANSILcode(String regionName) {

    USRegion region = regions.get(regionName);
    if ( region == null ) {
      LOG.warn("the us region : " + regionName + " is not currently supported by this class");
      return null;
    }
    return region.ANSIL;

  }

}
