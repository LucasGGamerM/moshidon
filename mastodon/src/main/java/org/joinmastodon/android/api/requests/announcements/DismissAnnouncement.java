package org.joinmastodon.android.api.requests.announcements;

import org.joinmastodon.android.api.MastodonAPIRequest;

public class DismissAnnouncement extends MastodonAPIRequest<Object>{
	public DismissAnnouncement(String id){
		super(HttpMethod.POST, "/announcements/" + id + "/dismiss", Object.class);
		setRequestBody(new Object());
	}
}
