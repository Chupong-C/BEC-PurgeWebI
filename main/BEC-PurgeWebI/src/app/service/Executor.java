package app.service;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import app.util.PropertyFileUtil;
import app.util.RestFulUtil;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

public class Executor extends RestFulUtil {

    public static void main(String[] args) throws Exception {
		
    	tessst();
    	System.exit(0);
    	
		if (args.length <1) {
			PurgeAllWebi();
		}
		// https://lightrun.com/java-tutorial-java-command-line-arguments/
		Options options = new Options();
        Option docid = new Option("docid", "docid", true, "Document ID");
        docid.setRequired(false);
        options.addOption(docid);
        Option biggest = new Option("big", "biggest", true, "Top biggest documents");
        biggest.setRequired(false);
        options.addOption(biggest);
        
        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("Purge_Webi -option1 <arg1> -option2 <arg2>", options);
            System.exit(1);
            return;
        }
        
        if (cmd.hasOption("docid")) {
            System.out.println("Document ID to be purged is : " + cmd.getOptionValue("docid"));
            PurgeDocID(cmd.getOptionValue("docid"));
        }
        if (cmd.hasOption("big")) {
            System.out.println("Number of biggest documents to be purged is : " + cmd.getOptionValue("big"));
        }
        System.exit(0);
    }
    
	private static void PurgeDocID(String DocID) {
		
    	PurgeAndSave instance = new PurgeAndSave();
		try {
			// !! Fill the parameters in the Config class !!
			restLogon();
			instance.PurgeWebi(DocID);
			restLogoff();

		} catch (Exception ex) {
			ex.printStackTrace();
			exitError("Exception on PurgeWebi() "+"Argument " + DocID);
		}
	}

	private static void PurgeBatchNo(int batchNo) {
		
		PurgeAndSave instance = new PurgeAndSave();
		try {
			// !! Fill the parameters in the Config class !!
			restLogon();
			instance.PurgeBatch(TIMEOUT_MINUTE, batchNo, batchNo);
			restLogoff();

		} catch (Exception ex) {
			ex.printStackTrace();
			exitError("Exception on PurgeBatch() "+"Argument " + batchNo);
		}
	}

    private static void PurgeAllWebi() {
    	
    	PurgeAndSave instance = new PurgeAndSave();
    	
		try {
			// !! Fill the parameters in the Config class !!
			restLogon();
			instance.PurgeBatch(TIMEOUT_MINUTE, 0);
			restLogoff();

		} catch (Exception ex) {
			ex.printStackTrace();
			exitError("Exception on PurgeAllWebi() ");
		}
    }
    
    private static void exitError(String message) {
    	
        if (message != null) {
            System.err.println(message);
        }
        try {
			restLogoff();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        System.exit(1);
   }

    private static void tessst() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String timeStamp = LocalDateTime.now().format(formatter);
		System.out.println(timeStamp + " Start time");
    	
    	try {
	    	PurgeAndSave instance = new PurgeAndSave();
	    	instance.PurgeLargest(TIMEOUT_MINUTE);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
    }
    	
    
}