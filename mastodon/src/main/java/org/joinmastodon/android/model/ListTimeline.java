package org.joinmastodon.android.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

@Parcel
public class ListTimeline extends BaseModel {
	@RequiredField
	public String id;
	@RequiredField
	public String title;
	public RepliesPolicy repliesPolicy;
	public boolean exclusive;

	@NonNull
	@Override
	public String toString() {
		return "List{" +
				"id='" + id + '\'' +
				", title='" + title + '\'' +
				", repliesPolicy=" + repliesPolicy +
				", exclusive=" + exclusive +
				'}';
	}

	public enum RepliesPolicy{
		@SerializedName("followed")
		FOLLOWED,
		@SerializedName("list")
		LIST,
		@SerializedName("none")
		NONE
	}
}
