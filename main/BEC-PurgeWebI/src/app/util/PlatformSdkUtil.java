package app.util;

import java.util.ArrayList;
import java.util.List;

//import org.apache.log4j.Logger;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;

import app.config.Config;

/**
 */
//public class PlatformSdkUtil {
public class PlatformSdkUtil extends Config {
//	private static Logger logger = Logger.getLogger(PlatformSdkUtil.class);

	public PlatformSdkUtil() {}

	public static IEnterpriseSession ConfigLogon() throws Exception {

		IEnterpriseSession boSession = SdkLogon(CMS_USER, CMS_PASS, CMS_HOST + ":" + CMS_PORT, CMS_AUTH);
		return boSession;

		//return SdkLogon(CMS_USER, CMS_PASS, CMS_HOST + ":" + CMS_PORT, CMS_AUTH);
	
	}

	public static IEnterpriseSession PropertyLogon() throws Exception {

		PropertyFileUtil logonInfo  = new PropertyFileUtil();
		
		return SdkLogon(logonInfo.getCMS_USER(), logonInfo.getCMS_PASS()
				, logonInfo.getCMS_HOST() +":"+ logonInfo.getCMS_PORT()
//				, logonInfo.getCMS_CLUSTER()
				, logonInfo.getCMS_AUTH()
				);
	}
	
	public static IEnterpriseSession SdkLogon(String userName, String password, String CMS, String authen) throws Exception, SDKException {
		String errMsg = null;
		
		try {
			IEnterpriseSession boSession = CrystalEnterprise.getSessionMgr().logon(userName, password, CMS, authen);
			if (boSession == null)
				System.out.println("SdkLogon failed: CMS="+ CMS +" User="+ userName);
			else
				System.out.println("SdkLogon success: CMS="+ CMS +" User="+ userName);
			return boSession;
		} catch (SDKException ex) {
			errMsg = ex.toString();
			ex.printStackTrace();			// This goes to System.err
	        System.exit(1);
		}
		throw new Exception(errMsg != null ? errMsg : "Connot logon to SAP BI server" + CMS +" with user "+ userName);
	}
	
	public static void SdkLogoff(IEnterpriseSession boSession) throws SDKException {
		boSession.logoff();
		System.out.println("SdkLogoff");
	}

	public static List<String> GetLargestWebiList(int topBatchSize, int fileSize) throws Exception {
		IInfoObject boInfoObject = null;
		
		// Logon BI server with parameters in app.properties (config.java)
		IEnterpriseSession boSession = ConfigLogon();
		IInfoStore boInfoStore = (IInfoStore) boSession.getService("InfoStore");

		// Get most largest WebI of this batch, order by SI_ID
		String query = "SELECT top "+ topBatchSize +" SI_ID FROM CI_INFOOBJECTS "+
				"WHERE SI_KIND = 'WEBI' and SI_INSTANCE = 0"+
				"AND SI_SIZE > " + fileSize +
				"ORDER BY SI_SIZE DESC, SI_ID";
		
		// Run query
		IInfoObjects boInfoObjects = boInfoStore.query(query);

		List<String> WEBIs = new ArrayList<String>();
		System.out.println("Total WebI size over " + fileSize + " bytes is " + boInfoObjects.size());
		// Adds all IDs at List<String>
		for (int i = 0; i < boInfoObjects.size(); i++) {
			boInfoObject = (IInfoObject) boInfoObjects.get(i);
			WEBIs.add(String.valueOf(boInfoObject.getID()));
		}
		SdkLogoff(boSession);
		return WEBIs;
	}

}
