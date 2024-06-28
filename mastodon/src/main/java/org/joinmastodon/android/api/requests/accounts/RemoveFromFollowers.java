package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

public class RemoveFromFollowers extends MastodonAPIRequest<Relationship>{
    public RemoveFromFollowers(String id){
        super(HttpMethod.POST, "/accounts/"+id+"/remove_from_followers", Relationship.class);
        setRequestBody(new Object());
    }
}
