package keville.unit;

import static org.junit.Assert.assertEquals;
import keville.util.GeoUtils;
import keville.location.Location;

import org.junit.Test;

public class GeoUtilsTest 
{

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GeoUtilsTest.class);

    //this is not comprehensive
    @Test
    public void getLocationFromGeoCoordinatesReturnsCorrectLocation() {
      double lat = 40.171200;
      double lon = -74.071700;
      Location result = GeoUtils.getLocationFromGeoCoordinates(lat,lon);

      String expectedLocality = "Wall Township";
      String expectedRegion = "NJ";

      assertEquals(expectedRegion,result.region);
      assertEquals(expectedLocality,result.locality);
    }
}
