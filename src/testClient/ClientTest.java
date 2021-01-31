package testClient;

/** client tries to connect to server
// server (if no other client connected already) accepts connection
// client receives connection established and points out options:
// low quality (fast, may bad result), medium quality (decent, decent) and good quality (slow, good quality)
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
					System.out.println("NEED TO IMPLEMENT!");

/*
 list
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
			ArrayList<String> filesToMergeList = new ArrayList<String>(Arrays.asList(filesToMerge));
			new PackageDataMerge("ClientTest_Merge", filesToMergeList).Execute();

			// and list again to validate
			fileList = new PackageDataList().Execute().split(";");

		   ArrayList<String> filesToDelete = new ArrayList<String>();
		   filesToDelete.add("test1");
		   filesToDelete.add("test2");
		   new PackageDataDelete(filesToDelete).Execute();
*/

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
}
