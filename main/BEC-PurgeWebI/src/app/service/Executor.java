package app.service;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import app.util.RestFulUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
//import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
//import com.crystaldecisions.sdk.plugin.desktop.program.IProgramBase;

// SAP Note 1578811 Show status = "Failed" if error
//import com.crystaldecisions.sdk.plugin.desktop.program.IProgramBaseEx;
//import com.crystaldecisions.sdk.properties.IProperty;
//import com.programobject.dependencies.ProgramObjectDependency;

// SAP Note 1388696, 1501932, 2099941, 1206053 
// https://blogs.sap.com/2014/11/25/how-to-create-a-program-file-in-bi4/
public class Executor implements com.crystaldecisions.sdk.plugin.desktop.program.IProgramBase {

    public static void main(String[] args) throws Exception {
    	
//    	Executor prgObj = new Executor();
//    	prgObj.run(null, null, null);
    	 
    	 
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String timeStamp = LocalDateTime.now().format(formatter);
		System.out.println(timeStamp + " Start time");
		
		// If no command line parameter then value will < 0
		int argSize = -1;
		int argTimeout = -1;
      
		// https://lightrun.com/java-tutorial-java-command-line-arguments/
		Options options = new Options();
		
        Option docid = new Option("docid", "docid", true, "An id of WebI to be purged.");
        docid.setRequired(false);
        options.addOption(docid);
        
        Option size = new Option("size", "size", true, "Purge only fie size bigger than <arg> (byte).");
        size.setRequired(false);
        options.addOption(size);
        
        Option timeout = new Option("timeout", "timeout", true, "Runtime limit <arg> (minute).");
        size.setRequired(false);
        options.addOption(timeout);
        
        HelpFormatter h_formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            h_formatter.printHelp("Purge_Webi -option1 <arg1> [-option2 <arg2>]", options);
            System.exit(1);
            return;
        }
    
        if (cmd.hasOption("docid")) {
            System.out.println("Document ID to be purged is : " + cmd.getOptionValue("docid"));
            PurgeDocID(cmd.getOptionValue("docid"));
            System.exit(0);
            return;
        }

        if (cmd.hasOption("size")) {
            argSize = Integer.parseInt(cmd.getOptionValue("size"));
        	System.out.println("Purge only file size bigger than : " + argSize);
        }
   
        if (cmd.hasOption("timeout")) {
        	argTimeout = Integer.parseInt(cmd.getOptionValue("timeout"));
            System.out.println("Limit runing time to : " + argTimeout);
        }

        // Start purge
        PurgeAllBigThan(argTimeout, argSize);

		formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		timeStamp = LocalDateTime.now().format(formatter);
		System.out.println("\n"+timeStamp + " End time");

		//System.exit(0);		//main always return void
    }
    
	private static void PurgeDocID(String DocID) {
		
    	PurgeAndSave instance = new PurgeAndSave();
		try {
			// !! Fill the parameters in the app.properties file !!
			app.util.RestFulUtil.restLogon();
			instance.PurgeWebi(DocID);
			app.util.RestFulUtil.restLogoff();

		} catch (Exception ex) {
			ex.printStackTrace();
			exitError("Exception on PurgeWebi() "+"Argument " + DocID);
		}
	}

    private static void BatchPurgeAll() {
    	
    	PurgeAndSave instance = new PurgeAndSave();
    	
		try {
			// !! Fill the parameters in the app.properties file !!
			app.util.RestFulUtil.restLogon();
//			instance.PurgeBatch(TIMEOUT_MINUTE, 0);
			app.util.RestFulUtil.restLogoff();

		} catch (Exception ex) {
			ex.printStackTrace();
			exitError(null);
		}
    }
    
    private static void exitError(String message) {
    	
        if (message != null)
            System.err.println(message);

        try {
        	app.util.RestFulUtil.restLogoff();
		} catch (Exception ex) {
			ex.printStackTrace();
	        System.exit(1);
		}
        System.exit(1);
   }

    private static void PurgeAllBigThan(int timeout, int fileSize) {
    	try {
			// !! Fill the parameters in the Config class !!
	    	PurgeAndSave instance = new PurgeAndSave();
	    	instance.PurgeLargest(timeout, fileSize);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
    }

    // SAP Note 1388696, 1501932, 2099941, 1206053 
    // https://blogs.sap.com/2014/11/25/how-to-create-a-program-file-in-bi4/
    // implements IProgramBase (working)
//	@Override
//	public void run(IEnterpriseSession arg0, IInfoStore arg1, IInfoObject programInfoObject, String arg3, String[] arg4)
//			throws SDKException {
//		
//        programInfoObject.getProcessingInfo().properties().add("SI_PROGRAM_CAN_FAIL_JOB", Boolean.TRUE, IProperty.DIRTY);
//        programInfoObject.save();
//		
//		System.out.println("Call from IProgramBaseEx");
//		System.exit(1);
		
//		ProgramObjectDependency myProgramObjectDependency = new ProgramObjectDependency();
//		String returnValue = myProgramObjectDependency.testFunction();
//	}

    // Test
	@Override
	public void run(IEnterpriseSession arg0, IInfoStore arg1, String[] arg2) throws SDKException {

		System.out.println("run IProgramBase");
		System.exit(1);
	}
    	
    
}