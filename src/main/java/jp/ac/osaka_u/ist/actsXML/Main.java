package jp.ac.osaka_u.ist.actsXML;

import java.io.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

public class Main {
	
	public static void main(String[] args) throws Exception {
        
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(new File(args[0]));

		
		NodeList nl = document.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			traverse(nl.item(i), 0);
		}		
		
		System.out.println("================");
		String[] arg2 = new String[1];
		arg2[0] = args[0];
		main2(arg2);
	}

	private static void traverse(Node n, int depth) {
		if (n == null)
			return;
//		if (n.getNodeType() != Node.ELEMENT_NODE && n.getNodeType() != Node.TEXT_NODE)
//			return;
		String str1 = n.getNodeName();
		if (str1 == "Parameters") {
			processParameters(n);
			return;
		}
		
		String str3;
		String at1 ="";
		String at2 ="";
		if (n.hasAttributes()) {
			NamedNodeMap nnm = n.getAttributes();
			Node an = nnm.getNamedItem("name");
			if (an != null) {
				at1 = an.getNodeValue();
			}
			an = nnm.getNamedItem("text");
			if (an != null) {
				at2 = an.getNodeValue();
			}
		}
		
		if (checkTextNode(n)) 
			str3 = n.getNodeValue();
		else 
			str3 = "";
			
		for (int i = 0; i<depth; i++)
			System.out.print("----");
	
		System.out.println(str1 + str3 + " " + at1 + at2);
		
		NodeList nl = n.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			traverse(nl.item(i), depth+1);
		}
	}
	
	private static void processParameters(Node n) {
		
		NodeList parameters = n.getChildNodes();
		for (int parai=0; parai<parameters.getLength(); parai++) {
			Node para = parameters.item(parai);
			if (para.getNodeName() == "Parameter")
				processParameter(para);
		}
	}
	
	private static void processParameter(Node n) {
		NamedNodeMap nnm = n.getAttributes();
		Node an = nnm.getNamedItem("name");
		String name = an.getNodeValue();		
		System.out.print(name  + " " + "(");
		
		NodeList values = n.getChildNodes();
		for (int i = 0; i < values.getLength(); i++) {
			Node node = values.item(i);
			if (node.getNodeName() == "values") {
				processValues(node);
				break;
			}
		}
		System.out.println(")");
	}
	
	private static void processValues(Node node) {
		NodeList values = node.getChildNodes();
		for (int i=0; i< values.getLength(); i++) {
			Node value = values.item(i);
			if (value.getNodeName() == "value") {
				processValueName(value);
			}
		}	
	}

	private static void processValueName(Node node) {
		NodeList names = node.getChildNodes();
		for (int i=0; i< names.getLength(); i++) {
			Node name = names.item(i);
			if (name.getNodeType() == Node.TEXT_NODE) {
				String level = name.getNodeValue();
				System.out.print(level + " ");
			}
		}	
	}
	
	private static boolean checkTextNode(Node node) {
		if (node == null) return false;
		if (node.getNodeType() == Node.TEXT_NODE) 
			return true;
		return false;
	}
	
	public static void main2(String[] args) throws Exception {
        
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(new File(args[0]));

		NodeList nl = document.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			traverse2(nl.item(i), 0);
		}		
	}

	private static void traverse2(Node n, int depth) {
		if (n == null)
			return;
//		if (n.getNodeType() != Node.ELEMENT_NODE && n.getNodeType() != Node.TEXT_NODE)
//			return;
		String str1 = n.getNodeName();
		String str3;
		String at1 ="";
		String at2 ="";
		if (n.hasAttributes()) {
			NamedNodeMap nnm = n.getAttributes();
			Node an = nnm.getNamedItem("name");
			if (an != null) {
				at1 = "at_name:" + an.getNodeValue();
			}
			an = nnm.getNamedItem("text");
			if (an != null) {
				at2 = "at_value:" +an.getNodeValue();
			}
		}
		
		if (checkTextNode(n)) 
			str3 = n.getNodeValue();
		else 
			str3 = "";
			
		for (int i = 0; i<depth; i++)
			System.out.print("----");
	
		System.out.println(str1 + str3 + " " + at1 + at2);
		
		NodeList nl = n.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			traverse2(nl.item(i), depth+1);
		}
	}
	
	private static boolean checkTextNode2(Node node) {
		if (node == null) return false;
		if (node.getNodeType() == Node.TEXT_NODE) 
			return true;
		return false;
	}
}
