package keville;

import java.util.List;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Proximal {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Proximal.class);

  public static void main(String[] args) {

    LOG.info("Proximal version 0.1");
  
    try {

      //get the localhost IP address, if server is running on some other IP, you need to use that
      InetAddress host = InetAddress.getLocalHost();
      Socket socket = null;
      ObjectOutputStream oos = null;
      ObjectInputStream ois = null;

      socket = new Socket(host.getHostName(), 9876);
      socket.setSoTimeout(5000/*ms*/);
      oos = new ObjectOutputStream(socket.getOutputStream());
      LOG.info("Sending request to Socket Server");

      oos.writeObject("List"); //List request
      oos.writeObject("");//empty filter string

      //read the server response message
      ois = new ObjectInputStream(socket.getInputStream());
      String message = (String) ois.readObject();

      //read payload
      if ( message.equals("Okay") ) {

        LOG.info("Server Response: "+message);
        List<Event> events = (List<Event>) ois.readObject();
        for (Event e : events ) {
          System.out.println(e.toColorString()+"\n");
        }

      } else if ( message.equals("Unknown") ) {

        LOG.info("Server says unknown request: "+message);

      } else {

        LOG.info("miscommunication w/ target server");
        LOG.info("Server Response: "+message);
        LOG.info("miscommunication w/ target server");

      }

      //close resources
      ois.close();
      oos.close();

    } catch (
        IOException|
        ClassNotFoundException e
    ) {
      LOG.error("error connecting to server");
      LOG.error(e.getMessage());
    }

  }
}
