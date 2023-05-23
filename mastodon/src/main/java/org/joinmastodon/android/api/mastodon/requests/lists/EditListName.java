package org.joinmastodon.android.api.mastodon.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;
import java.util.List;

public class EditListName extends MastodonAPIRequest<Object> {
    public EditListName(String newListName, String listId){
        super(HttpMethod.PUT, "/lists/"+listId, Object.class);
        Request req = new Request();
        req.title = newListName;
        setRequestBody(req);
    }

    public static class Request{
        public String title;
    }
}
