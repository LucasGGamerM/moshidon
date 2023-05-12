package org.joinmastodon.android.api.mastodon.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.ListTimeline;

public class DeleteList extends MastodonAPIRequest<Object> {
	public DeleteList(String id) {
		super(HttpMethod.DELETE, "/lists/" + id, Object.class);
	}
}
