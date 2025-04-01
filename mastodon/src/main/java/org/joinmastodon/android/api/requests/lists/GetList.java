package org.joinmastodon.android.api.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.FollowList;

public class GetList extends MastodonAPIRequest<FollowList> {
	public GetList(String id) {
		super(HttpMethod.GET, "/lists/" + id, FollowList.class);
	}
}
