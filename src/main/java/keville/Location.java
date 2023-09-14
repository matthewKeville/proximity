package keville;

public class Location {

  //country, region, locality tries to emulate ISO-3166-2
  //https://en.wikipedia.org/wiki/ISO_3166-2
  public String name;
  public String country;
  public String region;
  public String locality;
  public String streetAddress;
  public Double latitude;
  public Double longitude;

  public Location(String name,
                  String country,
                  String region,
                  String locality,
                  String streetAddress,
                  Double latitude,
                  Double longitude) {
    this.name = name;
    this.country = country;
    this.region = region;
    this.locality = locality;
    this.streetAddress = streetAddress;
    this.latitude = latitude;
    this.longitude = longitude;    
  }

  public String toString() {

    String result = "";
    if ( name != null ) {
      result="name: " +  this.name;
    }

    if ( latitude != null &&  longitude != null ) {
      result += "\nlatitude : " + this.latitude + "\t longitude " + this.longitude;
    }

    if ( country != null ) {
      result+="\ncountry: " +  this.country;
    }

    if ( region != null ) {
      result+="\nregion: " +  this.region;
    }

    if ( locality != null ) {
      result+="\nlocality: " +  this.locality;
    }

    return result;
  }

}
