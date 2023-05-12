package org.joinmastodon.android.api.mastodon.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.ListTimeline;

public class UpdateList extends MastodonAPIRequest<ListTimeline> {
	public UpdateList(String id, String title, ListTimeline.RepliesPolicy repliesPolicy) {
		super(HttpMethod.PUT, "/lists/" + id, ListTimeline.class);
		CreateList.Request req = new CreateList.Request();
		req.title = title;
		req.repliesPolicy = repliesPolicy;
		setRequestBody(req);
	}
}
