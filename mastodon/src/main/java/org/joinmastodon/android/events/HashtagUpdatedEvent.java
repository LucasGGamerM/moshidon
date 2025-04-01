package org.joinmastodon.android.events;

public class HashtagUpdatedEvent {
	public final String name;
	public final boolean following;

	public HashtagUpdatedEvent(String name, boolean following) {
		this.name = name;
		this.following = following;
	}
}
