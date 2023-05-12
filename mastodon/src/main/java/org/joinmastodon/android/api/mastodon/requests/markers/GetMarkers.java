package org.joinmastodon.android.api.mastodon.requests.markers;

import org.joinmastodon.android.api.ApiUtils;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Marker;
import org.joinmastodon.android.model.Markers;

import java.util.EnumSet;

public class GetMarkers extends MastodonAPIRequest<Markers> {
	public GetMarkers(EnumSet<Marker.Type> timelines) {
		super(HttpMethod.GET, "/markers", Markers.class);
		for (String type : ApiUtils.enumSetToStrings(timelines, Marker.Type.class)){
			addQueryParameter("timeline[]", type);
		}
	}
}
