package org.joinmastodon.android;

import static org.joinmastodon.android.api.MastodonAPIController.gson;
import static org.joinmastodon.android.api.session.AccountLocalPreferences.ColorPreference.MATERIAL3;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.StringRes;
import android.os.Build;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountLocalPreferences.ColorPreference;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.ContentType;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.TimelineDefinition;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GlobalUserPreferences{
	private static final String TAG="GlobalUserPreferences";

	public static boolean playGifs;
	public static boolean useCustomTabs;
	public static boolean altTextReminders, confirmUnfollow, confirmBoost, confirmDeletePost;
	public static ThemePreference theme;

	// MEGALODON
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
	public static boolean forwardReportDefault;
	public static AutoRevealMode autoRevealEqualSpoilers;
	public static boolean disableM3PillActiveIndicator;
	public static boolean showNavigationLabels;
	public static boolean displayPronounsInTimelines, displayPronounsInThreads, displayPronounsInUserListings;
	public static boolean overlayMedia;
	public static boolean showSuicideHelp;
	public static boolean underlinedLinks;
	public static ColorPreference color;
	public static boolean likeIcon;

	// MOSHIDON
	public static boolean showDividers;
	public static boolean relocatePublishButton;
	public static boolean defaultToUnlistedReplies;
	public static boolean doubleTapToSearch;
	public static boolean doubleTapToSwipe;
	public static boolean confirmBeforeReblog;
	public static boolean hapticFeedback;
	public static boolean replyLineAboveHeader;
	public static boolean swapBookmarkWithBoostAction;
	public static boolean loadRemoteAccountFollowers;
	public static boolean mentionRebloggerAutomatically;
	public static boolean showPostsWithoutAlt;
	public static boolean showMediaPreview;
	public static boolean disableRightToLeftCharacter;

	public static SharedPreferences getPrefs(){
		return MastodonApp.context.getSharedPreferences("global", Context.MODE_PRIVATE);
	}

	public static <T> T fromJson(String json, Type type, T orElse){
		if(json==null) return orElse;
		try{
			T value=gson.fromJson(json, type);
			return value==null ? orElse : value;
		}catch(JsonSyntaxException ignored){
			return orElse;
		}
	}

	public static <T extends Enum<T>> T enumValue(Class<T> enumType, String name) {
		try { return Enum.valueOf(enumType, name); }
		catch (NullPointerException npe) { return null; }
	}

	public static void load(){
		SharedPreferences prefs=getPrefs();

		playGifs=prefs.getBoolean("playGifs", true);
		useCustomTabs=prefs.getBoolean("useCustomTabs", true);
		theme=ThemePreference.values()[prefs.getInt("theme", 0)];
		altTextReminders=prefs.getBoolean("altTextReminders", true);
		confirmUnfollow=prefs.getBoolean("confirmUnfollow", true);
		confirmBoost=prefs.getBoolean("confirmBoost", false);
		confirmDeletePost=prefs.getBoolean("confirmDeletePost", true);

		// MEGALODON
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
		forwardReportDefault=prefs.getBoolean("forwardReportDefault", true);
		disableM3PillActiveIndicator=prefs.getBoolean("disableM3PillActiveIndicator", false);
		showNavigationLabels=prefs.getBoolean("showNavigationLabels", true);
		displayPronounsInTimelines=prefs.getBoolean("displayPronounsInTimelines", true);
		displayPronounsInThreads=prefs.getBoolean("displayPronounsInThreads", true);
		displayPronounsInUserListings=prefs.getBoolean("displayPronounsInUserListings", true);
		overlayMedia=prefs.getBoolean("overlayMedia", false);
		showSuicideHelp=prefs.getBoolean("showSuicideHelp", true);
		underlinedLinks=prefs.getBoolean("underlinedLinks", true);
		color=ColorPreference.valueOf(prefs.getString("color", MATERIAL3.name()));
		likeIcon=prefs.getBoolean("likeIcon", false);

		// MOSHIDON
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
		loadRemoteAccountFollowers=prefs.getBoolean("loadRemoteAccountFollowers", true);
		mentionRebloggerAutomatically=prefs.getBoolean("mentionRebloggerAutomatically", false);
		showPostsWithoutAlt=prefs.getBoolean("showPostsWithoutAlt", true);
		showMediaPreview=prefs.getBoolean("showMediaPreview", true);
		disableRightToLeftCharacter=prefs.getBoolean("disableRightToLeftCharacter", false);

		theme=ThemePreference.values()[prefs.getInt("theme", 0)];


		if (prefs.contains("prefixRepliesWithRe")) {
			prefixReplies = prefs.getBoolean("prefixRepliesWithRe", false)
					? PrefixRepliesMode.TO_OTHERS : PrefixRepliesMode.NEVER;
			prefs.edit()
					.putString("prefixReplies", prefixReplies.name())
					.remove("prefixRepliesWithRe")
					.apply();
		}

		int migrationLevel=prefs.getInt("migrationLevel", BuildConfig.VERSION_CODE);
		if(migrationLevel < 61)
			migrateToUpstreamVersion61();
		if(migrationLevel < BuildConfig.VERSION_CODE)
			prefs.edit().putInt("migrationLevel", BuildConfig.VERSION_CODE).apply();
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

				// MEGALODON
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
				.putBoolean("forwardReportDefault", forwardReportDefault)
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

				// MOSHIDON
				.putBoolean("defaultToUnlistedReplies", defaultToUnlistedReplies)
				.putBoolean("doubleTapToSearch", doubleTapToSearch)
				.putBoolean("doubleTapToSwipe", doubleTapToSwipe)
				.putBoolean("replyLineAboveHeader", replyLineAboveHeader)
				.putBoolean("confirmBeforeReblog", confirmBeforeReblog)
				.putBoolean("swapBookmarkWithBoostAction", swapBookmarkWithBoostAction)
				.putBoolean("loadRemoteAccountFollowers", loadRemoteAccountFollowers)
				.putBoolean("hapticFeedback", hapticFeedback)
				.putBoolean("mentionRebloggerAutomatically", mentionRebloggerAutomatically)
				.putBoolean("showDividers", showDividers)
				.putBoolean("relocatePublishButton", relocatePublishButton)
				.putBoolean("enableDeleteNotifications", enableDeleteNotifications)
				.putBoolean("showPostsWithoutAlt", showPostsWithoutAlt)
				.putBoolean("showMediaPreview", showMediaPreview)
				.putBoolean("disableRightToLeftCharacter", disableRightToLeftCharacter)

				.apply();
	}

	public enum ThemePreference{
		AUTO,
		LIGHT,
		DARK
	}

	public enum AutoRevealMode {
		NEVER,
		THREADS,
		DISCUSSIONS
	}

	public enum PrefixRepliesMode {
		NEVER,
		ALWAYS,
		TO_OTHERS
	}


	//region preferences migrations

	private static void migrateToUpstreamVersion61(){
		Log.d(TAG, "Migrating preferences to upstream version 61!!");

		Type accountsDefaultContentTypesType = new TypeToken<Map<String, ContentType>>() {}.getType();
		Type pinnedTimelinesType = new TypeToken<Map<String, ArrayList<TimelineDefinition>>>() {}.getType();
		Type recentLanguagesType = new TypeToken<Map<String, ArrayList<String>>>() {}.getType();

		// migrate global preferences
		SharedPreferences prefs=getPrefs();
		altTextReminders=!prefs.getBoolean("disableAltTextReminder", false);
		confirmBoost=prefs.getBoolean("confirmBeforeReblog", false);
		toolbarMarquee=!prefs.getBoolean("disableMarquee", false);

		save();

		// migrate local preferences
		AccountSessionManager asm=AccountSessionManager.getInstance();
		// reset: Set<String> accountsWithContentTypesEnabled=prefs.getStringSet("accountsWithContentTypesEnabled", new HashSet<>());
		Map<String, ContentType> accountsDefaultContentTypes=fromJson(prefs.getString("accountsDefaultContentTypes", null), accountsDefaultContentTypesType, new HashMap<>());
		Map<String, ArrayList<TimelineDefinition>> pinnedTimelines=fromJson(prefs.getString("pinnedTimelines", null), pinnedTimelinesType, new HashMap<>());
		Set<String> accountsWithLocalOnlySupport=prefs.getStringSet("accountsWithLocalOnlySupport", new HashSet<>());
		Set<String> accountsInGlitchMode=prefs.getStringSet("accountsInGlitchMode", new HashSet<>());
		Map<String, ArrayList<String>> recentLanguages=fromJson(prefs.getString("recentLanguages", null), recentLanguagesType, new HashMap<>());

		for(AccountSession session : asm.getLoggedInAccounts()){
			String accountID=session.getID();
			AccountLocalPreferences localPrefs=session.getLocalPreferences();
			localPrefs.revealCWs=prefs.getBoolean("alwaysExpandContentWarnings", false);
			localPrefs.recentLanguages=recentLanguages.get(accountID);
			// reset: localPrefs.contentTypesEnabled=accountsWithContentTypesEnabled.contains(accountID);
			localPrefs.defaultContentType=accountsDefaultContentTypes.getOrDefault(accountID, ContentType.PLAIN);
			localPrefs.showInteractionCounts=prefs.getBoolean("showInteractionCounts", false);
			localPrefs.timelines=pinnedTimelines.getOrDefault(accountID, TimelineDefinition.getDefaultTimelines(accountID));
			localPrefs.localOnlySupported=accountsWithLocalOnlySupport.contains(accountID);
			localPrefs.glitchInstance=accountsInGlitchMode.contains(accountID);
			localPrefs.publishButtonText=prefs.getString("publishButtonText", null);
			localPrefs.keepOnlyLatestNotification=prefs.getBoolean("keepOnlyLatestNotification", false);
			localPrefs.showReplies=prefs.getBoolean("showReplies", true);
			localPrefs.showBoosts=prefs.getBoolean("showBoosts", true);

			if(session.getInstance().map(Instance::isAkkoma).orElse(false)){
				localPrefs.timelineReplyVisibility=prefs.getString("replyVisibility", null);
			}

			localPrefs.save();
		}
	}

	//endregion

}
