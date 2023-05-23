package org.joinmastodon.android.api.mastodon.requests.tags;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Hashtag;

public class GetHashtag extends MastodonAPIRequest<Hashtag> {
    public GetHashtag(String name){
        super(HttpMethod.GET, "/tags/"+name, Hashtag.class);
    }
}

