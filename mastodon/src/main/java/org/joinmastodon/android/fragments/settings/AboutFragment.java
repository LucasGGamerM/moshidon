package org.joinmastodon.android.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.LruCache;
import android.widget.Toast;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.requests.oauth.RevokeOauthToken;
import org.joinmastodon.android.api.session.AccountActivationInfo;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.onboarding.AccountActivationFragment;
import org.joinmastodon.android.fragments.onboarding.InstanceRulesFragment;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;
import org.parceler.Parcels;

import java.util.ArrayList;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageCache;

public class AboutFragment extends SettingsBaseFragment{

    private TextItem checkForUpdateItem, clearImageCacheItem;
    private ImageCache imageCache;
    @Override
    public void addItems(ArrayList<Item> items) {
        items.add(new HeaderItem(R.string.sk_settings_about));

        items.add(new TextItem(R.string.mo_settings_contribute, ()->UiUtils.launchWebBrowser(getActivity(), "https://github.com/LucasGGamerM/moshidon"), R.drawable.ic_fluent_open_24_regular));
        items.add(new TextItem(R.string.sk_settings_donate, ()->UiUtils.launchWebBrowser(getActivity(), "https://github.com/sponsors/LucasGGamerM"), R.drawable.ic_fluent_heart_24_regular));

        if (GithubSelfUpdater.needSelfUpdating()) {
            checkForUpdateItem = new TextItem(R.string.sk_check_for_update, GithubSelfUpdater.getInstance()::checkForUpdates);
            items.add(checkForUpdateItem);
            items.add(new SwitchItem(R.string.sk_updater_enable_pre_releases, 0, GlobalUserPreferences.enablePreReleases, i->{
                GlobalUserPreferences.enablePreReleases=i.checked;
                GlobalUserPreferences.save();
            }));
        }

        LruCache<?, ?> cache = imageCache == null ? null : imageCache.getLruCache();
        clearImageCacheItem = new TextItem(R.string.settings_clear_cache, UiUtils.formatFileSize(getContext(), cache != null ? cache.size() : 0, true), this::clearImageCache, 0);
        items.add(clearImageCacheItem);
        items.add(new TextItem(R.string.sk_clear_recent_languages, ()->UiUtils.showConfirmationAlert(getActivity(), R.string.sk_clear_recent_languages, R.string.sk_confirm_clear_recent_languages, R.string.clear, ()->{
            GlobalUserPreferences.recentLanguages.remove(accountID);
            GlobalUserPreferences.save();
        })));

        items.add(new TextItem(R.string.mo_clear_recent_emoji, ()-> {
            GlobalUserPreferences.recentEmojis.clear();
            GlobalUserPreferences.save();
        }));

        if(BuildConfig.DEBUG){
            items.add(new RedHeaderItem("Debug options"));

            items.add(new TextItem("Test E-Mail confirmation flow", ()->{
                AccountSession sess=AccountSessionManager.getInstance().getAccount(accountID);
                sess.activated=false;
                sess.activationInfo=new AccountActivationInfo("test@email", System.currentTimeMillis());
                Bundle args=new Bundle();
                args.putString("account", accountID);
                args.putBoolean("debug", true);
                Nav.goClearingStack(getActivity(), AccountActivationFragment.class, args);
            }));

            items.add(new TextItem("Copy preferences", ()->{
                StringBuilder prefBuilder = new StringBuilder();
                GlobalUserPreferences.load();
                GlobalUserPreferences.getPrefs().getAll().forEach((key, value) -> prefBuilder.append(key).append(": ").append(value).append('\n'));
                UiUtils.copyText(view, prefBuilder.toString());
            }));

            items.add(new TextItem("Reset preferences", ()->{
                GlobalUserPreferences.load();
                GlobalUserPreferences.getPrefs().edit().clear().commit();
                UiUtils.restartApp();
            }, R.drawable.ic_fluent_warning_24_regular));

            items.add(new TextItem("Open App Info", () ->
                            getContext().startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", getContext().getPackageName(), null))),
                            R.drawable.ic_fluent_open_24_regular
                    )
            );

            items.add(new TextItem("Open developer settings",
                    ()-> getContext().startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)),
                    R.drawable.ic_fluent_open_24_regular)
            );
        }

        String version = getContext().getString(R.string.mo_settings_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        items.add(new FooterItem(version, () -> UiUtils.copyText(view, version)));
    }

    private void clearImageCache(){
        MastodonAPIController.runInBackground(()->{
            Activity activity=getActivity();
            imageCache.clear();
            Toast.makeText(activity, R.string.media_cache_cleared, Toast.LENGTH_SHORT).show();
        });
        if (list.findViewHolderForAdapterPosition(items.indexOf(clearImageCacheItem)) instanceof TextViewHolder tvh) {
            clearImageCacheItem.secondaryText = UiUtils.formatFileSize(getContext(), 0, true);
            tvh.rebind();
        }
    }
}
