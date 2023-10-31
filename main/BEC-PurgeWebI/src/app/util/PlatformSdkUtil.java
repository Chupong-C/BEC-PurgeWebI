package app.util;

//import org.apache.log4j.Logger;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;

import app.config.Config;

/**
 */
//public class PlatformSdkUtil {
public class PlatformSdkUtil extends Config {
//	private static Logger logger = Logger.getLogger(PlatformSdkUtil.class);

	public PlatformSdkUtil() {}

	public static IEnterpriseSession ConfigLogon() throws Exception {

		return SdkLogon(CMS_USER, CMS_PASS, CMS_HOST + ":" + CMS_PORT, CMS_AUTH);
		
	}

	public static IEnterpriseSession iniLogon() throws Exception {

		PropertyFileUtil logonInfo  = new PropertyFileUtil();
		
		return SdkLogon(logonInfo.getCMS_USER(), logonInfo.getCMS_PASS()
				, logonInfo.getCMS_CLUSTER() +":"+ logonInfo.getCMS_PORT()
//				, logonInfo.getCMS_HOST()
				, logonInfo.getCMS_AUTH()
				);
	}
	
	public static IEnterpriseSession SdkLogon(String userName, String password, String CMS, String authen) throws Exception, SDKException {
		String errMsg = null;
		
		try {
			IEnterpriseSession boSession = CrystalEnterprise.getSessionMgr().logon(userName, password, CMS, authen);
			if (boSession == null)
				System.out.println("SdkLogon failed");
			else
				System.out.println("SdkLogon Success");
			return boSession;
		} catch (SDKException ex) {
			errMsg = ex.toString();
		}
		throw new Exception(errMsg != null ? errMsg : "Connot logon to SAP BI server" + CMS);
	}
	
	public static void SdkLogoff(IEnterpriseSession boSession) throws SDKException {
		boSession.logoff();
		System.out.println("SdkLogoff");
	}

}
