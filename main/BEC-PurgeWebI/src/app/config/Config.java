package app.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
//import java.io.InputStream;
import java.util.Properties;

import javax.xml.xpath.XPath;

/**
 * <b>Collected parameters</b> of sample project for good execution.</br>
 * </br>
 * <p>All members of this class are <u>immutable</u>.</br>
 * </br>
 * <u>About</u>:</br>
 * By default, the CMS_HOST_PORT is configured with the port <b<6405</b>.</br>
 * Before launching any sample, you must fill the following variables with your own values:</br>
 * <ul>
 * 		<li>CMS_SERVER_URL</li>
 * 		<li>CMS_USER</li>
 * 		<li>CMS_PASS</li>
 * 		<li>CMS_AUTH</li>
 * </ul>
 * 	CMS_SERVER_URL:</br>
 * </br><i>scheme://domain:port</i> (e.g. <i>http://myserver:6405</i>).
 * </p>
 */
//@SuppressWarnings("nls")
public class Config {

	//
	// !!! Adjust value in app.property file on app's folder location !!!
	//

	//
	// Value getting from the app.property file
	//
	protected static String CMS_CLUSTER;
	protected static String CMS_HOST;
	protected static String CMS_PORT;
	protected static String CMS_USER;
	protected static String CMS_PASS;
	protected static String CMS_AUTH;				//secEnterprise, secLDAP
	protected static Integer REST_HTTP_PORT;
	protected static Integer REST_BATCH_SIZE;		// Limit when call API document listing */
	protected static Integer TOP_BATCH_SIZE;		// Limit when call SDK query */
	protected static Integer TIMEOUT_MINUTE;		// Runtime duration limit (minute)
	protected static Integer SIZE_BIGGER_THAN;			// Purge only if file is bigger than (byte)


	// RESTFUL WEB SERVICE URLs
	protected static String CMS_SERVER_URL;
	protected static String BIP_RWS;
	protected static String RL_RWS;

	// SAMPLE (global)

	/** Header Element ID */
	protected static final String HEADER = "1";

	/** Body Element ID */
	protected static final String BODY = "2";

	/** Footer Element ID */
	protected static final String FOOTER = "3";

	/**	The ID of the report you want to add the new chart */
	protected static final String ID_INITIAL_REPORT = "1";
	protected static final String ID_DEFAULT_REPORT = "2";

	// SAMPLE (1/6) : Create new Document & Save it (CreateAndSaveSample.java)
	/** The CUID of the destination folder used to save your new document */
	protected static final String CUID_DESTINATION_FOLDER = "AeN4lEu0h_tAtnPEjFYxwi8";

	/** The CUID of the sample Universe */
	protected static final String CUID_WAREHOUSE_UNIVERSE = "AaouRkFcx9lDiNfR0Z4JAqc";

	// SAMPLE (2 & 3/6) : Add chart & table to Doc (AddChartSample.java / AddTableSample.java)
	/**	The CUID of the document used to add the new chart & table */
	protected static final String CUID_EMPTY_DOC_TEMPLATE = "AX7aeEkZLjxFvd7NcYtphLY";

	// SAMPLE (4/6) : Refresh Document (RefreshSample.java)
	/**	The CUID of the document used to show how to refresh a document/universe */
	protected static final String CUID_REFRESH_DOC_TEMPLATE = "AV_Q8mnwRc5FpkJyyVjJaX4";

	// SAMPLE (5/6) : Schedule Document (ScheduleSample.java)
	/**	The CUID of the document used to show how to schedule a document */
	protected static final String CUID_SCHEDULE_DOC_TEMPLATE = "AThi89W.9tVBleIPE.ytxGo";

	// SAMPLE (6/6) : Change Source Document (ChangeSourceSample.java)
	/**	The CUID of the document used to show how to change the source of a document */
	protected static final String CUID_CHANGESOURCE_DOC_TEMPLATE = "AU9j.mtslcBFtwlANIFWrpI";
	
	/**	The CUID of the eFashion.UNX universe used to replace eFashion.UNV */
	protected static final String CUID_UNX_NEWEFASHION_UNIVERSE = "AQm3f6oR3.ZBqsvLN0nKUUo";
	
	// SAMPLE (7/7) : Use Free-Hand SQL feature (FreeHandSQLSample.java)
	/** The CUID of the sample Connection */
	protected static final String CUID_WAREHOUSE_CONNECTION = "1EzlTPcACOBFWAcAUwC1ANkjHokN3A";



	//
	// Utilities. (don't modify!)
	//

	public static final String APP_XML = "application/xml";
	public static final String TXT_XML = "text/xml";
	protected static String logonToken;
	protected static XPath xPath;
	
	public Config() {

		String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
		String appConfigPath = rootPath + "app.properties";

		Properties appProps = new Properties();
		try {
			appProps.load(new FileInputStream(appConfigPath));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Config.CMS_CLUSTER = appProps.getProperty("CMS_CLUSTER");
		Config.CMS_HOST = appProps.getProperty("CMS_HOST");
		Config.CMS_PORT = appProps.getProperty("CMS_PORT");
		Config.CMS_USER = appProps.getProperty("CMS_USER");
		Config.CMS_PASS = appProps.getProperty("CMS_PASS");
		Config.CMS_AUTH = appProps.getProperty("CMS_AUTH");
		Config.REST_HTTP_PORT = Integer.parseInt(appProps.getProperty("REST_HTTP_PORT"));

		Config.REST_BATCH_SIZE = Integer.parseInt(appProps.getProperty("REST_BATCH_SIZE"));
		Config.TOP_BATCH_SIZE = Integer.parseInt(appProps.getProperty("TOP_BATCH_SIZE"));
		Config.TIMEOUT_MINUTE = Integer.parseInt(appProps.getProperty("TIMEOUT_MINUTE"));
		Config.SIZE_BIGGER_THAN = Integer.parseInt(appProps.getProperty("SIZE_BIGGER_THAN"));

		/** CMS URL (don't modify!) */
		Config.CMS_SERVER_URL = "http://" + CMS_HOST + ":" + REST_HTTP_PORT;

		/** BIP URL (don't modify!) */
		Config.BIP_RWS = CMS_SERVER_URL + "/biprws";

		/** Raylight WebService URL (don't modify!) */
		Config.RL_RWS = BIP_RWS + "/raylight/v1";

	}
	
}
