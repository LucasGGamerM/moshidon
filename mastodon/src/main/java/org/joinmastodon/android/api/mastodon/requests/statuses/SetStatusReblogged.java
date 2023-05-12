package org.joinmastodon.android.api.mastodon.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;

public class SetStatusReblogged extends MastodonAPIRequest<Status>{
	public SetStatusReblogged(String id, boolean reblogged, StatusPrivacy visibility){
		super(HttpMethod.POST, "/statuses/"+id+"/"+(reblogged ? "reblog" : "unreblog"), Status.class);
		Request req = new Request();
		req.visibility = visibility;
		setRequestBody(req);
	}

	public static class Request {
		public StatusPrivacy visibility;
	}
}
