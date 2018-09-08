package sheasmith.me.betterkamar.dataModels;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by TheDiamondPicks on 6/09/2018.
 */

public class AbsenceObject {
    public StudentAbsenceStatsResults StudentAbsenceStatsResults;

    public AbsenceObject(String xml) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));

        Element root = (Element) doc.getElementsByTagName("StudentAbsenceStatsResults").item(0);
        Element studentElement = (Element) ((Element) root.getElementsByTagName("Students").item(0)).getElementsByTagName("Student");

        StudentAbsenceStatsResults results = new StudentAbsenceStatsResults();
        results.AccessLevel = root.getElementsByTagName("AccessLevel").item(0).getTextContent();
        results.ErrorCode = root.getElementsByTagName("ErrorCode").item(0).getTextContent();
        results.NumberRecords = root.getElementsByTagName("NumberRecords").item(0).getTextContent();

        Student student = new Student();
        student.HalfDaysJ = studentElement.getElementsByTagName("HalfDaysJ").item(0).getTextContent();
        student.HalfDaysU = studentElement.getElementsByTagName("HalfDaysU").item(0).getTextContent();
        student.HalfDaysO = studentElement.getElementsByTagName("HalfDaysO").item(0).getTextContent();
        student.HalfDaysT = studentElement.getElementsByTagName("HalfDaysT").item(0).getTextContent();
        student.HalfDaysOpen = studentElement.getElementsByTagName("HalfDaysOpen").item(0).getTextContent();
        student.FullDaysJ = studentElement.getElementsByTagName("FullDaysJ").item(0).getTextContent();
        student.FullDaysU = studentElement.getElementsByTagName("FullDaysU").item(0).getTextContent();
        student.FullDaysT = studentElement.getElementsByTagName("FullDaysT").item(0).getTextContent();
        student.FullDaysOpen = studentElement.getElementsByTagName("FullDaysOpen").item(0).getTextContent();
        student.PctgeU = studentElement.getElementsByTagName("PctgeU").item(0).getTextContent();
        student.PctgeO = studentElement.getElementsByTagName("PctgeO").item(0).getTextContent();
        student.PctgeT = studentElement.getElementsByTagName("PctgeT").item(0).getTextContent();
        student.PctgeP = studentElement.getElementsByTagName("PctgeP").item(0).getTextContent();

        results.Student = student;

        StudentAbsenceStatsResults = results;
    }

    public class StudentAbsenceStatsResults {
        public String AccessLevel;

        public String ErrorCode;

        public Student Student;

        public String apiversion;

        public String NumberRecords;
    }

    public class Student {
        public String FullDaysO;

        public String index;

        public String HalfDaysT;

        public String HalfDaysU;

        public String FullDaysU;

        public String PctgeJ;

        public String FullDaysT;

        public String HalfDaysJ;

        public String HalfDaysO;

        public String FullDaysJ;

        public String HalfDaysOpen;

        public String FullDaysOpen;

        public String PctgeT;

        public String PctgeU;

        public String PctgeO;

        public String PctgeP;
    }
}