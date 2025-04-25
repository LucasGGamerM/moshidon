package org.joinmastodon.android.model;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.joinmastodon.android.api.ObjectValidationException;
import org.parceler.Parcel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Represents a user of Mastodon and their associated profile.
 */
@Parcel
public class Account extends BaseModel implements Searchable{
	// Base attributes

	/**
	 * The account id
	 */
//	@RequiredField
	public String id;
	/**
	 * The username of the account, not including domain.
	 */
//	@RequiredField
	public String username;
	/**
	 * The Webfinger account URI. Equal to username for local users, or username@domain for remote users.
	 */
//	@RequiredField
	public String acct;
	/**
	 * The location of the user's profile page.
	 */
//	@RequiredField
	public String url;

	// Display attributes

	/**
	 * The profile's display name.
	 */
//	@RequiredField
	public String displayName;
	/**
	 * The profile's bio / description.
	 */
//	@RequiredField
	public String note;
	/**
	 * An image icon that is shown next to statuses and in the profile.
	 */
//	@RequiredField
	public String avatar;
	/**
	 * A static version of the avatar. Equal to avatar if its value is a static image; different if avatar is an animated GIF.
	 */
	public String avatarStatic;
	/**
	 * An image banner that is shown above the profile and in profile cards.
	 */
//	@RequiredField
	public String header;
	/**
	 * A static version of the header. Equal to header if its value is a static image; different if header is an animated GIF.
	 */
	public String headerStatic;
	/**
	 * Whether the account manually approves follow requests.
	 */
	public boolean locked;
	/**
	 * Custom emoji entities to be used when rendering the profile. If none, an empty array will be returned.
	 */
	public List<Emoji> emojis;
	/**
	 * Whether the account has opted into discovery features such as the profile directory.
	 */
	public boolean discoverable;

	// Statistical attributes

	/**
	 * When the account was created.
	 */
//	@RequiredField
	public Instant createdAt;
	/**
	 * When the most recent status was posted.
	 */
//	@RequiredField
	public LocalDate lastStatusAt;
	/**
	 * How many statuses are attached to this account.
	 */
	public long statusesCount;
	/**
	 * The reported followers of this profile.
	 */
	public long followersCount;
	/**
	 * The reported follows of this profile.
	 */
	public long followingCount;

	// Optional attributes

	/**
	 * Indicates that the profile is currently inactive and that its user has moved to a new account.
	 */
	public Account moved;
	/**
	 * Additional metadata attached to a profile as name-value pairs.
	 */
	public List<AccountField> fields;
	/**
	 * A presentational flag. Indicates that the account may perform automated actions, may not be monitored, or identifies as a robot.
	 */
	public boolean bot;
	/**
	 * An extra entity to be used with API methods to verify credentials and update credentials.
	 */
	public Source source;
	/**
	 * An extra entity returned when an account is suspended.
	 */
	public boolean suspended;
	/**
	 * When a timed mute will expire, if applicable.
	 */
	public Instant muteExpiresAt;
	public boolean noindex;

	public List<Role> roles;

	public @Nullable String fqn; // akkoma has this, mastodon't

	@Override
	public String getQuery() {
		return url;
	}

	@Parcel
	public static class Role {
		public String name;
		/** #rrggbb */
		public String color;
	}

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(fields!=null){
			for(AccountField f:fields)
				f.postprocess();
		}else{
			// MOSHIDON:
			fields=Collections.emptyList();
		}
		if(emojis!=null){
			for(Emoji e:emojis)
				e.postprocess();
		}else{
			// MOSHIDON:
			emojis=Collections.emptyList();
		}
		if(moved!=null)
			moved.postprocess();

		// MOSHIDON: fqn block
		if(fqn==null) fqn=getFullyQualifiedName();
		if(id==null) id="";
		if(username==null) username="";

		// MOSHIDON: Akkoma stuff I believe
		if(TextUtils.isEmpty(displayName))
			displayName=!TextUtils.isEmpty(username) ? username : "";

		// MOSHIDON: I actually don't know why this is the way we are doing things here
		if(acct==null) acct="";
		if(url==null) url="";
		if(note==null) note="";
		if(avatar==null) avatar="";
	}

	public boolean isLocal(){
		return !acct.contains("@");
	}

	public String getDomain(){
		String[] parts=acct.split("@", 2);
		return parts.length==1 ? null : parts[1];
	}

	public String getDisplayUsername(){
		return '@'+acct;
	}

	// MOSHIDON:
	public String getShortUsername() {
		return '@'+acct.split("@")[0];
	}

	// MOSHIDON:
	public String getFullyQualifiedName() {
		if (TextUtils.isEmpty(acct))
			return "";
		return fqn != null ? fqn : acct.split("@")[0] + "@" + getDomainFromURL();
	}

	// MOSHIDON:
	public String getDisplayName(){
		return '\u2068'+displayName+'\u2069';
	}

	// MOSHIDON:
	public String getDomainFromURL() {
		return Uri.parse(url).getHost();
	}

	@Override
	public String toString(){
		return "Account{"+
				"id='"+id+'\''+
				", username='"+username+'\''+
				", acct='"+acct+'\''+
				", url='"+url+'\''+
				", displayName='"+displayName+'\''+
				", note='"+note+'\''+
				", avatar='"+avatar+'\''+
				", avatarStatic='"+avatarStatic+'\''+
				", header='"+header+'\''+
				", headerStatic='"+headerStatic+'\''+
				", locked="+locked+
				", emojis="+emojis+
				", discoverable="+discoverable+
				", createdAt="+createdAt+
				", lastStatusAt="+lastStatusAt+
				", statusesCount="+statusesCount+
				", followersCount="+followersCount+
				", followingCount="+followingCount+
				", moved="+moved+
				", fields="+fields+
				", bot="+bot+
				", source="+source+
				", suspended="+suspended+
				", muteExpiresAt="+muteExpiresAt+
				", noindex="+noindex+
				'}';
	}
}
