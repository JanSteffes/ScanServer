/**
 * 
 */
package data.packages;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author JanSt
 *
 */
public class PackageDataMerge extends APackageData implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5006935905820211682L;

	/**
	 * Files to merge
	 */
    public final ArrayList<String> filesToMerge;
    
    /**
     * Name of file after merging
     */
    public final String mergedFileName;

    public PackageDataMerge(String mergedFilename, ArrayList<String> filesToMerge) {
        this.mergedFileName = mergedFilename;
        this.filesToMerge = filesToMerge;
    }

    public String toString() {
        return "PackageDataMerge: " + mergedFileName + "|" + String.join(",", filesToMerge);
    }

	@Override
	public ServerAction getAction() {
		return ServerAction.MergeFiles;
	}
}