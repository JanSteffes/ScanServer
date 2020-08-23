package testClient;

import java.util.ArrayList;
import java.util.Arrays;

import data.packages.implementations.PackageDataDelete;
import data.packages.implementations.PackageDataList;
import data.packages.implementations.PackageDataMerge;
import data.packages.implementations.PackageDataScan;

/** client tries to connect to server
// server (if no other client connected already) accepts connection
// client receives connection established and points out options:
// low quali (fast, may bad result), medium quali (decent, decent) and good quali (slow, good quali)
// will send chosen option to server (if taking longer than 30 sec, server will close connection)
// server tries to do chosen option
// server sends result
 * 
 * @author JanSt
 *
 */
public class ClientTest {

	public static void main(String[] args) {
		
		try {			
						
			// list
			String[] fileList = new PackageDataList().Execute().split(";");
			
			// scan
			//new PackageDataScan(1, "ClientTest_Scan").Execute();
			
			// scan again to merge later if not enough files are there
			while (fileList.length < 3)
			{			
				new PackageDataScan(1, "ClientTest_Scan").Execute();
				fileList = new PackageDataList().Execute().split(";");
			}
			
			// merge
			String[] filesToMerge = Arrays.copyOfRange(fileList, 0, 2);
			ArrayList<String> filestoMergeList = new ArrayList<String>(Arrays.asList(filesToMerge));
			new PackageDataMerge("ClientTest_Merge", filestoMergeList).Execute();
			
			// and list again to validate
			fileList = new PackageDataList().Execute().split(";");	
			
		   ArrayList<String> filesToDelete = new ArrayList<String>();
		   filesToDelete.add("test1");
		   filesToDelete.add("test2");
		   new PackageDataDelete(filesToDelete).Execute();		   
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
}
