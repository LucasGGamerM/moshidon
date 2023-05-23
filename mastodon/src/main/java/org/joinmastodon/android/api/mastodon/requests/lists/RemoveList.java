package org.joinmastodon.android.api.mastodon.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;
import java.util.List;

public class RemoveList extends MastodonAPIRequest<Object> {
    public RemoveList(String listId){
        super(HttpMethod.DELETE, "/lists/"+listId, Object.class);
    }
}
