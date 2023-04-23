package org.joinmastodon.android.fragments.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.oauth.RevokeOauthToken;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.onboarding.InstanceRulesFragment;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;

public class AccountFragment extends SettingsBaseFragment{


    private SwitchItem glitchModeItem;
    @Override
    public void addItems(ArrayList<Item> items) {
        items.add(new HeaderItem(R.string.settings_account));
        items.add(new TextItem(R.string.sk_settings_profile, ()-> UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/settings/profile"), R.drawable.ic_fluent_open_24_regular));
        items.add(new TextItem(R.string.sk_settings_posting, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/settings/preferences/other"), R.drawable.ic_fluent_open_24_regular));
        items.add(new TextItem(R.string.sk_settings_filters, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/filters"), R.drawable.ic_fluent_open_24_regular));
        items.add(new TextItem(R.string.sk_settings_auth, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/auth/edit"), R.drawable.ic_fluent_open_24_regular));

        items.add(new HeaderItem(getInstanceName()));
        items.add(new TextItem(R.string.sk_settings_rules, ()->{
            Bundle args=new Bundle();
            args.putParcelable("instance", Parcels.wrap(getInstance()));
            Nav.go(getActivity(), InstanceRulesFragment.class, args);
        }, R.drawable.ic_fluent_task_list_ltr_24_regular));
        items.add(new TextItem(R.string.sk_settings_about_instance	, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/about"), R.drawable.ic_fluent_info_24_regular));
        items.add(new TextItem(R.string.settings_tos, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/terms"), R.drawable.ic_fluent_open_24_regular));
        items.add(new TextItem(R.string.settings_privacy_policy, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/terms"), R.drawable.ic_fluent_open_24_regular));
        items.add(new TextItem(R.string.log_out, this::confirmLogOut, R.drawable.ic_fluent_sign_out_24_regular));
        if (!TextUtils.isEmpty(getInstance().version)) items.add(new SmallTextItem(getString(R.string.sk_settings_server_version, getInstance().version)));

        items.add(new HeaderItem(R.string.sk_instance_features));
        items.add(new SwitchItem(R.string.sk_settings_support_local_only, 0, GlobalUserPreferences.accountsWithLocalOnlySupport.contains(accountID), i->{
            glitchModeItem.enabled = i.checked;
            if (i.checked) {
                GlobalUserPreferences.accountsWithLocalOnlySupport.add(accountID);
                if (getInstance().pleroma == null) GlobalUserPreferences.accountsInGlitchMode.add(accountID);
            } else {
                GlobalUserPreferences.accountsWithLocalOnlySupport.remove(accountID);
                GlobalUserPreferences.accountsInGlitchMode.remove(accountID);
            }
            glitchModeItem.checked = GlobalUserPreferences.accountsInGlitchMode.contains(accountID);
            if (list.findViewHolderForAdapterPosition(items.indexOf(glitchModeItem)) instanceof SwitchViewHolder svh) svh.rebind();
            GlobalUserPreferences.save();
        }));
        items.add(new SmallTextItem(getString(R.string.sk_settings_local_only_explanation)));
        items.add(glitchModeItem = new SwitchItem(R.string.sk_settings_glitch_instance, 0, GlobalUserPreferences.accountsInGlitchMode.contains(accountID), i->{
            if (i.checked) {
                GlobalUserPreferences.accountsInGlitchMode.add(accountID);
            } else {
                GlobalUserPreferences.accountsInGlitchMode.remove(accountID);
            }
            GlobalUserPreferences.save();
        }));
        glitchModeItem.enabled = GlobalUserPreferences.accountsWithLocalOnlySupport.contains(accountID);
        items.add(new SmallTextItem(getString(R.string.sk_settings_glitch_mode_explanation)));


        boolean translationAvailable = getInstance().v2 != null && getInstance().v2.configuration.translation != null && getInstance().v2.configuration.translation.enabled;
        items.add(new SmallTextItem(getString(translationAvailable ?
                R.string.sk_settings_translation_availability_note_available :
                R.string.sk_settings_translation_availability_note_unavailable, getInstance().title)));

    }

    private void confirmLogOut(){
        new M3AlertDialogBuilder(getActivity())
                .setTitle(R.string.log_out)
                .setMessage(R.string.confirm_log_out)
                .setPositiveButton(R.string.log_out, (dialog, which) -> logOut())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void logOut(){
        AccountSession session= AccountSessionManager.getInstance().getAccount(accountID);
        new RevokeOauthToken(session.app.clientId, session.app.clientSecret, session.token.accessToken)
                .setCallback(new Callback<>(){
                    @Override
                    public void onSuccess(Object result){
                        onLoggedOut();
                    }

                    @Override
                    public void onError(ErrorResponse error){
                        onLoggedOut();
                    }
                })
                .wrapProgress(getActivity(), R.string.loading, false)
                .exec(accountID);
    }

    private void onLoggedOut(){
        if (getActivity() == null) return;
        AccountSessionManager.getInstance().removeAccount(accountID);
        getActivity().finish();
        Intent intent=new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
    }
}
