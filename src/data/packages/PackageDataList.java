/**
 * 
 */
package data.packages;

import java.io.Serializable;

/**
 * @author JanSt
 *
 */
public class PackageDataList extends APackageData implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8483857577336184959L;

	public PackageDataList() {
    }

    public String toString() {
        return "PackageDataList";
    }

	@Override
	public ServerAction getAction() {
		return ServerAction.ReadFiles;
	}
}
