package keville;

public class Location  {

  // loose mapping to https://geocode.maps.co
  public double latitude;
  public double longitude;
  public String state;
  public String town;
  public String village;

  public Location(double latitude,double longitude, String state, String town, String village) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.state = state;
    this.town = town;
    this.village = village;
  }

  public String toString() {
    String result = "";
    result+="latitude : " + latitude + "\t longitude " + longitude;
    if ( state != null ) {
      result+="\nstate : " + state;
    }
    if ( town != null ) {
      result+="\ntown : " + town;
    }
    if ( village  != null ) {
      result+="\nvillage : " + village;
    }
    return result;
  }

}
