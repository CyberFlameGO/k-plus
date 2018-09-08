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

public class GlobalObject
{
    public GlobalsResults GlobalsResults;

    public GlobalObject(String xml) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));

        Element root = (Element) doc.getElementsByTagName("GlobalsResults").item(0);
        NodeList periodDefinitions = root.getElementsByTagName("PeriodDefinitions").item(0).getChildNodes();

        GlobalsResults results = new GlobalsResults();
        results.AccessLevel = root.getElementsByTagName("AccessLevel").item(0).getTextContent();
        results.ErrorCode = root.getElementsByTagName("ErrorCode").item(0).getTextContent();
        results.NumberRecords = root.getElementsByTagName("NumberRecords").item(0).getTextContent();

        for (int i = 0; i != periodDefinitions.getLength(); i++) {
            Element periodDefElement = (Element) periodDefinitions.item(i);

            PeriodDefinition definition = new PeriodDefinition();
            definition.index = periodDefElement.getAttribute("index");
            definition.PeriodName = periodDefElement.getElementsByTagName("PeriodName").item(0).getTextContent();
            definition.PeriodTime = periodDefElement.getElementsByTagName("PeriodTime").item(0).getTextContent();

            results.PeriodDefinitions.add(definition);
        }

        GlobalsResults = results;
    }

    public class GlobalsResults
    {
        public String AccessLevel;

        public String ErrorCode;

        public List<PeriodDefinition> PeriodDefinitions = new ArrayList<>();

        public String apiversion;

        public String NumberRecords;
    }

    public class PeriodDefinition
    {
        public String index;

        public String PeriodTime;

        public String PeriodName;
    }
}