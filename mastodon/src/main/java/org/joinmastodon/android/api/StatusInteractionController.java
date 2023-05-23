package org.joinmastodon.android.api;

import android.os.Looper;

import org.joinmastodon.android.E;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.api.mastodon.requests.statuses.SetStatusBookmarked;
import org.joinmastodon.android.api.mastodon.requests.statuses.SetStatusFavorited;
import org.joinmastodon.android.api.mastodon.requests.statuses.SetStatusReblogged;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;

import java.util.HashMap;
import java.util.function.Consumer;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class StatusInteractionController{
	private final String accountID;
	private final boolean updateCounters;
	private final HashMap<String, SetStatusFavorited> runningFavoriteRequests=new HashMap<>();
	private final HashMap<String, SetStatusReblogged> runningReblogRequests=new HashMap<>();
	private final HashMap<String, SetStatusBookmarked> runningBookmarkRequests=new HashMap<>();

	public StatusInteractionController(String accountID, boolean updateCounters) {
		this.accountID=accountID;
		this.updateCounters=updateCounters;
	}

	public StatusInteractionController(String accountID){
		this(accountID, true);
	}

	public void setFavorited(Status status, boolean favorited, Consumer<Status> cb){
		if(!Looper.getMainLooper().isCurrentThread())
			throw new IllegalStateException("Can only be called from main thread");

		SetStatusFavorited current=runningFavoriteRequests.remove(status.id);
		if(current!=null){
			current.cancel();
		}
		SetStatusFavorited req=(SetStatusFavorited) new SetStatusFavorited(status.id, favorited)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Status result){
						runningFavoriteRequests.remove(status.id);
						result.favouritesCount = Math.max(0, status.favouritesCount) + (favorited ? 1 : -1);
						cb.accept(result);
						if (updateCounters) E.post(new StatusCountersUpdatedEvent(result));
					}

					@Override
					public void onError(ErrorResponse error){
						runningFavoriteRequests.remove(status.id);
						error.showToast(MastodonApp.context);
						status.favourited=!favorited;
						cb.accept(status);
						if (updateCounters) E.post(new StatusCountersUpdatedEvent(status));
					}
				})
				.exec(accountID);
		runningFavoriteRequests.put(status.id, req);
		status.favourited=favorited;
		if (updateCounters) E.post(new StatusCountersUpdatedEvent(status));
	}

	public void setReblogged(Status status, boolean reblogged, StatusPrivacy visibility, Consumer<Status> cb){
		if(!Looper.getMainLooper().isCurrentThread())
			throw new IllegalStateException("Can only be called from main thread");

		SetStatusReblogged current=runningReblogRequests.remove(status.id);
		if(current!=null){
			current.cancel();
		}
		SetStatusReblogged req=(SetStatusReblogged) new SetStatusReblogged(status.id, reblogged, visibility)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Status reblog){
						Status result = reblog.getContentStatus();
						runningReblogRequests.remove(status.id);
						result.reblogsCount = Math.max(0, status.reblogsCount) + (reblogged ? 1 : -1);
						cb.accept(result);
						if (updateCounters) E.post(new StatusCountersUpdatedEvent(result));
					}

					@Override
					public void onError(ErrorResponse error){
						runningReblogRequests.remove(status.id);
						error.showToast(MastodonApp.context);
						status.reblogged=!reblogged;
						cb.accept(status);
						if (updateCounters) E.post(new StatusCountersUpdatedEvent(status));
					}
				})
				.exec(accountID);
		runningReblogRequests.put(status.id, req);
		status.reblogged=reblogged;
		if (updateCounters) E.post(new StatusCountersUpdatedEvent(status));
	}

	public void setBookmarked(Status status, boolean bookmarked){
		setBookmarked(status, bookmarked, r->{});
	}

	public void setBookmarked(Status status, boolean bookmarked, Consumer<Status> cb){
		if(!Looper.getMainLooper().isCurrentThread())
			throw new IllegalStateException("Can only be called from main thread");

		SetStatusBookmarked current=runningBookmarkRequests.remove(status.id);
		if(current!=null){
			current.cancel();
		}
		SetStatusBookmarked req=(SetStatusBookmarked) new SetStatusBookmarked(status.id, bookmarked)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Status result){
						runningBookmarkRequests.remove(status.id);
						cb.accept(result);
						if (updateCounters) E.post(new StatusCountersUpdatedEvent(result));
					}

					@Override
					public void onError(ErrorResponse error){
						runningBookmarkRequests.remove(status.id);
						error.showToast(MastodonApp.context);
						status.bookmarked=!bookmarked;
						cb.accept(status);
						if (updateCounters) E.post(new StatusCountersUpdatedEvent(status));
					}
				})
				.exec(accountID);
		runningBookmarkRequests.put(status.id, req);
		status.bookmarked=bookmarked;
		if (updateCounters) E.post(new StatusCountersUpdatedEvent(status));
	}
}
