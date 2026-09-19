package dev.dpetzold.oaisearch.harvest;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class OaiParser {

    private static final String OAI = "http://www.openarchives.org/OAI/2.0/";
    private static final String DC = "http://purl.org/dc/elements/1.1/";

    private final DocumentBuilderFactory documentBuilderFactory = secureDocumentBuilderFactory();
    private final XPathFactory xPathFactory = XPathFactory.newInstance();

    OaiResponse parse(byte[] xml) {
        Document document = toDocument(xml);
        XPath xpath = newXPath();

        String errorCode = text(xpath, document, "/oai:OAI-PMH/oai:error/@code");
        if (!errorCode.isEmpty()) {
            throw new IllegalStateException(
                    "OAI error " + errorCode + ": " + text(xpath, document, "/oai:OAI-PMH/oai:error"));
        }

        List<OaiRecord> records = new ArrayList<>();
        for (Node node : nodes(xpath, document, "/oai:OAI-PMH/oai:ListRecords/oai:record")) {
            // deleted records come without <metadata>, nothing to map
            if ("deleted".equals(text(xpath, node, "oai:header/@status"))) {
                continue;
            }
            records.add(toRecord(xpath, node));
        }

        // the last page has no resumptionToken element at all
        String token = text(xpath, document, "/oai:OAI-PMH/oai:ListRecords/oai:resumptionToken");
        return new OaiResponse(List.copyOf(records), token.isBlank() ? null : token);
    }

    private OaiRecord toRecord(XPath xpath, Node record) {
        String identifier = text(xpath, record, "oai:header/oai:identifier");
        List<String> identifiers = texts(xpath, record, "oai:metadata/*/dc:identifier");

        return new OaiRecord(
                ppnFrom(identifier),
                text(xpath, record, "oai:metadata/*/dc:title"),
                texts(xpath, record, "oai:metadata/*/dc:creator"),
                text(xpath, record, "oai:metadata/*/dc:date"),
                texts(xpath, record, "oai:metadata/*/dc:subject"),
                texts(xpath, record, "oai:metadata/*/dc:language"),
                texts(xpath, record, "oai:metadata/*/dc:type"),
                identifiers.stream().filter(value -> value.startsWith("http")).findFirst().orElse(null));
    }

    // oai:digital.staatsbibliothek-berlin.de:PPN1877460214 -> PPN1877460214
    private static String ppnFrom(String identifier) {
        int colon = identifier.lastIndexOf(':');
        return colon < 0 ? identifier : identifier.substring(colon + 1);
    }

    private Document toDocument(byte[] xml) {
        try {
            return documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new IllegalStateException("cannot parse OAI response", e);
        }
    }

    private static DocumentBuilderFactory secureDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            // response comes from a foreign server, no external entities
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("cannot harden XML parser", e);
        }
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private XPath newXPath() {
        XPath xpath = xPathFactory.newXPath();
        xpath.setNamespaceContext(new StaticNamespaceContext(Map.of("oai", OAI, "dc", DC)));
        return xpath;
    }

    private static String text(XPath xpath, Object item, String expression) {
        try {
            return xpath.evaluate(expression, item).trim();
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(expression, e);
        }
    }

    private static List<String> texts(XPath xpath, Object item, String expression) {
        List<String> values = new ArrayList<>();
        for (Node node : nodes(xpath, item, expression)) {
            String value = node.getTextContent().trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static List<Node> nodes(XPath xpath, Object item, String expression) {
        try {
            NodeList nodeList = (NodeList) xpath.evaluate(expression, item, XPathConstants.NODESET);
            List<Node> result = new ArrayList<>(nodeList.getLength());
            for (int i = 0; i < nodeList.getLength(); i++) {
                result.add(nodeList.item(i));
            }
            return result;
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(expression, e);
        }
    }

    private record StaticNamespaceContext(Map<String, String> byPrefix) implements NamespaceContext {

        @Override
        public String getNamespaceURI(String prefix) {
            return byPrefix.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return byPrefix.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(namespaceURI))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            String prefix = getPrefix(namespaceURI);
            return prefix == null ? List.<String>of().iterator() : List.of(prefix).iterator();
        }
    }
}
