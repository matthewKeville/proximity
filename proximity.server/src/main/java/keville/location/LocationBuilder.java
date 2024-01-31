package keville.location;



public class LocationBuilder {

  public String name;
  public String country;
  public String region;
  public String locality;
  public String streetAddress;
  public Double latitude;
  public Double longitude;


  public void setName(String name) {
    this.name = name;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public void setLocality(String locality) {
    this.locality = locality;
  }

  public void streetAddress(String streetAddress) {
    this.streetAddress = streetAddress;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public Location build() {

    Location location = 
      new Location(
          name,
          country,
          region,
          locality,
          streetAddress,
          latitude,
          longitude
      );

    return location;
  }


}


