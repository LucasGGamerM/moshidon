package org.joinmastodon.android.api.requests.announcements;

import org.joinmastodon.android.api.MastodonAPIRequest;

public class DeleteAnnouncementReaction extends MastodonAPIRequest<Object> {
	public DeleteAnnouncementReaction(String id, String emoji) {
		super(HttpMethod.DELETE, "/announcements/" + id + "/reactions/" + emoji, Object.class);
	}
}
