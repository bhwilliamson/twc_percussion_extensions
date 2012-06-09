package com.weather.percussion.extensions.jexl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSJexlExpression;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSJexlUtilBase;



import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;


public class TWCLFCBodyUtil extends PSJexlUtilBase implements IPSJexlExpression {
    
    private static final Log log = LogFactory.getLog(TWCLFCBodyUtil.class);
    //private TestLog log = new TestLog();
    
    //For test case running
    private class TestLog {
        void debug(String str) {
            System.out.println(str);
        }
        void error(String str) {
            debug(str);
        }
        void error(Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public TWCLFCBodyUtil() {       
    }
    
    public String processBody(String input) {
        String retVal = input;
        log.debug("Processing body string: " + input);
        
        try {            
            StringBuilder wrappedInput = new StringBuilder();
            //Document needs top level node, so we will create one
            wrappedInput.append("<wrapper xmlns:wxnode=\"http://www.weather.com/ns/wxnode/\">").append(input).append("</wrapper>");
            
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(wrappedInput.toString().getBytes()));
            
            updateTableAlignments(document);

            //This will always be the wrapper node
            Node wrapperNode = document.getFirstChild();
            NodeList nodeList = wrapperNode.getChildNodes();
            
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node childNode = nodeList.item(i); 
                boolean removedWxNode = false;                
                if (Node.ELEMENT_NODE == childNode.getNodeType()) {
                    List<Element> wxNodes = removeContainedWxNodeElements(childNode);                   
                    for (Element wxNode : wxNodes) {
                        removedWxNode = true;
                        childNode.getParentNode().insertBefore(wxNode, childNode);
                    }
                } 
                
                if (removedWxNode && !childNode.hasChildNodes()) {
                    childNode.getParentNode().removeChild(childNode);
                }                
            }
           
            retVal = getXMLNodeAsString(document); 
            log.debug("Before cleaning output: " + retVal);
            
            //Clean up the resulting string
            if (retVal != null) {              
                retVal = retVal.replace("<wrapper xmlns:wxnode=\"http://www.weather.com/ns/wxnode/\">", "");
                retVal = retVal.replace("</wrapper>", "");
                
                //For HTML Output Solution uncomment the following 1 line:
                retVal = retVal.replace("></wxnode:module>", "/>");
                
                //For XML Output Solution uncomment the following 1 line:
                //retVal = retVal.replace("<p/>", "<p></p>");
                
                retVal = retVal.replace(System.getProperty("line.separator"), "");
            }
        }
        catch (Exception e) {
            log.error("Exception trying to parse wxNode elements");
            log.error(e);     
        }
        
        log.debug("Output: " + retVal);       
        return retVal;        
        
    }
    

    public String moveWxNodesOutsideMarkup(String input) {
        return processBody(input);
    }
    
    private List<Element> removeContainedWxNodeElements(Node node) {
        List<Element> retVal = new ArrayList<Element>();
        if (node == null) {
            return retVal;
        }       

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            if (Node.ELEMENT_NODE == childNode.getNodeType()) {
                if (childNode.getNodeName().startsWith("wxnode")) {
                    retVal.add((Element) childNode);
                    node.removeChild(childNode);                    
                }
                retVal.addAll(removeContainedWxNodeElements(childNode));
            }
        }
        
        return retVal;
    }
    
    private void updateTableAlignments(Document document) {
        NodeList tableNodeList = document.getElementsByTagName(TABLE_TAG);
        for (int i = 0; i < tableNodeList.getLength(); i++) {
            Element table = (Element) tableNodeList.item(i);
            NamedNodeMap attributes = table.getAttributes();
            Node alignAttributeNode = attributes.getNamedItem(ALIGN_ATTRIBUTE);
            if (alignAttributeNode != null && alignAttributeNode.getNodeType() == Node.ATTRIBUTE_NODE) {
                Attr alignAttribute = (Attr) alignAttributeNode;
                if (ALIGN_ATTRIBUTE_LEFT.equals(alignAttribute.getValue())) {
                    table.setAttribute(CLASS_ATTRIBUTE, CLASS_ATTRIBUTE_LEFT);
                }
                else if (ALIGN_ATTRIBUTE_RIGHT.equals(alignAttribute.getValue())) {
                    table.setAttribute(CLASS_ATTRIBUTE, CLASS_ATTRIBUTE_RIGHT);
                }
            }
        }
    }
    
    private String getXMLNodeAsString(Node node) throws TransformerFactoryConfigurationError, TransformerException {
        Transformer xmlTransformer = SAXTransformerFactory.newInstance().newTransformer();
        xmlTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        xmlTransformer.setOutputProperty(OutputKeys.INDENT, "no");
        
        //For xml output method comment out the following 1 line (default is "xml"):
        xmlTransformer.setOutputProperty(OutputKeys.METHOD, "html");
        
        Source xmlSource = new DOMSource(node);
        StreamResult xmlResult = new StreamResult(new ByteArrayOutputStream());
        xmlTransformer.transform(xmlSource, xmlResult);
        return new String(((ByteArrayOutputStream)xmlResult.getOutputStream()).toByteArray());         
    }
    
    private static final String TABLE_TAG = "table";
    private static final String ALIGN_ATTRIBUTE = "align";
    private static final String CLASS_ATTRIBUTE = "class";
    private static final String ALIGN_ATTRIBUTE_RIGHT = "right";
    private static final String ALIGN_ATTRIBUTE_LEFT = "left";
    private static final String CLASS_ATTRIBUTE_RIGHT = "wx-right";
    private static final String CLASS_ATTRIBUTE_LEFT = "wx-left";

}
