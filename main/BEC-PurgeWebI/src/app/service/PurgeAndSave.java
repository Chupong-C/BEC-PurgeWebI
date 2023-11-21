package app.service;

import java.util.ArrayList;
import java.util.List;

import java.time.Duration;
import java.time.LocalDateTime;

import app.util.PlatformSdkUtil;
import app.util.RestFulUtil;

/**
 * Purge all Webi, all dataprovider.
 * Chupong 2023-10-02
 *
 */

// Java code for thread creation by implementing the Runnable Interface
// public class PurgeAndSave extends BaseUtil implements Runnable{

public class PurgeAndSave extends RestFulUtil {

	public void PurgeWebi(String DocumentID) throws Exception {

		// Get list of DataProviders from this DocumentID
		List<String> DPs = getDataProvidersIDs(DocumentID);
				
		// Purge all dataProviders by IDs 
		for (int i = 0; i < DPs.size(); i++)
			purgeDataProvider(DocumentID, DPs.get(i).toString());
		
		// Saves the current document
		saveDocument(DocumentID);
	}

	public void IsEmptyWebi(String DocumentID) throws Exception {

		isAllDataProvidersEmpty(DocumentID);

	}
	
	public void PurgeLargest(int MinuteLimit, int fileSize) throws Exception {
		LocalDateTime endTime = LocalDateTime.now(), startTime = LocalDateTime.now();
		Boolean isTimeOut = false;
		
		// If no parameter pass use default from app.properties file (Config class).
		MinuteLimit = (MinuteLimit<0 ? TIMEOUT_MINUTE : MinuteLimit);
		fileSize = (fileSize<0 ? FILE_BIGGER_THAN_BYTE : fileSize);
		
		// Get list of WebI DocIDs of this batch
		List<String> WEBIs = PlatformSdkUtil.GetLargestWebiList(TOP_BATCH_SIZE, fileSize);
		
		// Not found WebI bigger than SIZE_BIGGER_THAN
		if (WEBIs.isEmpty()) return;
		restLogon();

		// Check if the first largest WebI need to purge or not
		while (!isAllDataProvidersEmpty(WEBIs.get(0)) && !isTimeOut) {

			// Purge all WebI in this list (batch)
			for (int j = 0 ; j < WEBIs.size() && !isTimeOut; j++) {
				PurgeWebi(WEBIs.get(j));
				endTime = LocalDateTime.now();
				isTimeOut = !(MinuteLimit == 0 || MinuteLimit > Duration.between(startTime, endTime).toMinutes());
			}
			
			// If number of documents is less than TOP_BATCH_SIZE, no more loop 
			if (WEBIs.size() < TOP_BATCH_SIZE || isTimeOut) break;
			
			// Get list of WebI DocIDs of next batch
			WEBIs = PlatformSdkUtil.GetLargestWebiList(TOP_BATCH_SIZE, FILE_BIGGER_THAN_BYTE);
			
		}
		restLogoff();

	}

	public void PurgeBatch(int MinuteLimit, int batchStart) throws Exception {
		int bactchNo = batchStart;
		int offset = 0;
		List<String> WEBIs = new ArrayList<String>();
		LocalDateTime endTime, startTime = LocalDateTime.now();

		do {
			// Get list of WebI DocIDs of this batch
			offset = REST_BATCH_SIZE * bactchNo; 
			WEBIs = getDocumentsIDs(offset, REST_BATCH_SIZE);

			// Purge all WebI in this list (batch)
			for (int j = 0; j < WEBIs.size(); j++)
				PurgeWebi(WEBIs.get(j));
			
			endTime = LocalDateTime.now();
			bactchNo++;
		} while (WEBIs.size()> 0
				&& ( MinuteLimit == 0 
				|| MinuteLimit > Duration.between(startTime, endTime).toMinutes() )
				);
	}

	public void PurgeBatch(int MinuteLimit, int batchStart, int batchEnd) throws Exception {
		int bactchNo = batchStart;
		int offset = 0;
		List<String> WEBIs = new ArrayList<String>();
		LocalDateTime endTime, startTime = LocalDateTime.now();
		
		// Nothing to do
		if (batchStart > batchEnd) return;

		do {
			// Get list of WebI DocIDs of this batch
			offset = REST_BATCH_SIZE * bactchNo; 
			WEBIs = getDocumentsIDs(offset, REST_BATCH_SIZE);

			// Purge all WebI in this list (batch)
			for (int j = 0; j < WEBIs.size(); j++)
				PurgeWebi(WEBIs.get(j));
			
			endTime = LocalDateTime.now();
			bactchNo++;
		} while (WEBIs.size()> 0 
				&& bactchNo <= batchEnd 
				&& ( MinuteLimit == 0 
				|| MinuteLimit > Duration.between(startTime, endTime).toMinutes() )
				);
	}

/*
 * 	@Override
 */

/*	public void run() {
		// TODO Auto-generated method stub
		
	}
*/

}
