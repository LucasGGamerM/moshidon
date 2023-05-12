package org.joinmastodon.android.api.mastodon.requests.tags;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Hashtag;

public class SetHashtagFollowed extends MastodonAPIRequest<Hashtag>{
    public SetHashtagFollowed(String name, boolean followed){
        super(HttpMethod.POST, "/tags/"+name+"/"+(followed ? "follow" : "unfollow"), Hashtag.class);
        setRequestBody(new Object());
    }
}
