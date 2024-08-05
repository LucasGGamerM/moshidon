package org.joinmastodon.android;

import android.app.Fragment;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.ui.sheets.AccountSwitcherSheet;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.jsoup.internal.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import androidx.annotation.Nullable;
import me.grishka.appkit.FragmentStackActivity;

public class ExternalShareActivity extends FragmentStackActivity{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		UiUtils.setUserPreferredTheme(this);
		super.onCreate(savedInstanceState);
		if(savedInstanceState==null){
			Optional<String> text = Optional.ofNullable(getIntent().getStringExtra(Intent.EXTRA_TEXT));
			Optional<Pair<String, Optional<String>>> fediHandle = text.flatMap(UiUtils::parseFediverseHandle);
			boolean isFediUrl = text.map(UiUtils::looksLikeFediverseUrl).orElse(false);
			boolean isOpenable = isFediUrl || fediHandle.isPresent();

			List<AccountSession> sessions=AccountSessionManager.getInstance().getLoggedInAccounts();
			if (sessions.isEmpty()){
				Toast.makeText(this, R.string.err_not_logged_in, Toast.LENGTH_SHORT).show();
				finish();
			} else if (isOpenable || sessions.size() > 1) {
				AccountSwitcherSheet sheet = new AccountSwitcherSheet(this, null, R.drawable.ic_fluent_share_28_regular,
						isOpenable
								? R.string.sk_external_share_or_open_title
								: R.string.sk_external_share_title,
						null, isOpenable);
				sheet.setOnClick((accountId, open) -> {
					if (open && text.isPresent()) {
						BiConsumer<Class<? extends Fragment>, Bundle> callback = (clazz, args) -> {
							if (clazz == null) {
								Toast.makeText(this, R.string.sk_open_in_app_failed, Toast.LENGTH_SHORT).show();
								// TODO: do something about the window getting leaked
								sheet.dismiss();
								finish();
								return;
							}
							args.putString("fromExternalShare", clazz.getSimpleName());
							Intent intent = new Intent(this, MainActivity.class);
							intent.putExtras(args);
							finish();
							startActivity(intent);
						};

						fediHandle
								.<MastodonAPIRequest<?>>map(handle ->
										UiUtils.lookupAccountHandle(this, accountId, handle, callback))
								.or(() ->
										UiUtils.lookupURL(this, accountId, text.get(), callback))
								.ifPresent(req ->
										req.wrapProgress(this, R.string.loading, true, d -> {
											UiUtils.transformDialogForLookup(this, accountId, isFediUrl ? text.get() : null, d);
											d.setOnDismissListener((x) -> finish());
										}));
					} else {
						openComposeFragment(accountId);
					}
				});
				sheet.show();
			} else if (sessions.size() == 1) {
				openComposeFragment(sessions.get(0).getID());
			}
		}
	}

	private void openComposeFragment(String accountID){
		AccountSession session=AccountSessionManager.get(accountID);
		UiUtils.setUserPreferredTheme(this, session);
		getWindow().setBackgroundDrawable(null);

		Intent intent=getIntent();
		StringBuilder builder=new StringBuilder();
		String subject = "";
		if (intent.hasExtra(Intent.EXTRA_SUBJECT)) {
			subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
			if (!StringUtil.isBlank(subject)) builder.append(subject).append("\n\n");
		}
		if (intent.hasExtra(Intent.EXTRA_TEXT)) {
			String extra = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (!StringUtil.isBlank(extra)) {
				if (extra.startsWith(subject)) extra = extra.substring(subject.length()).trim();
				builder.append(extra).append("\n\n");
			}
		}
		String text=builder.toString();
		List<Uri> mediaUris;
		if(Intent.ACTION_SEND.equals(intent.getAction())){
			Uri singleUri=intent.getParcelableExtra(Intent.EXTRA_STREAM);
			mediaUris=singleUri!=null ? Collections.singletonList(singleUri) : null;
		}else if(Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())){
			ClipData clipData=intent.getClipData();
			if(clipData!=null){
				mediaUris=new ArrayList<>(clipData.getItemCount());
				for(int i=0;i<clipData.getItemCount();i++){
					ClipData.Item item=clipData.getItemAt(i);
					mediaUris.add(item.getUri());
				}
			}else{
				mediaUris=null;
			}
		}else{
			Toast.makeText(this, "Unexpected intent action: "+intent.getAction(), Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		Bundle args=new Bundle();
		args.putString("account", accountID);
		if(!TextUtils.isEmpty(text))
			args.putString("prefilledText", text);
		args.putInt("selectionStart", StringUtil.isBlank(subject) ? 0 : subject.length());
		if(mediaUris!=null && !mediaUris.isEmpty())
			args.putParcelableArrayList("mediaAttachments", toArrayList(mediaUris));
		Fragment fragment=new ComposeFragment();
		fragment.setArguments(args);
		showFragmentClearingBackStack(fragment);
	}

	private static <T> ArrayList<T> toArrayList(List<T> l){
		if(l instanceof ArrayList)
			return (ArrayList<T>) l;
		if(l==null)
			return null;
		return new ArrayList<>(l);
	}
}
