package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

public enum StatusPrivacy{
	@SerializedName("public")
	PUBLIC(0),
	@SerializedName("unlisted")
	UNLISTED(1),
	@SerializedName("private")
	PRIVATE(2),
	@SerializedName("direct")
	DIRECT(3),
	@SerializedName("local")
	LOCAL(4); // akkoma

	private final int privacy;

	StatusPrivacy(int privacy) {
		this.privacy = privacy;
	}

	public boolean isLessVisibleThan(StatusPrivacy other) {
		return privacy > other.getPrivacy();
	}

	public boolean isReblogPermitted(boolean isOwnStatus){
		return (this == StatusPrivacy.PUBLIC ||
				this == StatusPrivacy.UNLISTED ||
				this == StatusPrivacy.LOCAL ||
				(this == StatusPrivacy.PRIVATE && isOwnStatus));
	}

	public int getPrivacy() {
		return privacy;
	}
}
