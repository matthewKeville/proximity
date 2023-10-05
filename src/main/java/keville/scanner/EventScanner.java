package keville.scanner;

public interface EventScanner  {

  ScanReport scan(double latitude,double longitude, double radius) throws Exception;

}
