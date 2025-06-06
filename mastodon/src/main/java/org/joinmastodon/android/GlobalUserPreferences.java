package org.joinmastodon.android;

import static org.joinmastodon.android.api.MastodonAPIController.gson;
import static org.joinmastodon.android.api.session.AccountLocalPreferences.ColorPreference.MATERIAL3;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonSyntaxException;

import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;

import java.lang.reflect.Type;

public class GlobalUserPreferences{
	public static boolean playGifs;
	public static boolean useCustomTabs;
	public static boolean altTextReminders, confirmUnfollow, confirmBoost, confirmDeletePost;
	public static ThemePreference theme=ThemePreference.AUTO;
	public static boolean useDynamicColors;
	public static boolean showInteractionCounts;
	public static boolean customEmojiInNames;
	public static boolean showCWs;
	public static boolean hideSensitiveMedia;

	// MOSHIDON:
	public static boolean trueBlackTheme;
	public static boolean loadNewPosts;
	public static boolean showNewPostsButton;
	public static boolean toolbarMarquee;
	public static boolean disableSwipe;
	public static boolean enableDeleteNotifications;
	public static boolean translateButtonOpenedOnly;
	public static boolean uniformNotificationIcon;
	public static boolean reduceMotion;
	public static boolean showAltIndicator;
	public static boolean showNoAltIndicator;
	public static boolean enablePreReleases;
	public static PrefixRepliesMode prefixReplies;
	public static boolean collapseLongPosts;
	public static boolean spectatorMode;
	public static boolean autoHideFab;
	public static boolean allowRemoteLoading;
	public static AutoRevealMode autoRevealEqualSpoilers;
	public static boolean disableM3PillActiveIndicator;
	public static boolean showNavigationLabels;
	public static boolean displayPronounsInTimelines, displayPronounsInThreads, displayPronounsInUserListings;
	public static boolean overlayMedia;
	public static boolean showSuicideHelp;
	public static boolean underlinedLinks;
	public static AccountLocalPreferences.ColorPreference color;
	public static boolean likeIcon;
	public static boolean showDividers;
	public static boolean relocatePublishButton;
	public static boolean defaultToUnlistedReplies;
	public static boolean doubleTapToSearch;
	public static boolean doubleTapToSwipe;
	public static boolean confirmBeforeReblog;
	public static boolean hapticFeedback;
	public static boolean replyLineAboveHeader;
	public static boolean swapBookmarkWithBoostAction;
	public static boolean mentionRebloggerAutomatically;
	public static boolean showPostsWithoutAlt;
	public static boolean showMediaPreview;
	public static boolean removeTrackingParams;

	private static SharedPreferences getPrefs(){
		return MastodonApp.context.getSharedPreferences("global", Context.MODE_PRIVATE);
	}

	private static SharedPreferences getPreReplyPrefs(){
		return MastodonApp.context.getSharedPreferences("pre_reply_sheets", Context.MODE_PRIVATE);
	}

	public static void load(){
		SharedPreferences prefs=getPrefs();
		playGifs=prefs.getBoolean("playGifs", true);
		useCustomTabs=prefs.getBoolean("useCustomTabs", true);
		altTextReminders=prefs.getBoolean("altTextReminders", false);
		confirmUnfollow=prefs.getBoolean("confirmUnfollow", false);
		confirmBoost=prefs.getBoolean("confirmBoost", false);
		confirmDeletePost=prefs.getBoolean("confirmDeletePost", true);
		theme=ThemePreference.values()[prefs.getInt("theme", 0)];
		useDynamicColors=prefs.getBoolean("useDynamicColors", true);
		showInteractionCounts=prefs.getBoolean("interactionCounts", true);
		customEmojiInNames=prefs.getBoolean("emojiInNames", true);
		showCWs=prefs.getBoolean("showCWs", true);
		hideSensitiveMedia=prefs.getBoolean("hideSensitive", true);

		// MOSHIDON
		trueBlackTheme=prefs.getBoolean("trueBlackTheme", false);
		loadNewPosts=prefs.getBoolean("loadNewPosts", true);
		showNewPostsButton=prefs.getBoolean("showNewPostsButton", true);
		toolbarMarquee=prefs.getBoolean("toolbarMarquee", true);
		disableSwipe=prefs.getBoolean("disableSwipe", false);
		enableDeleteNotifications=prefs.getBoolean("enableDeleteNotifications", false);
		translateButtonOpenedOnly=prefs.getBoolean("translateButtonOpenedOnly", false);
		uniformNotificationIcon=prefs.getBoolean("uniformNotificationIcon", false);
		reduceMotion=prefs.getBoolean("reduceMotion", false);
		showAltIndicator=prefs.getBoolean("showAltIndicator", true);
		showNoAltIndicator=prefs.getBoolean("showNoAltIndicator", true);
		enablePreReleases=prefs.getBoolean("enablePreReleases", false);
		prefixReplies=PrefixRepliesMode.valueOf(prefs.getString("prefixReplies", PrefixRepliesMode.NEVER.name()));
		collapseLongPosts=prefs.getBoolean("collapseLongPosts", true);
		spectatorMode=prefs.getBoolean("spectatorMode", false);
		autoHideFab=prefs.getBoolean("autoHideFab", true);
		allowRemoteLoading=prefs.getBoolean("allowRemoteLoading", true);
		autoRevealEqualSpoilers=AutoRevealMode.valueOf(prefs.getString("autoRevealEqualSpoilers", AutoRevealMode.THREADS.name()));
		disableM3PillActiveIndicator=prefs.getBoolean("disableM3PillActiveIndicator", false);
		showNavigationLabels=prefs.getBoolean("showNavigationLabels", true);
		displayPronounsInTimelines=prefs.getBoolean("displayPronounsInTimelines", true);
		displayPronounsInThreads=prefs.getBoolean("displayPronounsInThreads", true);
		displayPronounsInUserListings=prefs.getBoolean("displayPronounsInUserListings", true);
		overlayMedia=prefs.getBoolean("overlayMedia", false);
		showSuicideHelp=prefs.getBoolean("showSuicideHelp", true);
		underlinedLinks=prefs.getBoolean("underlinedLinks", true);
		color=AccountLocalPreferences.ColorPreference.valueOf(prefs.getString("color", MATERIAL3.name()));
		likeIcon=prefs.getBoolean("likeIcon", false);
		uniformNotificationIcon=prefs.getBoolean("uniformNotificationIcon", false);
		showDividers =prefs.getBoolean("showDividers", false);
		relocatePublishButton=prefs.getBoolean("relocatePublishButton", true);
		defaultToUnlistedReplies=prefs.getBoolean("defaultToUnlistedReplies", false);
		doubleTapToSearch =prefs.getBoolean("doubleTapToSearch", true);
		doubleTapToSwipe =prefs.getBoolean("doubleTapToSwipe", true);
		replyLineAboveHeader=prefs.getBoolean("replyLineAboveHeader", true);
		confirmBeforeReblog=prefs.getBoolean("confirmBeforeReblog", false);
		hapticFeedback=prefs.getBoolean("hapticFeedback", true);
		swapBookmarkWithBoostAction=prefs.getBoolean("swapBookmarkWithBoostAction", false);
		mentionRebloggerAutomatically=prefs.getBoolean("mentionRebloggerAutomatically", false);
		showPostsWithoutAlt=prefs.getBoolean("showPostsWithoutAlt", true);
		showMediaPreview=prefs.getBoolean("showMediaPreview", true);
		removeTrackingParams=prefs.getBoolean("removeTrackingParams", true);
//		enhanceTextSize=prefs.getBoolean("enhanceTextSize", false);


		if(!prefs.getBoolean("perAccountMigrationDone", false)){
			AccountSession account=AccountSessionManager.getInstance().getLastActiveAccount();
			if(account!=null){
				SharedPreferences accPrefs=account.getRawLocalPreferences();
				showInteractionCounts=accPrefs.getBoolean("interactionCounts", true);
				customEmojiInNames=accPrefs.getBoolean("emojiInNames", true);
				showCWs=accPrefs.getBoolean("showCWs", true);
				hideSensitiveMedia=accPrefs.getBoolean("hideSensitive", true);
				save();
			}
			// Also applies to new app installs
			prefs.edit().putBoolean("perAccountMigrationDone", true).apply();
		}
	}

	public static void save(){
		getPrefs().edit()
				.putBoolean("playGifs", playGifs)
				.putBoolean("useCustomTabs", useCustomTabs)
				.putInt("theme", theme.ordinal())
				.putBoolean("altTextReminders", altTextReminders)
				.putBoolean("confirmUnfollow", confirmUnfollow)
				.putBoolean("confirmBoost", confirmBoost)
				.putBoolean("confirmDeletePost", confirmDeletePost)
				.putBoolean("useDynamicColors", useDynamicColors)
				.putBoolean("interactionCounts", showInteractionCounts)
				.putBoolean("emojiInNames", customEmojiInNames)
				.putBoolean("showCWs", showCWs)
				.putBoolean("hideSensitive", hideSensitiveMedia)

				// MOSHIDON
				.putBoolean("loadNewPosts", loadNewPosts)
				.putBoolean("showNewPostsButton", showNewPostsButton)
				.putBoolean("trueBlackTheme", trueBlackTheme)
				.putBoolean("toolbarMarquee", toolbarMarquee)
				.putBoolean("disableSwipe", disableSwipe)
				.putBoolean("enableDeleteNotifications", enableDeleteNotifications)
				.putBoolean("translateButtonOpenedOnly", translateButtonOpenedOnly)
				.putBoolean("uniformNotificationIcon", uniformNotificationIcon)
				.putBoolean("reduceMotion", reduceMotion)
				.putBoolean("showAltIndicator", showAltIndicator)
				.putBoolean("showNoAltIndicator", showNoAltIndicator)
				.putBoolean("enablePreReleases", enablePreReleases)
				.putString("prefixReplies", prefixReplies.name())
				.putBoolean("collapseLongPosts", collapseLongPosts)
				.putBoolean("spectatorMode", spectatorMode)
				.putBoolean("autoHideFab", autoHideFab)
				.putBoolean("allowRemoteLoading", allowRemoteLoading)
				.putString("autoRevealEqualSpoilers", autoRevealEqualSpoilers.name())
				.putBoolean("disableM3PillActiveIndicator", disableM3PillActiveIndicator)
				.putBoolean("showNavigationLabels", showNavigationLabels)
				.putBoolean("displayPronounsInTimelines", displayPronounsInTimelines)
				.putBoolean("displayPronounsInThreads", displayPronounsInThreads)
				.putBoolean("displayPronounsInUserListings", displayPronounsInUserListings)
				.putBoolean("overlayMedia", overlayMedia)
				.putBoolean("showSuicideHelp", showSuicideHelp)
				.putBoolean("underlinedLinks", underlinedLinks)
				.putString("color", color.name())
				.putBoolean("likeIcon", likeIcon)
				.putBoolean("defaultToUnlistedReplies", defaultToUnlistedReplies)
				.putBoolean("doubleTapToSearch", doubleTapToSearch)
				.putBoolean("doubleTapToSwipe", doubleTapToSwipe)
				.putBoolean("replyLineAboveHeader", replyLineAboveHeader)
				.putBoolean("confirmBeforeReblog", confirmBeforeReblog)
				.putBoolean("swapBookmarkWithBoostAction", swapBookmarkWithBoostAction)
				.putBoolean("hapticFeedback", hapticFeedback)
				.putBoolean("mentionRebloggerAutomatically", mentionRebloggerAutomatically)
				.putBoolean("showDividers", showDividers)
				.putBoolean("relocatePublishButton", relocatePublishButton)
				.putBoolean("enableDeleteNotifications", enableDeleteNotifications)
				.putBoolean("showPostsWithoutAlt", showPostsWithoutAlt)
				.putBoolean("showMediaPreview", showMediaPreview)
				.putBoolean("removeTrackingParams", removeTrackingParams)
//				.putBoolean("enhanceTextSize", enhanceTextSize)

				.apply();
	}

	public static boolean isOptedOutOfPreReplySheet(PreReplySheetType type, Account account, String accountID){
		if(getPreReplyPrefs().getBoolean("opt_out_"+type, false))
			return true;
		if(account==null)
			return false;
		String accountKey=account.acct;
		if(!accountKey.contains("@"))
			accountKey+="@"+AccountSessionManager.get(accountID).domain;
		return getPreReplyPrefs().getBoolean("opt_out_"+type+"_"+accountKey.toLowerCase(), false);
	}

	public static void optOutOfPreReplySheet(PreReplySheetType type, Account account, String accountID){
		String key;
		if(account==null){
			key="opt_out_"+type;
		}else{
			String accountKey=account.acct;
			if(!accountKey.contains("@"))
				accountKey+="@"+AccountSessionManager.get(accountID).domain;
			key="opt_out_"+type+"_"+accountKey.toLowerCase();
		}
		getPreReplyPrefs().edit().putBoolean(key, true).apply();
	}

	public static void resetPreReplySheets(){
		getPreReplyPrefs().edit().clear().apply();
	}

	public enum ThemePreference{
		AUTO,
		LIGHT,
		DARK
	}

	public enum PreReplySheetType{
		OLD_POST,
		NON_MUTUAL
	}

	// MOSHIDON:
	public enum AutoRevealMode {
		NEVER,
		THREADS,
		DISCUSSIONS
	}

	// MOSHIDON:
	public enum PrefixRepliesMode {
		NEVER,
		ALWAYS,
		TO_OTHERS
	}

	// MOSHIDON: we have jason
	public static <T> T fromJson(String json, Type type, T orElse){
		if(json==null) return orElse;
		try{
			T value=gson.fromJson(json, type);
			return value==null ? orElse : value;
		}catch(JsonSyntaxException ignored){
			return orElse;
		}
	}

	// MOSHIDON: enums too!
	public static <T extends Enum<T>> T enumValue(Class<T> enumType, String name) {
		try { return Enum.valueOf(enumType, name); }
		catch (NullPointerException npe) { return null; }
	}
}
