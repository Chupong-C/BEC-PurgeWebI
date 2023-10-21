package app.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
//import java.util.concurrent.TimeUnit;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import app.service.WebiSize;
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

	public void PurgeLargest(int MinuteLimit) throws Exception {
		LocalDateTime endTime = LocalDateTime.now(), startTime = LocalDateTime.now();
		String lastID = "";
		Boolean timeOut = false;
		
		// Get list of WebI DocIDs of this batch
		List<String> WEBIs = WebiSize.GetLargestList(TOP_BATCH_SIZE, SIZE_BIGGER_THAN);
		
		// Not found WebI bigger than BIGGER_THAN
		if (WEBIs.isEmpty()) return;

		do {
			// Keep ID of this batch
			lastID = WEBIs.get(0);

			// Purge all WebI in this list (batch)
			restLogon();
			for (int j = 0 ; j < WEBIs.size() && !timeOut; j++) {
				PurgeWebi(WEBIs.get(j));
				endTime = LocalDateTime.now();
				timeOut = !(MinuteLimit == 0 || MinuteLimit > Duration.between(startTime, endTime).toMinutes());
			}
			restLogoff();
			
			// If number of documents is less than TOP_BATCH_SIZE, no more loop 
			if (WEBIs.size() < TOP_BATCH_SIZE) break;
			
			// Get list of WebI DocIDs of next batch
			WEBIs = WebiSize.GetLargestList(TOP_BATCH_SIZE, SIZE_BIGGER_THAN);

		// If this-batch ID vs. next-batch ID is same then break loop
		} while (!lastID.equals(WEBIs.get(0)) && !timeOut);
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
