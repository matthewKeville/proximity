package keville.scanner;

/* an EventScanner finds events specific to a provider */
public interface EventScanner  {
  ScanReport scan(double latitude,double longitude, double radius) throws Exception;
}
