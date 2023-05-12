package org.joinmastodon.android.api.mastodon.requests.notifications;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.ApiUtils;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Notification;

import java.util.EnumSet;
import java.util.List;

public class DismissNotification extends MastodonAPIRequest<Object>{
    public DismissNotification(String id){
        super(HttpMethod.POST, "/notifications/" + (id != null ? id + "/dismiss" : "clear"), Object.class);
        setRequestBody(new Object());
    }
}
