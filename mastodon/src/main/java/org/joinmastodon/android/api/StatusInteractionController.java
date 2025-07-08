package org.joinmastodon.android.api;

import android.os.Looper;

import org.joinmastodon.android.E;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.api.requests.statuses.SetStatusBookmarked;
import org.joinmastodon.android.api.requests.statuses.SetStatusFavorited;
import org.joinmastodon.android.api.requests.statuses.SetStatusReblogged;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.EmojiReactionsUpdatedEvent;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiCategory;
import org.joinmastodon.android.model.EmojiReaction;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class StatusInteractionController{
	private final String accountID;

	// MOSHIDON: for remote interactions
	private final boolean updateCounters;

	private final HashMap<String, SetStatusFavorited> runningFavoriteRequests=new HashMap<>();
	private final HashMap<String, SetStatusReblogged> runningReblogRequests=new HashMap<>();
	private final HashMap<String, SetStatusBookmarked> runningBookmarkRequests=new HashMap<>();

	public StatusInteractionController(String accountID, boolean updateCounters){
		this.accountID=accountID;
		// MOSHIDON:
		this.updateCounters=updateCounters;
	}

	// MOSHIDON: may the overflow helps us all
	public StatusInteractionController(String accountID){
		this(accountID, true);
	}

	public void setFavorited(Status status, boolean favorited){
		setFavorited(status, favorited, r->{});
	}


	public void setFavorited(Status status, boolean favorited, Consumer<Status> cb){
		if(!Looper.getMainLooper().isCurrentThread())
			throw new IllegalStateException("Can only be called from main thread");

		// MOSHIDON:
		AccountSession session=AccountSessionManager.get(accountID);
		Instance instance=session.getInstance().get();

		SetStatusFavorited current=runningFavoriteRequests.remove(status.id);
		if(current!=null){
			current.cancel();
		}
		SetStatusFavorited req=(SetStatusFavorited) new SetStatusFavorited(status.id, favorited)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Status result){
						runningFavoriteRequests.remove(status.id);

						// MOSHIDON:
						cb.accept(result);
						if(updateCounters) E.post(new StatusCountersUpdatedEvent(result));
						if(instance.isIceshrimpJs()) E.post(new EmojiReactionsUpdatedEvent(status.id, result.reactions, false, null));
					}

					@Override
					public void onError(ErrorResponse error){
						runningFavoriteRequests.remove(status.id);
						error.showToast(MastodonApp.context);
						status.favourited=!favorited;
						if(favorited)
							status.favouritesCount--;
						else
							status.favouritesCount++;

						// MOSHIDON:
						cb.accept(status);
						if(updateCounters) E.post(new StatusCountersUpdatedEvent(status));
						if(instance.isIceshrimpJs()) E.post(new EmojiReactionsUpdatedEvent(status.id, status.reactions, false, null));
					}
				})
				.exec(accountID);
		runningFavoriteRequests.put(status.id, req);
		status.favourited=favorited;
		if(favorited)
			status.favouritesCount++;
		else
			status.favouritesCount--;

		// MOSHIDON:
		if(updateCounters) E.post(new StatusCountersUpdatedEvent(status));

		// MOSHIDON: all below is for the emojis
		if(instance.configuration==null || instance.configuration.reactions==null)
			return;

		String defaultReactionEmojiRaw=instance.configuration.reactions.defaultReaction;
		if(!instance.isIceshrimpJs() || defaultReactionEmojiRaw==null)
			return;

		boolean reactionIsCustom=defaultReactionEmojiRaw.startsWith(":");
		String defaultReactionEmoji=reactionIsCustom ? defaultReactionEmojiRaw.substring(1, defaultReactionEmojiRaw.length()-1) : defaultReactionEmojiRaw;
		ArrayList<EmojiReaction> reactions=new ArrayList<>(status.reactions.size());
		for(EmojiReaction reaction:status.reactions){
			reactions.add(reaction.copy());
		}
		Optional<EmojiReaction> existingReaction=reactions.stream().filter(r->r.me).findFirst();
		Optional<EmojiReaction> existingDefaultReaction=reactions.stream().filter(r->r.name.equals(defaultReactionEmoji)).findFirst();
		if(existingReaction.isPresent() && !favorited){
			existingReaction.get().me=false;
			existingReaction.get().count--;
			existingReaction.get().pendingChange=true;
		}else if(existingDefaultReaction.isPresent() && favorited){
			existingDefaultReaction.get().count++;
			existingDefaultReaction.get().me=true;
			existingDefaultReaction.get().pendingChange=true;
		}else if(favorited){
			EmojiReaction reaction=null;
			if(reactionIsCustom){
				List<EmojiCategory> customEmojis=AccountSessionManager.getInstance().getCustomEmojis(session.domain);
				for(EmojiCategory category:customEmojis){
					for(Emoji emoji:category.emojis){
						if(emoji.shortcode.equals(defaultReactionEmoji)){
							reaction=EmojiReaction.of(emoji, session.self);
							break;
						}
					}
				}
				if(reaction==null)
					reaction=EmojiReaction.of(defaultReactionEmoji, session.self);
			}else{
				reaction=EmojiReaction.of(defaultReactionEmoji, session.self);
			}
			reaction.pendingChange=true;
			reactions.add(reaction);
		}
		E.post(new EmojiReactionsUpdatedEvent(status.id, reactions, false, null));

	}

	public void setReblogged(Status status, boolean reblogged){
		if(!Looper.getMainLooper().isCurrentThread())
			throw new IllegalStateException("Can only be called from main thread");

		SetStatusReblogged current=runningReblogRequests.remove(status.id);
		if(current!=null){
			current.cancel();
		}
		SetStatusReblogged req=(SetStatusReblogged) new SetStatusReblogged(status.id, reblogged)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Status result){
						runningReblogRequests.remove(status.id);

						// MOSHIDON:
						if(updateCounters) E.post(new StatusCountersUpdatedEvent(result));
					}

					@Override
					public void onError(ErrorResponse error){
						runningReblogRequests.remove(status.id);
						error.showToast(MastodonApp.context);
						status.reblogged=!reblogged;
						if(reblogged)
							status.reblogsCount--;
						else
							status.reblogsCount++;
						E.post(new StatusCountersUpdatedEvent(status));
					}
				})
				.exec(accountID);
		runningReblogRequests.put(status.id, req);
		status.reblogged=reblogged;
		if(reblogged)
			status.reblogsCount++;
		else
			status.reblogsCount--;

		// MOSHIDON:
		if(updateCounters) E.post(new StatusCountersUpdatedEvent(status));
	}

	public void setBookmarked(Status status, boolean bookmarked){
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
						E.post(new StatusCountersUpdatedEvent(result));
					}

					@Override
					public void onError(ErrorResponse error){
						runningBookmarkRequests.remove(status.id);
						error.showToast(MastodonApp.context);
						status.bookmarked=!bookmarked;
						E.post(new StatusCountersUpdatedEvent(status));
					}
				})
				.exec(accountID);
		runningBookmarkRequests.put(status.id, req);
		status.bookmarked=bookmarked;

		// MOSHIDON:
		if(updateCounters) E.post(new StatusCountersUpdatedEvent(status));
	}
}
