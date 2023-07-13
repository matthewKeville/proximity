package keville;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.stream.Collectors;
import java.util.List;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Proximal {
  public static void main(String[] args) {

    System.out.println("Proximal version 0.1");
  
    try {

      //get the localhost IP address, if server is running on some other IP, you need to use that
      InetAddress host = InetAddress.getLocalHost();
      Socket socket = null;
      ObjectOutputStream oos = null;
      ObjectInputStream ois = null;

      socket = new Socket(host.getHostName(), 9876);
      oos = new ObjectOutputStream(socket.getOutputStream());
      System.out.println("Sending request to Socket Server");

      oos.writeObject("List"); //List request
      oos.writeObject("");//empty filter string

      //read the server response message
      ois = new ObjectInputStream(socket.getInputStream());
      String message = (String) ois.readObject();

      //read payload
      if ( message.equals("Okay") ) {

        System.out.println("Server Response: "+message);
        //List<JsonObject> eventsList = (List<JsonObject>) ois.readObject();
        List<String> eventsStringList = (List<String>) ois.readObject();

        List<JsonObject> events = eventsStringList.stream()
          .map(e -> JsonParser.parseString(e).getAsJsonObject() )
          .collect(Collectors.toList());

        System.out.println(events.toString());

      } else if ( message.equals("Unknown") ) {

        System.out.println("Server Response: "+message);

      } else {

        System.out.println("Server Response: "+message);
        System.out.println("miscommunication w/ target server");

      }

      //close resources
      ois.close();
      oos.close();
      Thread.sleep(100);

    } catch (
        IOException|
        ClassNotFoundException|
        InterruptedException e 
    ) {
      System.out.println("error connecting to server");
      System.out.println(e.getMessage());
    }


    System.out.println("After test");


  }
}
