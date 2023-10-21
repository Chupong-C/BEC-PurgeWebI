package app.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.crystaldecisions.celib.exception.AbstractException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;

import app.config.Config;
import app.util.PlatformSdkUtil;
import app.util.RestFulUtil;

public class WebiSize extends app.util.PlatformSdkUtil {

	public static List<String> GetLargestList(int topBatchSize, int purgeSize) throws Exception {
		IInfoObject boInfoObject = null;
		
		// Logon BI server with parameters in config.java
		IEnterpriseSession boSession = PlatformSdkUtil.ConfigLogon();
		IInfoStore boInfoStore = (IInfoStore) boSession.getService("InfoStore");

		// Get most largest WebI of this batch, order by SI_ID
		String query = "SELECT top "+ topBatchSize +" SI_ID FROM CI_INFOOBJECTS "+
				"WHERE SI_KIND = 'WEBI' and SI_INSTANCE = 0"+
				"AND SI_SIZE > " + purgeSize +
				"ORDER BY SI_SIZE DESC, SI_ID";
		
		// Run query
		IInfoObjects boInfoObjects = boInfoStore.query(query);

		List<String> WEBIs = new ArrayList<String>();
		// Adds all IDs at List<String>
		for (int i = 0; i < boInfoObjects.size(); i++) {
			boInfoObject = (IInfoObject) boInfoObjects.get(i);
			WEBIs.add(String.valueOf(boInfoObject.getID()));
		}
		SdkLogoff(boSession);
		return WEBIs;
	}

}
