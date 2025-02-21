package org.joinmastodon.android.api.requests.announcements;

import org.joinmastodon.android.api.MastodonAPIRequest;

public class AddAnnouncementReaction extends MastodonAPIRequest<Object> {
	public AddAnnouncementReaction(String id, String emoji) {
		super(HttpMethod.PUT, "/announcements/" + id + "/reactions/" + emoji, Object.class);
		setRequestBody(new Object());
	}
}
