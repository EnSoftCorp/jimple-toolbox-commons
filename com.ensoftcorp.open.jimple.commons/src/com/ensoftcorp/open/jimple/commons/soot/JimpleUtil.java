package com.ensoftcorp.open.jimple.commons.soot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ensoftcorp.abp.common.util.JimpleUtil.JimpleSource;

public class JimpleUtil {

	private static final String FILENAME = "jimplesource.xml";
	private static final String SOURCE = "JimpleSource";
	private static final String TYPE = "type";
	private static final String PATH = "path";
	private static final String ROOT = "root";

	/**
	 * Writes an XML file specifying the source of the jimple file.
	 * 
	 * @param type
	 * @param path
	 * @param outputDir
	 * @throws Exception 
	 */
	public static void writeHeaderFile(JimpleSource src, IFile jar, IFolder jimpleDirectory) throws Exception {
		// create an xml document to edit
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document jimpleSource = builder.newDocument();
		
		// add the xml root
		Element rootElement = jimpleSource.createElement(ROOT);
		jimpleSource.appendChild(rootElement);
		
		// add the source entry
		Element sourceElement = jimpleSource.createElement(SOURCE);
		rootElement.appendChild(sourceElement);
		
		// add source type attribute
		sourceElement.setAttribute(TYPE, src.name());
		
		// add the portable OS string path
		IFile projectResource = jar.getProject().getFile(jar.getProjectRelativePath());
		boolean isRelativePath = projectResource != null && projectResource.exists();
		if(isRelativePath){
			sourceElement.setAttribute(PATH, jar.getProjectRelativePath().toPortableString());
		} else {
			sourceElement.setAttribute(PATH, jar.getFullPath().toPortableString());
		}
		
		// write out the xml file
		writeJimpleSourceXML(jimpleDirectory, jimpleSource);
	}
	
	private static void writeJimpleSourceXML(IFolder jimpleFolder, Document jimpleSource) throws Exception {
		// remove blank lines
		// reference: https://stackoverflow.com/a/12670194/475329
		XPath xp = XPathFactory.newInstance().newXPath();
		NodeList nl = (NodeList) xp.evaluate("//text()[normalize-space(.)='']", jimpleSource, XPathConstants.NODESET);
		for (int i=0; i < nl.getLength(); ++i) {
		    Node node = nl.item(i);
		    node.getParentNode().removeChild(node);
		}
		// save the indented xml file
		File jimpleSourceFile = new File(new File(jimpleFolder.getLocation().toOSString()).getAbsolutePath() + File.separator + FILENAME);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
		transformer.transform(new DOMSource(jimpleSource), new StreamResult(new FileOutputStream(jimpleSourceFile)));
	}
}
