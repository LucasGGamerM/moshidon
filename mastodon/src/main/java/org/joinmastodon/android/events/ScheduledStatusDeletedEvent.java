package org.joinmastodon.android.events;

public class ScheduledStatusDeletedEvent{
	public final String id;
	public final String accountID;

	public ScheduledStatusDeletedEvent(String id, String accountID){
		this.id=id;
		this.accountID=accountID;
	}
}
