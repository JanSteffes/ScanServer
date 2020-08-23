/**
 * 
 */
package data.packages.implementations;

import java.io.Serializable;
import java.util.ArrayList;

import data.ServerAction;
import data.packages.APackageData;

/**
 * @author JanSt
 *
 */
public class PackageDataDelete extends APackageData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6498300798327382888L;
	
	/**
	 * Files to delete
	 */
    public final ArrayList<String> filesToDelete;
    
	/**
	 * 
	 */
	public PackageDataDelete(ArrayList<String> filesToMerge) {
		this.filesToDelete = filesToMerge;
	}

	@Override
	public ServerAction getAction() {
		return ServerAction.DeleteFiles;
	}

}
