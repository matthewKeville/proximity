package keville;

import static org.junit.Assert.assertEquals;
import keville.util.GeoUtils;

import org.junit.Test;

public class GeoUtilsTest 
{

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GeoUtilsTest.class);

    //this is not comprehensive
    @Test
    public void getLocationFromGeoCoordinatesReturnsCorrectLocation() {
      double lat = 40.171200;
      double lon = -74.071700;
      keville.Location result = GeoUtils.getLocationFromGeoCoordinates(lat,lon);

      String expectedTown = "Wall Township";
      String expectedState = "New Jersey";

      assertEquals(expectedState,result.state);
      assertEquals(expectedTown,result.town);
    }
}
