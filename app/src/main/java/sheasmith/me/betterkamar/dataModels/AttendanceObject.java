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

public class AttendanceObject
{
    public StudentAttendanceResults StudentAttendanceResults;

    public AttendanceObject(String xml) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));

        Element root = (Element) doc.getElementsByTagName("StudentAttendanceResults").item(0);
        NodeList weeks = root.getElementsByTagName("Weeks").item(0).getChildNodes();

        StudentAttendanceResults results = new StudentAttendanceResults();
        results.AccessLevel = root.getElementsByTagName("AccessLevel").item(0).getTextContent();
        results.ErrorCode = root.getElementsByTagName("ErrorCode").item(0).getTextContent();
        results.NumberRecords = root.getElementsByTagName("NumberRecords").item(0).getTextContent();

        for (int i = 0; i != weeks.getLength(); i++) {
            Element weekElement = (Element) weeks.item(i);
            Week week = new Week();
            week.WeekStart = weekElement.getElementsByTagName("WeekStart").item(0).getTextContent();
            week.index = weekElement.getAttribute("index");

            NodeList days = weekElement.getElementsByTagName("Days").item(0).getChildNodes();
            for (int j = 0; j != days.getLength(); j++) {
                Element dayElement = (Element) days.item(j);
                Day day = new Day();
                day.content = dayElement.getTextContent();
                day.index = dayElement.getAttribute("index");

                week.Days.add(day);
            }

            results.Weeks.add(week);
        }

        StudentAttendanceResults = results;
    }

    public class StudentAttendanceResults
    {
        public String AccessLevel;

        public List<Week> Weeks = new ArrayList<>();

        public String ErrorCode;

        public String apiversion;

        public String NumberRecords;
    }


    public class Day
    {
        public String content;

        public String index;
    }

    public class Week
    {
        public String index;

        public String WeekStart;

        public List<Day> Days = new ArrayList<>();
    }



}