package org.joinmastodon.android.api.requests.announcements;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Announcement;

import java.util.List;

public class GetAnnouncements extends MastodonAPIRequest<List<Announcement>> {
	public GetAnnouncements(boolean withDismissed) {
		super(MastodonAPIRequest.HttpMethod.GET, "/announcements", new TypeToken<>(){});
		addQueryParameter("with_dismissed", withDismissed ? "true" : "false");
	}
}
