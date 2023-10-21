package app.util;

import static java.net.HttpURLConnection.HTTP_OK;				// HTTP Status-Code 200: OK.
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;		// HTTP Status-Code 404: Not Found.

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
// import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import app.config.Config;

import org.w3c.dom.Element;

import org.apache.commons.text.StringEscapeUtils;

/**
 * This class consists mainly of static methods that operate on or return XML 
 * structures from Web Intelligence RESTful Web Services.
 * <p>
 * <u>Pre-requisites</u><br>
 * The major step is to fill the Config.java file in the com.sap.webi.raylight.param
 * package with your own values:<br>
 * <ul>
 * 	<li>CMS_SERVER_URL</li>
 * 	<li>CMS_USER</li>
 * 	<li>CMS_PASS</li>
 * 	<li>CMS_AUTH</li>
 * </ul>
 * <br>
 * <u>Universes</u><br>
 * The samples are based on the Warehouse.unx and eFashion.unv/unx universes 
 * that are preinstalled in the CMS repository.<br>
 * <br>
 * <u>Errors</u><br>
 * The methods of this class all throw an <i>Exception</i> if the XML files, 
 * parameters or class objects provided to them are inappropriate.<br>
 * </p>
 */
//@SuppressWarnings("nls")
public class RestFulUtil extends Config {

	/**
	 * Logs in to the CMS.
	 * 
	 * <p>Once logged in, the URLs to <b>BIP_RWS</b> and <b>RL_RWS</b> are available
	 * through the static members with these names.</p>
	 * 
	 * @throws 			Exception
	 * @throws 			RuntimeException if the port number has not been set
	 */
	public static void restLogon() throws Exception {
		// Checks if a port has been set, otherwise default it to 6405.
		URL url = new URL(CMS_SERVER_URL);
		if (url.getPort() == -1) {
			throw new RuntimeException("The server port has not been correctly set.");
		}

		// Sends the logon request
		RestFulRequest request = new RestFulRequest();
		request.send(BIP_RWS + "/logon/long", "GET", null);

		// Sets logon information
		Map<String, String> map = new HashMap<String, String>();
		map.put("//attr[@name='userName']", CMS_USER);
		map.put("//attr[@name='password']", CMS_PASS);
		map.put("//attr[@name='auth']", CMS_AUTH);

		String filledLogonResponse = fillXml(request.getResponseContent(), map);

		// Posts logon information
		request.send(BIP_RWS + "/logon/long", "POST", filledLogonResponse, APP_XML, APP_XML);

		logonToken = request.getResponseHeaders().get("X-SAP-LogonToken").get(0);
	}

	/**
	 * Logs off from the CMS.
	 * 
	 * @throws 			Exception
	 */
	public static void restLogoff() throws Exception {
		if (logonToken != null) {
			RestFulRequest request = new RestFulRequest(logonToken);
			request.send(BIP_RWS + "/logoff", "POST", null, null, APP_XML);
			logonToken = null;
		}
	}

	/**
	 * Creates a new document.
	 * The XML structure is filled with both variables (name & folder)
	 * 
	 * @param docName	The name for the new document
	 * @param folderID	The folder in which you want to create your file
	 * @return 			A <code>String</code> containing the ID of the document
	 * @throws			Exception
	 */
	protected static String createDocument(String docName, String folderID) 
			throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		String newDocXml = "<document>" +
				"<name>" + docName + "</name>" + 
				"<folderId>" + folderID + "</folderId>" +
				"</document>";

		// Creates a new empty document
		request.send(RL_RWS + "/documents", "POST", newDocXml, APP_XML, APP_XML);
		
		// Blocking Point, if not 200: End of run.
		Assert.assertEquals(HTTP_OK, request.getResponseCode());

		// Reads XML code
		Document response = getXmlDocument(request.getResponseContent());
		
		// Returns the new doc ID 
		return evaluateAsString(response, "//id/text()");
	}

	/**
	 * Deletes a specific document from the CMS repository.
	 * 
	 * @param docID		The ID of the document you want to delete
	 * @throws			Exception
	 */
	protected static void deleteDocument(String docID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Deletes the document 
		request.send(RL_RWS + "/documents/" + docID, "DELETE", null, null, APP_XML);
	}
	
	/**
	 * Saves all changes for a specific document.
	 * The same XML as the one sent for creation is sent, but since it 
	 * already exists, it overwrites it and therefore, makes a save of changes.
	 * 
	 * @param docID		The ID of the document you want to save
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @since 			4.1 SP4
	 */
	protected static void saveDocument(String docID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Gets the XML of the document to save
		request.send(RL_RWS + "/documents/" + docID, "GET", null, APP_XML, APP_XML);

		// Retrieves <name> & <folderId>
		Document response = getXmlDocument(request.getResponseContent());
		
		// String name = evaluateAsString(response, "//name/text()");		// Error with XML escape characters
		
		// Fix XML escape characters
		String name = StringEscapeUtils.escapeXml11(evaluateAsString(response, "//name/text()"));
		String folderId = evaluateAsString(response, "//folderId/text()");

		String xmlDocToSave = 	"<document>" +
				"<name>" + name + "</name>" + 
				"<folderId>" + folderId + "</folderId>" +
				"</document>";

		// Saves it!
		request.send(RL_RWS + "/documents/" + docID, "PUT", xmlDocToSave, APP_XML, APP_XML);
		
		// If not 200: save failed.
//		Assert.assertEquals(HTTP_OK, request.getResponseCode());
		if (HTTP_OK != request.getResponseCode()) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			String timeStamp = LocalDateTime.now().format(formatter);
			System.err.println(timeStamp + ", Error, Save document failded, Response= "  + request.getResponseCode() + ", DocID= " + docID + ", Name= " + name + ", FolderID= " + folderId);
		}
	}

	/**
	 * Saves all changes for a specific document.
	 * The same XML as the one sent for creation is sent, but since it 
	 * already exists, it overwrites it and therefore, makes a save of changes.
	 * 
	 * @param docID		The ID of the document you want to save
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @since 			4.1 SP4
	 */
	protected static void saveDocument_xml(String docID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Gets the XML of the document to save
		request.send(RL_RWS + "/documents/" + docID, "GET", null, APP_XML, APP_XML);

		// Retrieves <name> & <folderId>
		Document response = getXmlDocument(request.getResponseContent());
		 String name = evaluateAsString(response, "//name/text()");
		 String folderId = evaluateAsString(response, "//folderId/text()");

		// create a document
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		Element documentElement = doc.createElement("document");
		doc.appendChild(documentElement);
		
		// Add a name node under document node
		Element childElement = doc.createElement("name");
		childElement.appendChild(doc.createTextNode(name));
		documentElement.appendChild(childElement);
		
		// Add a folder node under document node
		childElement = doc.createElement("folderId");
		childElement.appendChild(doc.createTextNode(folderId));
		documentElement.appendChild(childElement);
		
		// Transform result
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StreamResult streamResult = new StreamResult(stream);
		transformer.transform(new DOMSource(doc), streamResult);
		String xmlDocToSave = stream.toString();
		 
		// Saves it!
		request.send(RL_RWS + "/documents/" + docID, "PUT", xmlDocToSave, APP_XML, APP_XML);
		
		// If not 200: save failed.
		Assert.assertEquals(HTTP_OK, request.getResponseCode());
	}

	/**
	 * Copies (duplicates) an existing document.
	 * 
	 * @param docID		The ID of the document you want to make a copy
	 * @param newName	The name of your copy (if 'null', the default name 
	 * 					is the same as the initial document with "[1]")
	 * @return 			A <code>String</code> containing the ID of the new copied document
	 * @throws			Exception
	 * @since 			4.0 SP5
	 */
	protected static String copyDocument(String docID, String newName) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		if (newName != null) {
			String propertiesName = "<document>" +
					"<name>" + 
					newName +
					"</name>" + 
					"</document>";

			// Creates a new copy from an existing document with a custom name
			request.send(RL_RWS + "/documents?sourceId=" +  docID, "POST", propertiesName, APP_XML, APP_XML);

		} else {
			// Creates a new copy about an existing document
			request.send(RL_RWS + "/documents?sourceId=" +  docID, "POST", null, null, APP_XML);
		}

		Document response = getXmlDocument(request.getResponseContent());

		// Returns the new doc ID
		return evaluateAsString(response, "/success/id/text()");
	}
	
	/**
	 * Gets the ID from a CUID number.
	 * 
	 * @param CUID		The CUID to look for
	 * @return 			A <code>String</code> containing the ID number
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 */
	protected static String getIdFromCUID(String CUID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Gets infostore about CUID number
		request.send(BIP_RWS + "/infostore/cuid_" +  CUID, "GET", null);
		Document response = getXmlDocument(request.getResponseContent());

		// If not 200: CUIDs not found ? In all cases, IDs had not been retrieved
		Assert.assertEquals(HTTP_OK, request.getResponseCode());
		
		// Returns the ID 
		return evaluateAsString(response, "//attr[@name='id']/text()");
	}
	
	/**
	 * Adds a report (new tab) to a specific document.
	 * 
	 * @param docID		The ID of the document to add report
	 * @param name		The name of your new report
	 * @param orientation The page orientation ('Portrait' by default)
	 * @return 			A <code>String</code> containing the ID of the new report
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 */
	protected static String addReport(String docID, String name, String orientation) 
			throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);
		
		// Creates the XML report (with default page values)
		String reportContent = "<report>" +
								"<name>" + name + "</name>" +
								"<pageSettings>" +
									"<margins left='2835' right='2835' top='2835' bottom='2835'/>" +
									"<format orientation='" + orientation + "' />" +
									"<records vertical='100' horizontal='20'/>" +
									"<scaling factor='100'/>" +
								"</pageSettings>" +
								"</report>";

		// Adds the report to the document given as parameter
		request.send(RL_RWS + "/documents/" + docID + "/reports", "POST", reportContent, APP_XML, APP_XML);

		// Returns the new report ID
		Document response = getXmlDocument(request.getResponseContent());
		return evaluateAsString(response, "//id/text()");
	}
	
	/**
	 * Gets all report IDs contained in a specific document. 
	 * 
	 * @param docID		The ID of the document you want to use
	 * @return 			A <code>List<String></code> containing all the report IDs
	 * @throws			Exception
	 * @see				List
	 * @since			4.0 SP5
	 */
	protected static List<String> getAllReports(String docID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);
		List<String> reportIDs = new ArrayList<String>();

		// Gets all reports
		request.send(RL_RWS + "/documents/" + docID + "/reports", "GET", null, APP_XML, APP_XML);
		Document response = getXmlDocument(request.getResponseContent());

		// Gets all IDs
		XPathExpression expr = xPath.compile("/reports/report/id/text()");
		NodeList nodeList = (NodeList) expr.evaluate(response, XPathConstants.NODESET);

		// Adds all IDs at List<String>
		for (int i = 0; i < nodeList.getLength(); i++) {
			reportIDs.add(nodeList.item(i).getNodeValue());
		}

		// Returns a List<String> of IDs
		return reportIDs;	
	}
	
	/**
	 * Deletes a report (tab) from a specific document.
	 * 
	 * @param docID		The ID of the document in which you want to delete the report
	 * @param reportID	The ID of the report you want to delete
	 * @throws			Exception
	 */
	protected static void deleteReport(String docID, String reportID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Deletes a specific Report 
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID,
				"DELETE", null, null, APP_XML);
	}
	
	/**
	 * Renames a report from a specific document.
	 * 
	 * @param docID		The ID of the document in which you want to rename the report
	 * @param reportID	The ID of the report you want to rename
	 * @throws			Exception
	 */
	protected static void renameReport(String docID, String reportID, String newName) 
			throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Builds the new XML used to rename the report
		String newXmlReport = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
							"<report>" +
							"<name>" + newName + "</name>" + 
							"</report>";

		// Updates the report name
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID,
				"PUT", newXmlReport, APP_XML, APP_XML);
	}
	
	/**
	 * Adds a data provider to an existing document.
	 * 
	 * @param docID		The ID of the document 
	 * @param dpCUID	The data provider CUID that will be added
	 * @return 			A <code>String</code> containing the new data provider ID
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 */
	protected static String addDataProvider(String docID, String dpCUID) throws Exception {
		return addDataProvider(docID, dpCUID, null);
	}
	
	/**
	 * Adds a data provider (for FreeHandSQL) to an existing document.
	 * 
	 * @param docID		The ID of the document
	 * @param dpCUID	The data provider CUID that will be added
	 * @param sql		All SQL requests
	 * @return 			A <code>String</code> containing the new DataProvider ID
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 */
	protected static String addDataProvider(String docID, String dpCUID, List<String> sql) 
			throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Gets the universe ID from CUID
		String dpID = getIdFromCUID(dpCUID);
		String dpName = getDataProviderName(dpCUID);
		String xmlDataProvider = null;

		// Updates the data provider XML file
		if (sql == null) {
			xmlDataProvider = "<dataprovider>" + 
					"<name>"+ dpName + "(" + Now() + ")" + "</name>" +
					"<dataSourceId>" + dpID + "</dataSourceId>" + 
					"</dataprovider>";
		} else {
			xmlDataProvider = "<dataprovider>" + 
					"<name>"+ dpName + "(" + Now() + ")" + "</name>" +
					"<dataSourceId>" + dpID + "</dataSourceId>" + 
					"<properties>";
			for (int i = 0 ; i < sql.size() ; i++) {
				xmlDataProvider = xmlDataProvider + "<property key='sql'>" + 
													sql.get(i).toString() +
													"</property>";
			}
			xmlDataProvider = xmlDataProvider + "</properties></dataprovider>";
		}

		// Adds the new data provider to the document
		request.send(RL_RWS + "/documents/"+ docID +"/dataproviders",
				"POST", xmlDataProvider, APP_XML, APP_XML);
		
		// If not 200: Problem during add the new data provider.
		Assert.assertEquals(HTTP_OK, request.getResponseCode());

		// Returns the New data provider ID
		Document response = getXmlDocument(request.getResponseContent());
		return evaluateAsString(response, "/success/id/text()");
	}
	
	/**
	 * Gets all data provider IDs.
	 * 
	 * @param docID		The ID of the document
	 * @return 			A <code>List<String></code> containing all data provider IDs
	 * @throws			Exception
	 * @see				List
	 * @see				NodeList
	 * @see				XPathExpression
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @since			4.0 SP5
	 */
	protected static List<String> getDataProvidersIDs(String docID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);
		List<String> listDP = new ArrayList<String>();

		request.send(RL_RWS + "/documents/" + docID + "/dataproviders", "GET", null);
		Document response = getXmlDocument(request.getResponseContent());

		// Gets all IDs
		XPathExpression expr = xPath.compile("//id/text()");
		NodeList nodeList = (NodeList) expr.evaluate(response, XPathConstants.NODESET);

		// All data provider IDs added to a list of Strings
		for (int i = 0; i < nodeList.getLength(); i++) {
			listDP.add(nodeList.item(i).getNodeValue());
			// System.out.println(nodeList.item(i).getNodeValue());
		}
		return listDP;
	}

	/**
	 * Gets all Webi IDs.
	 * 
	 * @param docID		The ID of the document
	 * @return 			A <code>List<String></code> containing Webi IDs in this bulk
	 * @throws			Exception
	 * @see				List
	 * @see				NodeList
	 * @see				XPathExpression
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @since			4.0 SP5
	 */
	protected static List<String> getDocumentsIDs(int offset,int limit) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);
		List<String> listDocID = new ArrayList<String>();

		request.send(RL_RWS + "/documents/?offset=" + offset + "&limit=" + limit, "GET", null);

        switch (request.getResponseCode()) {

        // HTTP Status-Code 404: Not Found, no more WebI found in this batch       
        case HTTP_NOT_FOUND:
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			String timeStamp = LocalDateTime.now().format(formatter);
			System.out.println(timeStamp + ", Info, No more Webi found., Response= "  + request.getResponseCode() + ", Offset= " + offset + ", limit= " + limit);
        	break;

        case HTTP_OK:
    		Document response = getXmlDocument(request.getResponseContent());

    		// Gets all IDs
    		XPathExpression expr = xPath.compile("//id/text()");
    		NodeList nodeList = (NodeList) expr.evaluate(response, XPathConstants.NODESET);

    		// All data provider IDs added to a list of Strings
    		for (int i = 0; i < nodeList.getLength(); i++)
    			listDocID.add(nodeList.item(i).getNodeValue());
    		break;
    		
        default:
    		// Blocking Point, if not 200: End of run.
    		Assert.assertEquals(HTTP_OK, request.getResponseCode());
           break;
        }
    
		return listDocID;
	}

	
	protected static List<String> getDocumentsIDs_old(int offset,int limit) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);
		List<String> listDocID = new ArrayList<String>();

		request.send(RL_RWS + "/documents/?offset=" + offset + "&limit=" + limit, "GET", null);

		// HTTP Status-Code 404: Not Found, no more WebI found in this batch
		if (HTTP_NOT_FOUND == request.getResponseCode()) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			String timeStamp = LocalDateTime.now().format(formatter);
			System.out.println(timeStamp + ", Info, No more Webi found., Response= "  + request.getResponseCode() + ", Offset= " + offset + ", limit= " + limit);
			return listDocID;
		}
		// Blocking Point, if not 200: End of run.
		Assert.assertEquals(HTTP_OK, request.getResponseCode());

		Document response = getXmlDocument(request.getResponseContent());

		// Gets all IDs
		XPathExpression expr = xPath.compile("//id/text()");
		NodeList nodeList = (NodeList) expr.evaluate(response, XPathConstants.NODESET);

		// All data provider IDs added to a list of Strings
		for (int i = 0; i < nodeList.getLength(); i++)
			listDocID.add(nodeList.item(i).getNodeValue());
		
		return listDocID;
	}

	/**
	 * Gets the data provider name from CUID.
	 * 
	 * @param dpCUID	The data provider CUID to look for
	 * @return 			A <code>String</code> containing the data provider name
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 * @since			4.0 SP5
	 */
	protected static String getDataProviderName(String dpCUID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Gets infostore about CUID number
		request.send(BIP_RWS + "/infostore/cuid_" + dpCUID, "GET", null);
		Document response = getXmlDocument(request.getResponseContent());

		// Returns the ID 
		return evaluateAsString(response, "//title/text()");
	}
	
	/**
	 * Gets the data provider details.
	 * 
	 * @param docID		The ID of the document
	 * @param dpID		The data provider ID to look for
	 * @return 			A <code>String</code> containing the data provider details
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 */
	protected static String getDataProvider(String docID, String dpID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Gets infostore about CUID
		request.send(RL_RWS + "/documents/" + docID + "/dataproviders/" + dpID, "GET", null);

		// Returns the data provider details
		return request.getResponseContent();
	}

	/**
	 * Purges all data with specific doc and data provider.
	 * 
	 * @param docID		The ID of the document
	 * @param dpID		The ID of the data provider you want to purge
	 * @throws			Exception
	 */
	protected static void purgeDataProvider(String docID, String dpID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Purges data
		request.send(RL_RWS + "/documents/" + docID + "/dataproviders/" + dpID + "?purge=true",
				"PUT", null, null, APP_XML);
	}
	
	/**
	 * Gets the type of a given connection.
	 * 
	 * @param connectionID The ID of the document in which you want to add a query specification
	 * @return 			A <code>String</code> containing the type of the connection
	 * @throws			Exception
	 */
	protected static String getTypeOfConnection(String connectionID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);
		
		// Gets the connection details
		request.send(RL_RWS + "/connections/" + connectionID,
				"GET", null, null, APP_XML);
		Document response = getXmlDocument(request.getResponseContent());

		// Returns the type of connection
		return evaluateAsString(response, "//connection/@type");
	}
	
	/**
	 * Adds a query specification to a document.
	 * 
	 * @param docID		The ID of the document in which you want to add a query specification
	 * @param dpID		The ID of the data provider used by the query specification
	 * @param qsContent	The content of the query specification (XML formatted file as {@link String}) 
	 * @throws			Exception
	 */
	protected static void addQuerySpecification(String docID, String dpID, String qsContent) 
			throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Adds the query specification
		request.send(RL_RWS + "/documents/" + docID + "/dataproviders/" + dpID + "/specification",
				"PUT", qsContent, TXT_XML, APP_XML);
	}

	/**
	 * Gets the query specification for a specific data provider.
	 * 
	 * @param docID		The ID of the document
	 * @param dpID		The ID of the data provider to look for
	 * @return 			A <code>String</code> containing the content of the query specification
	 * @throws			Exception
	 */
	protected static String getQuerySpecification(String docID, String dpID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Gets the query specification
		request.send(RL_RWS + "/documents/" + docID + "/dataproviders/" + dpID + "/specification", 
				"GET", null, null, TXT_XML);

		return request.getResponseContent();
	}
	
	/**
	 * Gets the query specification for a specific data provider.
	 * 
	 * @param docID		The ID of the document
	 * @param reportID	The ID of the report in which you want to add an input control
	 * @param xmlInputControl The XML content of the input control
	 * @return 			A <code>String</code> containing the ID of the input control
	 * @throws			Exception
	 * @since			4.1 SP3
	 */
	protected static String addInputControl(String docID, String reportID, String xmlInputControl) 
			throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);
		
		// Adds the input control
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID + "/inputcontrols", 
				"POST", xmlInputControl, APP_XML, APP_XML);
		Document response = getXmlDocument(request.getResponseContent());

		// Returns the input control ID
		return evaluateAsString(response, "//id/text()");
	}

	/**
	 * Adds an element to a specific document report.
	 * 
	 * @param docID		The ID of the document
	 * @param reportID	The ID of the report you want to add an element
	 * @param containerID Your element must be contained in a parent element 
	 * 					(default values are header(1), body(2), footer(3))
	 * @return 			A <code>String</code> containing the ID of the new variable
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 * @see 			app.util.RestFulUtil#fillXml(String, Map)
	 * @since			4.1 SP2
	 */
	protected static String addElement(String docID, String reportID, String containerID, 
			String element) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Updates the parentId's tag
		Map<String, String> map = new HashMap<String, String>();
		map.put("/element/parentId", containerID);

		String elementXml = fillXml(element, map);

		// Adds the element to the report
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID + "/elements",
				"POST", elementXml, APP_XML, APP_XML);
		Document response = getXmlDocument(request.getResponseContent());

		// Returns the new element ID
		return evaluateAsString(response, "//id/text()");
	}

	/**
	 * Adds an empty row or column to a table.
	 * <p>
	 * This method adds an empty row or column with no values.</p>
	 * 
	 * @param docID		The ID of the document
	 * @param reportID	The ID of the report where your table is located
	 * @param elementID	The element ID which serves as reference to add your row or column
	 * @param strip		Accepts: 'row' or 'column'
	 * @param position	Accepts for columns: 'left' or 'right'
	 * 					Accepts for row: 'above' or 'below'
	 * @return 			A <code>List<String></code> containing the new header[0] & body[1] IDs
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 * @since			4.2
	 */
	protected static List<String> addRowColumnInTable(String docID, String reportID, String elementID, 
			String strip, String position) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Adds a row or column to the table
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID + "/elements/" + elementID +
				"?strip=" + strip +
				"&position=" + position,
				"POST", null, null, APP_XML);

		// Gets the table ID
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID + "/elements/" + elementID,
				"GET", null, null, APP_XML);
		Document response = getXmlDocument(request.getResponseContent());
		String tableID = evaluateAsString(response, "//parentId/text()");

		// Gets the new column ID
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID + "/elements/" + tableID,
				"GET", null, null, APP_XML);
		response = getXmlDocument(request.getResponseContent());

		// Retrieves the header and body IDs
		String xPathHeader = "//*/child[not(@id < preceding-sibling::child/@id) and " +
				"not(@id < following-sibling::child/@id)]/@id";
		String headerID = evaluateAsString(response, xPathHeader);
		String xPathBody = "//*[last()]/child[not(@id < preceding-sibling::child/@id) and " +
				"not(@id < following-sibling::child/@id)]/@id";
		String bodyID = evaluateAsString(response, xPathBody);

		List<String> list = new ArrayList<String>();
		list.add(headerID);
		list.add(bodyID);

		// Returns column IDs
		return list;
	}
	
	/**
	 * Adjusts the width column according the content. 
	 * 
	 * @param docID		The ID of the document
	 * @param reportID	The ID of the report 
	 * @param elementID	The ID of the column you want to adjust the width
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 * @see				Map
	 */
	protected static void autoWidthColumn(String docID, String reportID, String elementID)
			throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Gets the element details
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID + "/elements/" + elementID,
				"GET", null, null, APP_XML);
		String elementXML = request.getResponseContent();

		// Applies an automatic width to your element
		Map<String, String> map = new HashMap<String, String>();
		map.put("/element/size/@autofitWidth", "true");
		String newXmlElement = fillXml(elementXML, map);

		// Updates the report element content
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID + "/elements/" + elementID,
				"PUT", newXmlElement, APP_XML, APP_XML);
	}

	/**
	 * Updates the element expression <i>(formula)</i>.
	 * <p>
	 * This method is mainly used after a 
	 * com.sap.webi.raylight.utils.BaseSample#addRowColumnInTable(String, String, String, String, String)
	 * to fill the header and body expressions (column or row).
	 * </p>
	 * 
	 * @param docID		The ID of the document
	 * @param reportID	The ID of the report where your element is located
	 * @param elementID	The ID of the element in which you want to update a formula
	 * @param expression The new expression (formula) as a {@link String}
	 * @param dataObjectID If you expression use a data object, you need to
	 * 					specify the corresponding ID (optional, null is accepted) 
	 * @param expressionType The type of your new expression
	 * @param dataType	The data type of your new expression
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 * @since			4.1 SP2
	 */
	protected static void updateExpression(String docID, String reportID, String elementID, 
			String expression, String dataObjectID, String expressionType, String dataType) 
			throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);
		String expressionTag="";

		// Builds the expression
		if (dataObjectID==null) {
			expressionTag = "<element type=\"Cell\">" + 
					"<id>" + elementID + "</id>" + 
					"<content>" +
					"<expression>" +
					"<formula type=\"" + expressionType + "\" " +
					"dataType=\"" + dataType + "\" >" +
					expression +
					"</formula>" + 
					"</expression>" +
					"</content></element>";
		} else {
			expressionTag = "<element type=\"Cell\">" + 
					"<id>" + elementID + "</id>" + 
					"<content>" +
					"<expression>" +
					"<formula type=\"" + expressionType + "\" " +
					"dataType=\"" + dataType + "\" " +
					"dataObjectId=\"" + dataObjectID + "\">" +
					expression +
					"</formula>" + 
					"</expression>" +
					"</content></element>";			
		}

		// Adds/Updates the element expression
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID + "/elements/" + elementID,
				"PUT", expressionTag, APP_XML, APP_XML);
	}
	
	/**
	 * Adds a variable <i>(custom formula)</i> to a specific document.
	 * 
	 * @param docID		The ID of the document in which you want to add a new variable
	 * @param name		The name of this variable
	 * @param formulaLanguage The name of this new formula
	 * @param definition The formula definition (expression)
	 * @return			A <code>String</code> containing the ID of the new variable
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 */
	protected static String addVariableNumeric(String docID, String name, String formulaLanguage, 
			String definition) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);
		
		String newVariable = "<variable qualification='Measure' dataType='Numeric'>" +
							 "<name>" + name + "</name>" +
							 "<formulaLanguageId>" + formulaLanguage + "</formulaLanguageId>" +
							 "<definition>" + definition + "</definition>" +
							 "</variable>";

		// Adds the element to the report
		request.send(RL_RWS + "/documents/" + docID + "/variables",
				"POST", newVariable, APP_XML, APP_XML);

		// Retrieves the response 
		Document response = getXmlDocument(request.getResponseContent());

		// Returns the new variable ID
		return evaluateAsString(response, "//id/text()");
	}

	/**
	 * Adds an alerter to the document dictionary.
	 * 
	 * @param docID		The ID of the document where you want to add an alerter
	 * @param alerter	The content of your alerter (XML formatted file as String) 
	 * @return 			A <code>String</code> containing the ID of your new alerter
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 */
	protected static String addAlerterToDocument(String docID, String alerter)
			throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Adds an alerter to the dictionary 
		request.send(RL_RWS + "/documents/" + docID + "/alerters", "POST", alerter, TXT_XML, APP_XML);
		Document alerterResponse = getXmlDocument(request.getResponseContent());
		String alerterID = evaluateAsString(alerterResponse, "//id/text()");

		// Returns the new alerter ID
		return alerterID;
	}

	/**
	 * Attaches an existing alerter to an element.
	 * <p>
	 * The previous step is to add an alerter to the document dictionary.
	 * </p>
	 * 
	 * @param docID		The ID of the document
	 * @param reportID	The ID of the report
	 * @param elementID	The ID of the element in which you want to attach an alerter
	 * @param alerterID	The ID of the alerter which must be attached to the element ID
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 */
	protected static void addAlerterToElement(String docID, String reportID, 
			String elementID, String alerterID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Gets the targeted element details
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID + "/elements/" + elementID,
				"GET", null, null, APP_XML);
		Document elementXML = getXmlDocument(request.getResponseContent());

		// Checks if tag 'Alerter' already exists
		String newXmlElement = null;
		if (existNode(elementXML, "alerters")) {
			// Creates the new XML Fragment <id> with his value
			Text t = elementXML.createTextNode(alerterID);
			Node nodeID = elementXML.createElement("id");
			nodeID.appendChild(t);

			// Adds the New <id> Node
			Node contentNode = elementXML.getElementsByTagName("alerters").item(0);
			contentNode.appendChild(nodeID);
			newXmlElement = transformXmlDocumentToString(elementXML);		
		}
		else {			
			// Creates the new XML Fragment <id> with his value
			Text t = elementXML.createTextNode(alerterID);
			Node nodeID = elementXML.createElement("id");
			nodeID.appendChild(t);

			// Creates the new XML Fragment <alert> and appends <id>
			Node nodeAlerter = elementXML.createElement("alerters");
			nodeAlerter.appendChild(nodeID);

			// Adds the New <alerter><id> Nodes
			Node contentNode = elementXML.getElementsByTagName("content").item(0);
			contentNode.appendChild(nodeAlerter);
			newXmlElement = transformXmlDocumentToString(elementXML);			
		}

		// Adds Alerter Tag
		request.send(RL_RWS + "/documents/" + docID + "/reports/" + reportID + "/elements/" + elementID,
				"PUT", newXmlElement, APP_XML, APP_XML);
	}

	/**
	 * Refreshes a document <b>WITHOUT</b> any changes.
	 * 
	 * @param docID		The ID of the document you want to refresh
	 * @throws			Exception
	 * @since			4.0 SP5
	 */
	protected static void refreshDocWithoutChanges(String docID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);
		// Refreshes without changes
		request.send(RL_RWS + "/documents/" + docID + "/parameters", "PUT", null, null, APP_XML);
	}
	
	/**
	 * Refreshes a document <b>WITH</b> changes.
	 * 
	 * @param docID		The ID of the document you want to refresh
	 * @param xmlNewParams The content of the new refresh <i>(context and/or prompt)</i> parameters 
	 * 					<i>(XML formatted file as String)</i> 
	 * @throws			Exception
	 * @since			4.0 SP5
	 */
	protected static void refreshDocWithChanges(String docID, String xmlNewParams) 
			throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Refreshes the document with parameters
		request.send(RL_RWS + "/documents/" + docID + "/parameters", "PUT", xmlNewParams, APP_XML, APP_XML);
	}

	/**
	 * Cancels a refresh.
	 * <p>
	 * This request will try to cancel the current execution of the query.<br>
	 * If no execution is currently running, this will have no effect.
	 * </p>
	 * 
	 * @param docID		The ID of the document
	 * @param mode		Possible values are 'partial', 'restore' and 'purge'
	 * @throws			Exception
	 */
	protected static void cancelRefresh(String docID, String mode) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Cancels the current execution of refresh
		request.send(RL_RWS + "/documents/" + docID + "/parameters/execution?cancel=" + mode,
				"PUT", null, null, APP_XML);
	}
	
	/**
	 * Adds a new schedule defining a monthly instance by email.
	 * <p>
	 * You must set up your CMS Server Configuration before using this feature.
	 * </p> 
	 * 
	 * @param docID		The ID of the document you want to edit
	 * @param schedule	XML structure of mail scheduling
	 * @param startDate	The date on which you want to start running this schedule
	 * @param endDate	The date on which you want to stop running this schedule
	 * @param intervalMonth The monthly recurrence of schedule
	 * @param type		Output file type <i>(pdf, webi, xls, ...)</i>
	 * @param emailValues Email values <i>(from, to, cc, subject, message, server group and more)</i>
	 * @param parameters XML structure that contains parameters to refresh the document
	 * 					<i>(context & prompt values grouped in one structure)</i>
	 * @throws			Exception
	 * @return 			A <code>String</code> containing the ID of the new schedule
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @since			4.0 SP5
	 */
	protected static String scheduleMonthlyEmail(String docID, String schedule, String startDate, 
			String endDate, String intervalMonth, String type, Map<String, String> emailValues, 
			String parameters) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Merges schedule XML and parameters
		Document schedu = getXmlDocument(schedule);
		Document params = getXmlDocument(parameters);
		mergeXMLDocument(schedu,params,"/schedule", "/parameters");

		// Puts basic schedule values 
		emailValues.put("//startdate", startDate);
		emailValues.put("//enddate", endDate);
		emailValues.put("/schedule/format/@type", type);
		emailValues.put("/schedule/monthly/month", intervalMonth);

		String xmlSchedule = fillXml(transformXmlDocumentToString(schedu), emailValues);

		// Adds the schedule to the document
		request.send(RL_RWS + "/documents/" + docID + "/schedules", "POST", xmlSchedule, APP_XML, APP_XML);
		Document response = getXmlDocument(request.getResponseContent());

		// New schedule ID
		return evaluateAsString(response, "//id/text()");
	}

	/**
	 * Adds a new schedule defining a weekly instance by FTP.
	 * <p>
	 * You must set up your CMS Server Configuration before using this feature.
	 * </p> 
	 * 
	 * @param docID		The ID of the document you want to edit
	 * @param schedule	XML structure of mail scheduling
	 * @param startDate	The date on which you want to start running this schedule
	 * @param endDate	The date on which you want to stop running this schedule
	 * @param type		Output file type <i>(pdf, webi, xls, ...)</i>
	 * @param ftpValues FTP values <i>(name, host, port, username, account, directory
	 * 					and serverGroup)</i>
	 * @param parameters XML structure that contains parameters to refresh the document
	 * 					<i>(context & prompt values grouped in one structure)</i>
	 * @throws			Exception
	 * @return 			A <code>String</code> containing the ID of the new schedule
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @since			4.0 SP5
	 */
	protected static String scheduleWeeklyFtp(String docID, String schedule, String startDate,
			String endDate, String type, Map<String, String> ftpValues, 
			String parameters) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Merges schedule XML and parameters
		Document schedu = getXmlDocument(schedule);
		Document params = getXmlDocument(parameters);
		mergeXMLDocument(schedu,params,"/schedule", "/parameters");

		// Puts basic schedule values 
		ftpValues.put("//startdate", startDate);
		ftpValues.put("//enddate", endDate);
		ftpValues.put("/schedule/format/@type", type);

		String xmlSchedule = fillXml(transformXmlDocumentToString(schedu), ftpValues);

		// Adds the schedule to the document
		request.send(RL_RWS + "/documents/" + docID + "/schedules", "POST", xmlSchedule, APP_XML, APP_XML);
		Document response = getXmlDocument(request.getResponseContent());

		// New Schedule ID
		return evaluateAsString(response, "//id/text()");
	}
	
	/**
	 * Adds a new schedule defining a daily instance saved as File System.
	 * <p>
	 * You must set up your CMS Server Configuration before using this feature.
	 * </p> 
	 * 
	 * @param docID		The ID of the document you want to edit
	 * @param schedule	XML structure of mail scheduling
	 * @param startDate	The date on which you want to start running this schedule
	 * @param endDate	The date on which you want to stop running this schedule
	 * @param type		Output file type <i>(pdf, webi, xls, ...)</i>
	 * @param fileSystemValues File system values <i>(name, username, directory, dayinterval
	 * 					and serverGroup)</i>
	 * @param parameters XML structure that contains parameters to refresh the document
	 * 					<i>(context & prompt values grouped in one structure)</i>
	 * @throws			Exception
	 * @return 			A <code>String</code> containing the ID of the new schedule
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @since			4.0 SP5
	 */
	protected static String scheduleDailyFileSystem(String docID, String schedule, String startDate,
			String endDate, String type, Map<String, String> fileSystemValues, 
			String parameters) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Merges schedule XML and parameters
		Document schedu = getXmlDocument(schedule);
		Document params = getXmlDocument(parameters);
		mergeXMLDocument(schedu,params,"/schedule", "/parameters");

		// Puts basic schedule values 
		fileSystemValues.put("//startdate", startDate);
		fileSystemValues.put("//enddate", endDate);
		fileSystemValues.put("/schedule/format/@type", type);

		String xmlSchedule = fillXml(transformXmlDocumentToString(schedu), fileSystemValues);

		// Adds the schedule to the document
		request.send(RL_RWS + "/documents/" + docID + "/schedules", "POST", xmlSchedule, APP_XML, APP_XML);
		Document response = getXmlDocument(request.getResponseContent());

		// New Schedule ID
		return evaluateAsString(response, "//id/text()");
	}

	/**
	 * Adds a new schedule defining an hourly instance as Inbox.
	 * <p>
	 * This method doesn't require any specific CMS Server Configuration.
	 * </p> 
	 * 
	 * @param docID		The ID of the document you want to edit
	 * @param schedule	XML structure of mail scheduling
	 * @param startDate	The date on which you want to start running this schedule
	 * @param endDate	The date on which you want to stop running this schedule
	 * @param type		Output file type (pdf, webi, xls, ...)
	 * @param hour		Interval hour between each instance
	 * @param minute	Interval minute between each instance
	 * @param parameters XML structure that contains parameters to refresh the document
	 * 					(context & prompt values grouped in one structure)
	 * @return 			A <code>String</code> containing the ID of the new schedule
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#evaluateAsString(Node, String)
	 * @see 			app.util.RestFulUtil#getXmlDocument(String)
	 * @since			4.0 SP5
	 */
	protected static String scheduleHourlyToInbox(String docID, String schedule, String startDate, 
			String endDate, String type, String hour, String minute, String parameters) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Merges schedule XML and parameters
		Document schedu = getXmlDocument(schedule);
		Document params = getXmlDocument(parameters);
		mergeXMLDocument(schedu,params,"/schedule", "/parameters");

		// Puts the new schedule values
		Map<String, String> map = new HashMap<String, String>();
		map.put("/schedule/hourly/startdate", startDate);
		map.put("/schedule/hourly/enddate", endDate);
		map.put("/schedule/hourly/hour", hour);
		map.put("/schedule/hourly/minute", minute);
		map.put("/schedule/format/@type", type);

		String xmlSchedule = fillXml(transformXmlDocumentToString(schedu), map);

		// Adds the schedule to the document
		request.send(RL_RWS + "/documents/" + docID + "/schedules", "POST", xmlSchedule, APP_XML, APP_XML);
		Document response = getXmlDocument(request.getResponseContent());

		// New Schedule's ID
		return evaluateAsString(response, "//id/text()");
	}

	/**
	 * Gets all schedules attached to a specific document.
	 * 
	 * @param docID		The ID of the document to look for
	 * @return 			A <code>String</code> containing the ID of the new schedule
	 * @throws			Exception
	 * @see				NodeList
	 * @see				List
	 * @see				XPathExpression
	 * @since			4.0 SP5
	 */
	protected static List<String> getAllSchedules(String docID) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		List<String> schedulesIDs = new ArrayList<String>();

		// Gets all schedules for a specific document
		request.send(RL_RWS + "/documents/" + docID + "/schedules", "GET", null, null, APP_XML);
		Document response = getXmlDocument(request.getResponseContent());

		// Gets all IDs
		XPathExpression expr = xPath.compile("/schedules/schedule/id/text()");
		NodeList nodeList = (NodeList) expr.evaluate(response, XPathConstants.NODESET);

		// Adds all IDs in a list of Strings
		for (int i = 0; i < nodeList.getLength(); i++) {
			schedulesIDs.add(nodeList.item(i).getNodeValue());
		}
		return schedulesIDs;
	}
	
	/**
	 * Gets the suggested mapping to change data source.
	 * <p>
	 * This method generates a suggested mapping.
	 * Obviously, you can edit this suggested mapping with your own values.
	 * </p>
	 * 
	 * @param docID		The ID of the document for change source
	 * @param newDataSourceID The ID of your new data source, supports all types
	 * 					of data sources <i>(xls, universe, ...)</i>
	 * @return 			A <code>String</code> containing the suggested XML mapping
	 * @throws			Exception
	 */
	protected static String changesourceGetSuggMapping(String docID, String newDataSourceID) 
			throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		// Gets the data provider IDs
		List<String> DPs = getDataProvidersIDs(docID);

		// Concatenates data providers to URL
		String basicURLwthDPs = "/documents/" + docID + 
				"/dataproviders/mappings?originDataproviderIds=";
		for (int i = 0; i < DPs.size(); i++) {
			if (i == (DPs.size()-1)) {
				// Only if it is the last element
				basicURLwthDPs += DPs.get(i).toString();
			} else {
				basicURLwthDPs += DPs.get(i).toString() + ",";
			}
		}

		request.send(RL_RWS + basicURLwthDPs + "&targetDatasourceId=" + newDataSourceID, 
				"GET", null, null, APP_XML);
		return request.getResponseContent();
	}

	/**
	 * Applies a change data source.
	 * <p>
	 * This method is the final method to change a data source.<br>
	 * You need to specify the ID of the target data source and the correct mapping.
	 * <i>(you can suggest a mapping from 
	 * com.sap.webi.raylight.utils.BaseSample#changesourceGetSuggMapping(String, String))</i>
	 * </p>
	 * 
	 * @param docID		The ID of the document for change source
	 * @param newDataSourceID The ID of your new data source, supports all types
	 * 					of data sources (xls, universe, ...)
	 * @param mapping	The mapping between your old and new data sources
	 * @throws			Exception
	 * @see 			app.util.RestFulUtil#changesourceGetSuggMapping(String, String)
	 */
	protected static void changesourceApply(String docID, String newDataSourceID,
			String mapping) throws Exception {
		RestFulRequest request = new RestFulRequest(logonToken);

		request.send(RL_RWS + "/documents/" + docID + "/dataproviders/mappings?targetDatasourceId=" +
				newDataSourceID, "POST", mapping, APP_XML, APP_XML);
		
		// If not 200: Change Source Error.
		Assert.assertEquals(HTTP_OK, request.getResponseCode());
	}
	
	/**
	 * Gets the resource XML file as a String.
	 * <p>
	 * This method is used in all samples to retrieve the content of templates <i>(XML files)</i>
	 * </p>
	 * 
	 * @param resourcePath The link of the XML file you want to look for
	 * @return 			A <code>String</code> containing the content of the XML file given as parameter
	 * @throws			Exception
	 * @throws			IOException 
	 * @see 			InputStream
	 */
	protected static String getResourceAsXml(String resourcePath) throws Exception {
		InputStream inputStream = createInputStream(resourcePath);
		try {
			return getXmlFromInputStream(inputStream);
		} 
		finally {
			try {
				inputStream.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Merges two XML documents.
	 * <p>You only need to specify a path.</p>
	 * 
	 * @param root		The main XML document in which you want to merge another document
	 * @param insertDoc	The other XML document
	 * @param toPath	The destination node (your content will be added before)
	 * @param fromPath	The first node from which it must be start
	 */
	protected static void mergeXMLDocument(Document root, Document insertDoc, String toPath, 
			String fromPath) {
		try {
			Node element = getNode(insertDoc, fromPath);
			Node dest = root.importNode(element, true);
			Node node = getNode(root, toPath);
			node.insertBefore(dest, null);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	/**
	 * Evaluates an XPath expression on the specified XML code.
	 * 
	 * @param node		Source Document XMLDocument document
	 * @param xpath		XPath expression to evaluate
	 * @return 			A <code>String</code> containing the result of the evaluation
	 * @throws 			Exception
	 */
	protected static String evaluateAsString(Node node, String xpath) throws XPathExpressionException {
		XPathExpression expr = getXPath().compile(xpath);
		Object value = expr.evaluate(node, XPathConstants.STRING);

		return (String) value;
	}

	/**
	 * Evaluates an XPath expression on the specified XML code.
	 * 
	 * @param node		Source Document XMLDocument document
	 * @param xpath		XPath expression to evaluate
	 * @return 			A <code>Node</code> containing the result of the evaluation
	 * @throws 			Exception
	 */
	protected static Node evaluateAsNode(Node node, String xpath) throws XPathExpressionException {
		XPathExpression expr = getXPath().compile(xpath);
		Object value = expr.evaluate(node, XPathConstants.NODE);

		return (Node) value;
	}

	/**
	 * Evaluates an XPath expression on the specified XML code.
	 * 
	 * @param node		Source Document XMLDocument document
	 * @param xpath		XPath expression to evaluate
	 * @return 			A <code>NodeList</code> containing the result of the evaluation
	 * @throws			Exception
	 */
	protected static NodeList evaluateAsNodeList(Node node, String xpath) throws XPathExpressionException {
		XPathExpression expr = getXPath().compile(xpath);
		Object value = expr.evaluate(node, XPathConstants.NODESET);

		return (NodeList) value;
	}
	
	/** 
	 * Gets DateTime in a format used in Web Intelligence <i>(particularly for schedules)</i>.
	 * 
	 * @param calendar	The calendar (Local)
	 * @param days 		The number of days to <b>add</b> or <b>decrement</b> compared with today
	 * @return 			A <code>String</code> containing the DateTime formatted to ISO 8601
	 * @see				Date
	 * @see				Calendar
	 * @see				SimpleDateFormat
	 **/
	protected static String nowDateTimeFormatISO8601(final Calendar calendar, 
			int days) {
		// Gets
		Date date;
		calendar.add(Calendar.DATE, days);
		date = calendar.getTime();
		// Formats
		String formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(date);
		// Customs & Returns
		return formatted.substring(0, 26) + ":" + formatted.substring(26);
	}
	
	protected static String Now() throws Exception {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		return (dateFormat.format(Calendar.getInstance().getTime()));
	}

	//
	// PRIVATE
	//
	
	/** 
	 * Gets a specific node of an XML document.
	 * 
	 * @param doc		The XML document to look for
	 * @param strXpathExpression The XPath of the node you want to retrieve
	 * @return 			The <code>Node</code> found in the document 
	 * @throws			Exception
	 * @see				XPath
	 * @see				XPathExpression
	 **/
	private static Node getNode(Document doc, String strXpathExpression)
			throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();

		// Shows all nodes values from XPath Query
		XPathExpression expr = xpath.compile(strXpathExpression);

		return (Node) expr.evaluate(doc, XPathConstants.NODE);
	}
	
	/** 
	 * Gets a specific node of an XML document.
	 * 
	 * @param doc		The XML document to look for
	 * @param nodeToCheck The node of this search 
	 * @return 			A <code>boolean</code> 'true' or 'false' if it has been found or not
	 * @throws			Exception
	 * @see				XPath
	 * @see				XPathExpression
	 **/
	private static boolean existNode(Document doc, String nodeToCheck) throws Exception {
		NodeList nl = doc.getElementsByTagName(nodeToCheck);
		boolean response;
		if (nl.getLength() > 0) { response = true; } else { response = false; }
		return response;
	}

	/** 
	 * Fills an XML file with your own values.
	 * 
	 * @param inputXML	The original document before changes
	 * @param values 	The Map of your own values<br>
	 * 					<i>(key: xpath expression, value: your value)</i>
	 * @return 			A <code>String</code> formatted as XML and filled with your own values 
	 * @throws			Exception
	 * @see				XPath
	 * @see				XPathExpression
	 **/
	private static String fillXml(String inputXML, Map<String, String> values) throws Exception {
		// Reads XML code
		Document document = getXmlDocument(inputXML);

		// Does injections
		if (values != null) {
			for (String value : values.keySet()) {
				Node node = evaluateAsNode(document, value);
				node.setTextContent(values.get(value));
			}
		}
		// Writes XML code
		return transformXmlDocumentToString(document);
	}

	/** 
	 * Creates an InputStream from a file.
	 * 
	 * @param resourcePath The file to read
	 * @return 			An <code>InputStream</code>
	 * @throws			IOException
	 * @see				InputStream
	 **/
	private static InputStream createInputStream(String resourcePath) throws IOException {
		InputStream inputStream = RestFulUtil.class.getClassLoader().getResourceAsStream(resourcePath);
		if (inputStream == null)
			throw new FileNotFoundException(resourcePath);
		return inputStream;
	}

	/** 
	 * Gets XML formatted String from InputStream.
	 * 
	 * @param inputStream The Input Stream to get as XML
	 * @return 			A <code>String</code> containing the XML content
	 * @see				InputStream
	 **/
	private static String getXmlFromInputStream(InputStream inputStream) {
		Scanner scanner = new Scanner(inputStream);
		scanner.useDelimiter("\\A");
		String xmlResource = scanner.hasNext() ? scanner.next() : "";
		scanner.close();
		return xmlResource;
	}

	/** 
	 * Transforms XML document to String.
	 * 
	 * @param document	The XML document to look for
	 * @return 			A <code>String</code> containing the XML document given as parameter 
	 * @throws			Exception
	 * @see				TransformerFactory
	 * @see				Transformer
	 * @see				StringWriter
	 **/
	private static String transformXmlDocumentToString(Document document) 
			throws Exception {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(writer));
		return writer.getBuffer().toString();
	}

	/** 
	 * Gets the XPath from path expression.<br>
	 * 
	 * <p>
	 * Used in:<br>
	 * <ul>
	 * 	<li>com.sap.webi.raylight.utils.BaseSample#evaluateAsNode(Node, String)</li>
	 * 	<li>com.sap.webi.raylight.utils.BaseSample#evaluateAsNodeList(Node, String)</li>
	 * 	<li>com.sap.webi.raylight.utils.BaseSample#evaluateAsString(Node, String)</li>
	 * </ul>
	 * </p>
	 * 
	 * @return 			The <code>XPath</code> found, or a new one if not be found
	 * @see				TransformerFactory
	 * @see				StringWriter
	 **/
	private static XPath getXPath() {
		if (xPath == null) {
			xPath = XPathFactory.newInstance().newXPath();
		}
		return xPath;
	}

	/** 
	 * Gets an XML formatted document from a String.
	 * 
	 * @param inputXML	String that contains the content you want as a document
	 * @return 			A <code>Document</code> containing the formatted XML given as parameter
	 * @throws			Exception
	 * @see				DocumentBuilderFactory
	 * @see				DocumentBuilder
	 **/
	protected static Document getXmlDocument(String inputXML) throws Exception {


        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document xmlDocument = builder.parse(new InputSource(new StringReader(inputXML)));

		// Test UTF-8 issue. 
//		Reader reader = new StringReader(inputXML);
//      InputSource is = new InputSource(reader);
//      is.setEncoding("UTF-8");
//		Document xmlDocument = builder.parse(is);
		
		return xmlDocument;
	}
}