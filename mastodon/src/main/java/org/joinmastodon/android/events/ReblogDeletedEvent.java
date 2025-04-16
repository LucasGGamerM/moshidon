package org.joinmastodon.android.events;

public class ReblogDeletedEvent{
	public final String statusID;
	public final String accountID;

	public ReblogDeletedEvent(String statusID, String accountID){
		this.statusID=statusID;
		this.accountID=accountID;
	}
}
