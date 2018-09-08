package sheasmith.me.betterkamar.dataModels;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by TheDiamondPicks on 6/09/2018.
 */

public class CalendarObject
{
    public EventsResults EventsResults;

    public CalendarObject(String xml) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));

        Element root = (Element) doc.getElementsByTagName("EventsResults").item(0);
        NodeList events = root.getElementsByTagName("Events").item(0).getChildNodes();

        EventsResults results = new EventsResults();
        results.AccessLevel = root.getElementsByTagName("AccessLevel").item(0).getTextContent();
        results.ErrorCode = root.getElementsByTagName("ErrorCode").item(0).getTextContent();
        results.NumberRecords = root.getElementsByTagName("NumberRecords").item(0).getTextContent();

        for (int i = 0; i != events.getLength(); i++) {
            Element eventElement = (Element) events.item(i);
            Event event = new Event();
            event.index = eventElement.getAttribute("index");
            event.Title = eventElement.getElementsByTagName("Title").item(0).getTextContent();
            event.Location = eventElement.getElementsByTagName("Location").item(0).getTextContent();
            event.Details = eventElement.getElementsByTagName("Details").item(0).getTextContent();
            event.Priority = eventElement.getElementsByTagName("Priority").item(0).getTextContent();
            event.Public = eventElement.getElementsByTagName("Public").item(0).getTextContent();
            event.Student = eventElement.getElementsByTagName("Student").item(0).getTextContent();
            event.CG1 = eventElement.getElementsByTagName("CG1").item(0).getTextContent();
            event.CG2 = eventElement.getElementsByTagName("CG2").item(0).getTextContent();
            event.Staff = eventElement.getElementsByTagName("Staff").item(0).getTextContent();
            event.Colour = eventElement.getElementsByTagName("Colour").item(0).getTextContent();
            event.ColourLabel = eventElement.getElementsByTagName("ColourLabel").item(0).getTextContent();
            event.DateTimeInfo = eventElement.getElementsByTagName("DateTimeInfo").item(0).getTextContent();
            event.DateTimeStart = eventElement.getElementsByTagName("DateTimeStart").item(0).getTextContent();
            event.DateTimeFinish = eventElement.getElementsByTagName("DateTimeFinish").item(0).getTextContent();
            event.Start = eventElement.getElementsByTagName("Start").item(0).getTextContent();
            event.Finish = eventElement.getElementsByTagName("Finish").item(0).getTextContent();
        }
    }

    public class EventsResults
    {
        public String AccessLevel;

        public List<Event> Events = new ArrayList<>();

        public String ErrorCode;

        public String apiversion;

        public String NumberRecords;
    }

    public class Event
    {
        public String index;

        public String Staff;

        public String Start;

        public String Details;

        public String DateTimeStart;

        public String Finish;

        public String Location;

        public String Title;

        public String Priority;

        public String Student;

        public String CG1;

        public String Colour;

        public String ColourLabel;

        public String CG2;

        public String DateTimeFinish;

        public String DateTimeInfo;

        public String Public;
    }
}

