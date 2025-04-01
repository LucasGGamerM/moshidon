package org.joinmastodon.android.api.requests.statuses;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.EmojiReaction;

import java.util.List;

public class PleromaGetStatusReactions extends MastodonAPIRequest<List<EmojiReaction>> {
    public PleromaGetStatusReactions(String id, String emoji) {
        super(HttpMethod.GET, "/pleroma/statuses/" + id + "/reactions/" + (emoji != null ? emoji : ""), new TypeToken<>(){});
    }
}
