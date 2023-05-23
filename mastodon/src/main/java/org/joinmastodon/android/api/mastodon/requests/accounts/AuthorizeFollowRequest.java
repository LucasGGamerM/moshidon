package org.joinmastodon.android.api.mastodon.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

public class AuthorizeFollowRequest extends MastodonAPIRequest<Relationship>{
    public AuthorizeFollowRequest(String id){
        super(HttpMethod.POST, "/follow_requests/"+id+"/authorize", Relationship.class);
        setRequestBody(new Object());
    }
}
