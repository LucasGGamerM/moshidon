package org.joinmastodon.android.ui.sheets;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountByID;
import org.joinmastodon.android.api.requests.statuses.PleromaGetStatusReactions;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.EmojiReaction;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

/**
 * Displays the accounts that used each emoji reaction on a status.
 *
 * Hollo exposes account_ids in the Mastodon-compatible status response, so
 * account information is resolved with the regular accounts endpoint.
 */
public class EmojiReactionDetailsSheet extends BottomSheet{
	private final Activity activity;
	private final String accountID;
	private final String statusID;
	private final List<EmojiReaction> reactions;
	private final boolean pleromaFallback;
	private final LinearLayout tabs;
	private final RecyclerView accountsList;
	private final View loading, empty;
	private final AccountsAdapter accountsAdapter;
	private int selectedReaction;
	private int loadGeneration;

	public EmojiReactionDetailsSheet(@NonNull Activity activity, String accountID, String statusID,
			List<EmojiReaction> reactions, int initiallySelectedReaction, boolean pleromaFallback){
		super(activity);
		this.activity=activity;
		this.accountID=accountID;
		this.statusID=statusID;
		this.reactions=new ArrayList<>(reactions);
		this.pleromaFallback=pleromaFallback;
		selectedReaction=Math.max(0, Math.min(initiallySelectedReaction, Math.max(0, reactions.size()-1)));

		View content=activity.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_emoji_reaction_details, null);
		setContentView(content);
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(activity, R.attr.colorM3Surface),
				UiUtils.getThemeColor(activity, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());
		((TextView) content.findViewById(R.id.title)).setText(R.string.mo_emoji_reaction_details);
		tabs=content.findViewById(R.id.tabs);
		accountsList=content.findViewById(R.id.accounts);
		loading=content.findViewById(R.id.loading);
		empty=content.findViewById(R.id.empty);
		accountsList.setLayoutManager(new LinearLayoutManager(activity));
		accountsList.setAdapter(accountsAdapter=new AccountsAdapter());
		ViewGroup.LayoutParams params=accountsList.getLayoutParams();
		params.height=(int) (activity.getResources().getDisplayMetrics().heightPixels*0.42f);
		accountsList.setLayoutParams(params);

		buildTabs();
		selectReaction(selectedReaction);
	}

	private void buildTabs(){
		for(int i=0;i<reactions.size();i++){
			final int index=i;
			EmojiReaction reaction=reactions.get(i);
			LinearLayout tab=new LinearLayout(activity);
			tab.setGravity(Gravity.CENTER);
			tab.setMinimumHeight(V.dp(40));
			tab.setPadding(V.dp(12), 0, V.dp(12), 0);
			tab.setBackgroundResource(R.drawable.bg_emoji_reaction_tab_selector);
			LinearLayout.LayoutParams tabParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, V.dp(40));
			tabParams.setMargins(V.dp(4), 0, V.dp(4), 0);
			tabs.addView(tab, tabParams);

			if(reaction.getUrl(GlobalUserPreferences.playGifs)!=null){
				ImageView image=new ImageView(activity);
				image.setContentDescription(reaction.name);
				tab.addView(image, new LinearLayout.LayoutParams(V.dp(24), V.dp(24)));
				ViewImageLoader.load(image, null, new UrlImageLoaderRequest(reaction.getUrl(GlobalUserPreferences.playGifs), V.dp(24), V.dp(24)));
			}else{
				TextView emoji=new TextView(activity);
				emoji.setText(reaction.name);
				emoji.setTextSize(18);
				tab.addView(emoji);
			}
			TextView count=new TextView(activity);
			count.setText(" "+UiUtils.abbreviateNumber(reaction.count));
			count.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
			tab.addView(count);
			tab.setOnClickListener(v->selectReaction(index));
		}
	}

	private void selectReaction(int index){
		selectedReaction=index;
		for(int i=0;i<tabs.getChildCount();i++)
			tabs.getChildAt(i).setSelected(i==index);
		accountsAdapter.setAccounts(Collections.emptyList());
		empty.setVisibility(View.GONE);
		loading.setVisibility(View.VISIBLE);
		int generation=++loadGeneration;
		EmojiReaction reaction=reactions.get(index);
		if(reaction.accounts!=null && !reaction.accounts.isEmpty()){
			showAccounts(generation, reaction.accounts);
			return;
		}
		if(reaction.accountIds!=null && !reaction.accountIds.isEmpty()){
			loadAccountsByID(generation, reaction.accountIds, 0, new ArrayList<>());
			return;
		}
		if(pleromaFallback){
			new PleromaGetStatusReactions(statusID, reaction.name)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(List<EmojiReaction> result){
							if(generation!=loadGeneration) return;
							if(result!=null && !result.isEmpty() && result.get(0).accounts!=null)
								showAccounts(generation, result.get(0).accounts);
							else
								showEmpty(generation);
						}

						@Override
						public void onError(ErrorResponse error){
							showEmpty(generation);
						}
					})
					.exec(accountID);
		}else{
			showEmpty(generation);
		}
	}

	private void loadAccountsByID(int generation, List<String> ids, int index, List<Account> loaded){
		if(generation!=loadGeneration) return;
		if(index>=ids.size()){
			showAccounts(generation, loaded);
			return;
		}
		new GetAccountByID(ids.get(index))
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						if(generation!=loadGeneration) return;
						loaded.add(result);
						loadAccountsByID(generation, ids, index+1, loaded);
					}

					@Override
					public void onError(ErrorResponse error){
						if(generation!=loadGeneration) return;
						loadAccountsByID(generation, ids, index+1, loaded);
					}
				})
				.exec(accountID);
	}

	private void showAccounts(int generation, List<Account> accounts){
		if(generation!=loadGeneration) return;
		loading.setVisibility(View.GONE);
		empty.setVisibility(accounts.isEmpty() ? View.VISIBLE : View.GONE);
		accountsAdapter.setAccounts(accounts);
	}

	private void showEmpty(int generation){
		showAccounts(generation, Collections.emptyList());
	}

	private class AccountsAdapter extends RecyclerView.Adapter<AccountViewHolder>{
		private List<Account> accounts=Collections.emptyList();

		void setAccounts(List<Account> accounts){
			this.accounts=new ArrayList<>(accounts);
			notifyDataSetChanged();
		}

		@NonNull
		@Override
		public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new AccountViewHolder(activity.getLayoutInflater().inflate(R.layout.item_emoji_reaction_account, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull AccountViewHolder holder, int position){
			holder.bind(accounts.get(position));
		}

		@Override
		public int getItemCount(){
			return accounts.size();
		}
	}

	private class AccountViewHolder extends RecyclerView.ViewHolder{
		private final ImageView avatar;
		private final TextView name, username;
		private Account account;

		AccountViewHolder(View itemView){
			super(itemView);
			avatar=itemView.findViewById(R.id.avatar);
			name=itemView.findViewById(R.id.name);
			username=itemView.findViewById(R.id.username);
			View.OnClickListener listener=v->openProfile();
			itemView.setOnClickListener(listener);
			username.setOnClickListener(listener);
		}

		void bind(Account account){
			this.account=account;
			name.setText(HtmlParser.parseCustomEmoji(account.getDisplayName(), account.emojis));
			UiUtils.loadCustomEmojiInTextView(name);
			username.setText(account.getDisplayUsername());
			String avatarUrl=GlobalUserPreferences.playGifs ? account.avatar : account.avatarStatic;
			if(TextUtils.isEmpty(avatarUrl)) avatarUrl=account.avatar;
			if(TextUtils.isEmpty(avatarUrl)){
				avatar.setImageResource(R.drawable.image_placeholder);
			}else{
				ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(avatarUrl, V.dp(44), V.dp(44)));
			}
		}

		private void openProfile(){
			if(account==null) return;
			UiUtils.openProfileByID(activity, accountID, account.id);
			dismiss();
		}
	}
}
