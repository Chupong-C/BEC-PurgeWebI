package app.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * 
 * Read properties from file name = app.properties
 * folder = app's folder.
 * 
 * @author chupong.c
 *
 */

public class PropertyFileUtil {
	private String CMS_CLUSTER;
	private String CMS_HOST;
	private String CMS_PORT;
	private String CMS_USER;
	private String CMS_PASS;
	private String CMS_AUTH;
	private int TIMEOUT_MINUTE;
	private int REST_PORT;
	

	public PropertyFileUtil() {

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

		this.CMS_CLUSTER = appProps.getProperty("CMS_CLUSTER");
		this.CMS_HOST = appProps.getProperty("CMS_HOST");
		this.CMS_PORT = appProps.getProperty("CMS_PORT");
		this.CMS_USER = appProps.getProperty("CMS_USER");
		this.CMS_PASS = appProps.getProperty("CMS_PASS");
		this.CMS_AUTH = appProps.getProperty("CMS_AUTH");
		this.TIMEOUT_MINUTE = Integer.parseInt(appProps.getProperty("TIMEOUT_MINUTE"));
		this.REST_PORT = Integer.parseInt(appProps.getProperty("REST_PORT"));
		
//		System.out.println(this.CMS_CLUSTER);

	}
	
	public String getCMS_CLUSTER() {
		return CMS_CLUSTER;
	}

	public String getCMS_HOST() {
		return CMS_HOST;
	}

	public String getCMS_PORT() {
		return CMS_PORT;
	}
	
	public String getCMS_USER() {
		return CMS_USER;
	}
	
	public String getCMS_PASS() {
		return CMS_PASS;
	}

	public String getCMS_AUTH() {
		return CMS_AUTH;
	}

	public Integer getTIMEOUT_MINUTE() {
		return TIMEOUT_MINUTE;
	}
	
	public Integer getREST_PORT() {
		return REST_PORT;
	}


}
