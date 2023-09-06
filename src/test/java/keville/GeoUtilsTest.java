package keville;

import static org.junit.Assert.assertEquals;
import keville.util.GeoUtils;

import org.junit.Test;

public class GeoUtilsTest 
{

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GeoUtilsTest.class);

    @Test
    public void getLocationFromGeoCoordinatesReturnsCorrectLocation() {
      double lat = 40.171200;
      double lon = -74.071700;
      keville.Location result = GeoUtils.getLocationFromGeoCoordinates(lat,lon);

      String expectedCity = "Wall Township";
      String expectedState = "New Jersey";

      assertEquals(expectedState,result.state);
      assertEquals(expectedCity,result.city);
    }
}
