package org.joinmastodon.android.api.mastodon.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.ListTimeline;

public class CreateList extends MastodonAPIRequest<ListTimeline> {
	public CreateList(String title, ListTimeline.RepliesPolicy repliesPolicy) {
		super(HttpMethod.POST, "/lists", ListTimeline.class);
		Request req = new Request();
		req.title = title;
		req.repliesPolicy = repliesPolicy;
		setRequestBody(req);
	}

	public static class Request {
		public String title;
		public ListTimeline.RepliesPolicy repliesPolicy;
	}
}
