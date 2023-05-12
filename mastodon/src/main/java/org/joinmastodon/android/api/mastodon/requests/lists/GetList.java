package org.joinmastodon.android.api.mastodon.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.ListTimeline;

public class GetList extends MastodonAPIRequest<ListTimeline> {
	public GetList(String id) {
		super(HttpMethod.GET, "/lists/" + id, ListTimeline.class);
	}
}
