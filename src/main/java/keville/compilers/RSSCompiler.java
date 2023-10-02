package keville.compilers;

import keville.Event;

import java.util.List;
import java.util.function.Predicate;
import java.io.File;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class RSSCompiler extends EventCompiler {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RSSCompiler.class);

  public RSSCompiler(String name,Predicate<Event> filter,File file) {
    super(name,filter,file);

    if ( !Files.exists(file.toPath()) ) {
      LOG.info(file.toString() + " not found, creating rss xml base...");
      createRssXml();
    } 

    LOG.info("created new compiler for file " + file.toString());

  }

  // update RSS file
  // this  doesn't check if events are already items in the file
  // there isn't supposed to be duplications because discoveries is meant
  // to be events that were just found after a scan...
  public void compile(List<Event> discoveries) {

    if ( !Files.exists(file.toPath()) ) {

      LOG.warn("the file created for this rss feed was deleted, making a new one...");
      createRssXml();

    } else {

      LOG.info(file.toString() + " was found loading ...");

    }

    Document rss = loadRssXml();
    if ( rss == null ) {
      LOG.error("unable to compile events");
      return;
    }

    NodeList channels = rss.getElementsByTagName("channel");
    NodeList items = rss.getElementsByTagName("item");

    //could throw if xml is malformed
    Node channel;
    try {
      channel  = channels.item(0);
    } catch (Exception e) {
      LOG.error("unable to locate channel element in rss xml");
      LOG.error(e.getMessage());
      return;
    }


    int added = 0;
    for ( Event event : discoveries ) {

      //what if the nodes need updating ?  Needs rework when implementing Event Update Protocol
      
      if ( !itemAlreadyExists(items,event) ) {
        
        channel.appendChild(createItem(rss,event));
        added++;

      } else {
        LOG.warn("found an event that already exists as an item, item : " + Integer.toString(event.id));
      }

    }

    LOG.info("added " + added + " new events to feed");
    saveRssXml(rss);

  }

  // this feels wrong
  private boolean itemAlreadyExists(NodeList items, Event event) {

    String eventGuid = event.eventType.toString() + event.eventId;

    for ( int i = 0; i < items.getLength(); i++ ) {

        Node item = items.item(i);
        NodeList itemChildren = item.getChildNodes();

        // find the guid node
        Node guidNode = null; 
        for ( int j  = 0; j < itemChildren.getLength(); j++ ) {
          if ( itemChildren.item(j).getNodeName().equals("guid") ) {
            guidNode = itemChildren.item(j);
            break;
          }
        }

        if ( guidNode != null ) {
          if (guidNode.getTextContent().equals(eventGuid)) {
            return true;
          }
        }

    }

    return false;

  }

  private Node createItem(Document doc,Event event) {

    Element item = doc.createElement("item");
   
    Element titleNode = doc.createElement("title");
    titleNode.appendChild(doc.createTextNode(event.name));
    item.appendChild(titleNode);

    Element linkNode = doc.createElement("link");
    linkNode.appendChild(doc.createTextNode(event.url));
    item.appendChild(linkNode);

    Element descriptionNode = doc.createElement("description");
    descriptionNode.appendChild(doc.createTextNode(event.description));
    item.appendChild(descriptionNode);

    String eventGuid = event.eventType.toString() + event.eventId;

    Element guidNode = doc.createElement("guid"); //the row in the db
    guidNode.appendChild(doc.createTextNode(eventGuid));
    item.appendChild(guidNode);

    return item;

  }


  private Document loadRssXml() {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = factory.newDocumentBuilder();
      Document doc = dBuilder.parse(file);
      return doc;
    } catch (Exception e) {
      LOG.error("error loading rss xml");
      LOG.error(e.getMessage());
    }
    return null;
  }


  private void createRssXml() {

    LOG.info("creating inital feed");

    String feedTitle = "a testing feed";
    String feedDescription = "this is a testing for proximity";
    String xml = ""
      + "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
      + "<rss version=\"2.0\">"
      + "<channel>"
      + " <title>"+feedTitle+"</title>"
      + " <link>file://"+file.getPath()+"</link>"
      + " <description>"+feedDescription+"</description>"
      + "</channel>"
      + "</rss> ";

    try  {
      Files.writeString(file.toPath(),xml);
    } catch (Exception e) {
      LOG.error("unable to create file");
      LOG.error(e.getMessage());
    }
  }


  private void saveRssXml(Document doc) {

    try {

      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transf = transformerFactory.newTransformer();

      transf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transf.setOutputProperty(OutputKeys.INDENT, "yes");
      transf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

      DOMSource source = new DOMSource(doc);

      StreamResult xfile = new StreamResult(file);

      transf.transform(source, xfile); 

    } catch (Exception e) {

      LOG.error("unable to save rss");
      LOG.error(e.getMessage());

    }

  }

  

}
