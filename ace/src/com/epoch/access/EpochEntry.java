package com.epoch.access;

/** Marker object tied to session; verifies that
 * the author has owned access to this page after proper authentication.
 */
public class EpochEntry {

	/** Maximum session time (4 hrs). Improves code readability. */
	static final long sessionMax = 4 * 60 * 60 * 1000; 
	/** Time of entry. */
	transient long entryTime;
	/** Whether user has master-author permissions. */
	boolean masterEdit;

	/** Constructor.
	 * @param	userId1	login ID of this user
	 */
	public EpochEntry(String userId1) {
		// userId = userId1;
		entryTime = System.currentTimeMillis();
		masterEdit = false;
	}

	/** Gets whether this session should have expired by now.
	 * @return	true if the session has not expired 
	 */
	public boolean isValid() {
		final long currentTime = System.currentTimeMillis();
		final boolean result = currentTime - entryTime < sessionMax; 
		// System.out.println(" valid " + r);
		return result;
	}

	/** Sets master-author permissions for user. 
	 */
	public void setMasterEdit() {
		masterEdit = true;
	}

	/** Gets whether user has master-author permissions. 
	 * @return	true if user has master-author permissions
	 */
	public boolean isMasterEdit() {
		return masterEdit;
	}

}


