package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;

@Parcel
public class Notification extends BaseModel implements DisplayItemsParent{
	@RequiredField
	public String id;
//	@RequiredField
	public Type type;
	@RequiredField
	public Instant createdAt;
	@RequiredField
	public Account account;
	public Status status;
	public Report report;
	public String emoji;
	public String emojiUrl;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		account.postprocess();
		if(status!=null)
			status.postprocess();
	}

	@Override
	public String getID(){
		return id;
	}

	public enum Type{
		@SerializedName("follow")
		FOLLOW,
		@SerializedName("follow_request")
		FOLLOW_REQUEST,
		@SerializedName("mention")
		MENTION,
		@SerializedName("reblog")
		REBLOG,
		@SerializedName("favourite")
		FAVORITE,
		@SerializedName("poll")
		POLL,
		@SerializedName("status")
		STATUS,
		@SerializedName("update")
		UPDATE,
		@SerializedName("admin.sign_up")
		SIGN_UP,
		@SerializedName("admin.report")
		REPORT,
		@SerializedName("pleroma:emoji_reaction")
		EMOJI_REACTION
	}

	@Parcel
	public static class Report {
		public String id;
		public String comment;
		public Account targetAccount;
	}
}
