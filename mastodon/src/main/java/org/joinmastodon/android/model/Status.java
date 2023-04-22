package org.joinmastodon.android.model;

import static org.joinmastodon.android.api.MastodonAPIController.gson;
import static org.joinmastodon.android.api.MastodonAPIController.gsonWithoutDeserializer;

import androidx.annotation.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.parceler.Parcel;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;

@Parcel
public class Status extends BaseModel implements DisplayItemsParent, Searchable{
	@RequiredField
	public String id;
	@RequiredField
	public String uri;
//	@RequiredField // sometimes null on calckey
	public Instant createdAt;
	@RequiredField
	public Account account;
	//	@RequiredField
	public String content;
	@RequiredField
	public StatusPrivacy visibility;
	public boolean sensitive;
	@RequiredField
	public String spoilerText;
	@RequiredField
	public List<Attachment> mediaAttachments;
	public Application application;
	@RequiredField
	public List<Mention> mentions;
	@RequiredField
	public List<Hashtag> tags;
	@RequiredField
	public List<Emoji> emojis;
	public long reblogsCount;
	public long favouritesCount;
	public long repliesCount;
	public Instant editedAt;
	public List<FilterResult> filtered;

	public String url;
	public String inReplyToId;
	public String inReplyToAccountId;
	public Status reblog;
	public Poll poll;
	public Card card;
	public String language;
	public String text;
	@Nullable
	public Account rebloggedBy;
	public boolean localOnly;

	public boolean favourited;
	public boolean reblogged;
	public boolean muted;
	public boolean bookmarked;
	public boolean pinned;

	public Status quote; // can be boolean in calckey

	public transient boolean filterRevealed;
	public transient boolean spoilerRevealed;
	public transient boolean textExpanded, textExpandable;
	public transient boolean hasGapAfter;
	public transient TranslatedStatus translation;
	public transient boolean translationShown;
	public boolean reloadWhenClicked;
	private transient String strippedText;

	@Override
	public void postprocess() throws ObjectValidationException{
		if(spoilerText!=null && !spoilerText.isEmpty() && !sensitive)
			sensitive=true;

		super.postprocess();
		if(application!=null)
			application.postprocess();
		for(Mention m:mentions)
			m.postprocess();
		for(Hashtag t:tags)
			t.postprocess();
		for(Emoji e:emojis)
			e.postprocess();
		for(Attachment a:mediaAttachments)
			a.postprocess();
		account.postprocess();
		if(poll!=null)
			poll.postprocess();
		if(card!=null)
			card.postprocess();
		if(reblog!=null)
			reblog.postprocess();

		spoilerRevealed=GlobalUserPreferences.alwaysExpandContentWarnings || !sensitive;
		if (visibility.equals(StatusPrivacy.LOCAL)) localOnly = true;
	}

	@Override
	public String toString(){
		return "Status{"+
				"id='"+id+'\''+
				", uri='"+uri+'\''+
				", createdAt="+createdAt+
				", account="+account+
				", content='"+content+'\''+
				", visibility="+visibility+
				", sensitive="+sensitive+
				", spoilerText='"+spoilerText+'\''+
				", mediaAttachments="+mediaAttachments+
				", application="+application+
				", mentions="+mentions+
				", tags="+tags+
				", emojis="+emojis+
				", reblogsCount="+reblogsCount+
				", favouritesCount="+favouritesCount+
				", repliesCount="+repliesCount+
				", url='"+url+'\''+
				", inReplyToId='"+inReplyToId+'\''+
				", inReplyToAccountId='"+inReplyToAccountId+'\''+
				", reblog="+reblog+
				", poll="+poll+
				", card="+card+
				", language='"+language+'\''+
				", text='"+text+'\''+
				", favourited="+favourited+
				", reblogged="+reblogged+
				", muted="+muted+
				", bookmarked="+bookmarked+
				", pinned="+pinned+
				'}';
	}

	@Override
	public String getID(){
		return id;
	}

	public void update(StatusCountersUpdatedEvent ev){
		favouritesCount=ev.favorites;
		reblogsCount=ev.reblogs;
		repliesCount=ev.replies;
		favourited=ev.favorited;
		reblogged=ev.reblogged;
		bookmarked=ev.bookmarked;
		pinned=ev.pinned;
	}

	public Status getContentStatus(){
		return reblog!=null ? reblog : this;
	}

	public String getStrippedText(){
		if(strippedText==null)
			strippedText=HtmlParser.strip(content);
		return strippedText;
	}

	public static Status ofFake(String id, String text, Instant createdAt) {
		Status s = new Status();
		s.id = id;
		s.mediaAttachments = List.of();
		s.createdAt = createdAt;
		s.content = s.text = text;
		s.spoilerText = "";
		s.visibility = StatusPrivacy.PUBLIC;
		s.mentions = List.of();
		s.tags = List.of();
		s.emojis = List.of();
		s.filtered = List.of();
		return s;
	}

	@Override
	public String getQuery() {
		return url;
	}

	public static class StatusDeserializer implements JsonDeserializer<Status> {

		@Override
		public Status deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject obj = json.getAsJsonObject();

			Status quote = null;
			if (obj.has("quote") && obj.get("quote").isJsonObject())
				quote = gson.fromJson(obj.get("quote"), Status.class);
			obj.remove("quote");

			Status reblog = null;
			if (obj.has("reblog"))
				reblog = gson.fromJson(obj.get("reblog"), Status.class);
			obj.remove("reblog");

			Status status = gsonWithoutDeserializer.fromJson(json, Status.class);
			status.quote = quote;
			status.reblog = reblog;

			return status;
		}
	}
}
