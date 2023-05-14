package org.joinmastodon.android;

import android.app.Fragment;
import android.app.assist.AssistContent;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import org.joinmastodon.android.api.mastodon.session.AccountSession;
import org.joinmastodon.android.api.mastodon.session.AccountSessionManager;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.ui.AccountSwitcherSheet;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.jsoup.internal.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import me.grishka.appkit.FragmentStackActivity;

public class ExternalShareActivity extends FragmentStackActivity{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		UiUtils.setUserPreferredTheme(this);
		super.onCreate(savedInstanceState);
		if(savedInstanceState==null){

			String text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
			boolean isMastodonURL = UiUtils.looksLikeMastodonUrl(text);

			List<AccountSession> sessions=AccountSessionManager.getInstance().getLoggedInAccounts();
			if(sessions.isEmpty()){
				Toast.makeText(this, R.string.err_not_logged_in, Toast.LENGTH_SHORT).show();
				finish();
			}else if(sessions.size()==1 && !isMastodonURL){
				openComposeFragment(sessions.get(0).getID());
			}else{
				new AccountSwitcherSheet(this, false, false, isMastodonURL, accountSession -> {
					if(accountSession!=null)
						openComposeFragment(accountSession.getID());
					else
						UiUtils.openURL(this, AccountSessionManager.getInstance().getLastActiveAccountID(), text);
				}).show();
			}
		}
	}

	private void openComposeFragment(String accountID){
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

	@Override
	public void onProvideAssistContent(AssistContent outContent) {
		super.onProvideAssistContent(outContent);

		outContent.setWebUri(Uri.parse(DomainManager.getInstance().getCurrentDomain()));
	}
}
