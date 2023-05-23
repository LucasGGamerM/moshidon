package org.joinmastodon.android.api.mastodon.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;
import java.util.List;

public class RemoveAccountsFromList extends MastodonAPIRequest<Object> {
    public RemoveAccountsFromList(String listId, List<String> accountIds){
        super(HttpMethod.DELETE, "/lists/"+listId+"/accounts", Object.class);
        Request req = new Request();
        req.accountIds = accountIds;
        setRequestBody(req);
    }

    public static class Request{
        public List<String> accountIds;
    }
}
