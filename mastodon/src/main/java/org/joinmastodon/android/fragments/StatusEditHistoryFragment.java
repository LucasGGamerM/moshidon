package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetStatusEditHistory;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.DummyStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.InlineStatusStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ReblogOrReplyLineCustomStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ReblogOrReplyLineStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;
//import org.joinmastodon.android.ui.utils.InsetStatusItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import me.grishka.appkit.api.SimpleCallback;
import name.fraser.neil.plaintext.diff_match_patch;

public class StatusEditHistoryFragment extends StatusListFragment{
	private String id, url;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		id=getArguments().getString("id");
		url=getArguments().getString("url");
		loadData();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.edit_history);
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetStatusEditHistory(id)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						if(getActivity()==null) return;
						Collections.sort(result, Comparator.comparing((Status s)->s.createdAt).reversed());
						onDataLoaded(result, false);
					}
				})
				.exec(accountID);
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		List<StatusDisplayItem> items=new ArrayList<>();
		InlineStatusStatusDisplayItem inlineItem=new InlineStatusStatusDisplayItem(s.getID(), this, s, accountID);
		inlineItem.fullWidth=true;
		items.add(inlineItem);
		int idx=data.indexOf(s);
		if(idx>=0){
			String date=UiUtils.DATE_TIME_FORMATTER.format(s.createdAt.atZone(ZoneId.systemDefault()));
			String action="";
			if(idx==data.size()-1){
				action=getString(R.string.edit_original_post);
			}else{
				enum StatusEditChangeType{
					TEXT_CHANGED,
					SPOILER_ADDED,
					SPOILER_REMOVED,
					SPOILER_CHANGED,
					POLL_ADDED,
					POLL_REMOVED,
					POLL_CHANGED,
					MEDIA_ADDED,
					MEDIA_REMOVED,
					MEDIA_REORDERED,
					MARKED_SENSITIVE,
					MARKED_NOT_SENSITIVE
				}
				EnumSet<StatusEditChangeType> changes=EnumSet.noneOf(StatusEditChangeType.class);
				Status prev=data.get(idx+1);

				// if only formatting was changed, don't even try to create a diff text
				if(!Objects.equals(HtmlParser.text(s.content), HtmlParser.text(prev.content))){
					changes.add(StatusEditChangeType.TEXT_CHANGED);
					//update status content to display a diffs
					s.content=createDiffText(prev.content, s.content);
				}
				if(!Objects.equals(s.spoilerText, prev.spoilerText)){
					if(s.spoilerText==null){
						changes.add(StatusEditChangeType.SPOILER_REMOVED);
					}else if(prev.spoilerText==null){
						changes.add(StatusEditChangeType.SPOILER_ADDED);
					}else{
						changes.add(StatusEditChangeType.SPOILER_CHANGED);
					}
				}
				if(s.poll!=null || prev.poll!=null){
					if(s.poll==null){
						changes.add(StatusEditChangeType.POLL_REMOVED);
					}else if(prev.poll==null){
						changes.add(StatusEditChangeType.POLL_ADDED);
					}else if(!s.poll.id.equals(prev.poll.id)){
						changes.add(StatusEditChangeType.POLL_CHANGED);
					}
				}
				List<String> newAttachmentIDs=s.mediaAttachments.stream().map(att->att.id).collect(Collectors.toList());
				List<String> prevAttachmentIDs=s.mediaAttachments.stream().map(att->att.id).collect(Collectors.toList());
				boolean addedOrRemoved=false;
				if(!newAttachmentIDs.containsAll(prevAttachmentIDs)){
					changes.add(StatusEditChangeType.MEDIA_REMOVED);
					addedOrRemoved=true;
				}
				if(!prevAttachmentIDs.containsAll(newAttachmentIDs)){
					changes.add(StatusEditChangeType.MEDIA_ADDED);
					addedOrRemoved=true;
				}
				if(!addedOrRemoved && !newAttachmentIDs.equals(prevAttachmentIDs)){
					changes.add(StatusEditChangeType.MEDIA_REORDERED);
				}
				if(s.sensitive && !prev.sensitive){
					changes.add(StatusEditChangeType.MARKED_SENSITIVE);
				}else if(prev.sensitive && !s.sensitive){
					changes.add(StatusEditChangeType.MARKED_NOT_SENSITIVE);
				}

				if(changes.size()==1){
					action=getString(switch(changes.iterator().next()){
						case TEXT_CHANGED -> R.string.edit_text_edited;
						case SPOILER_ADDED -> R.string.edit_spoiler_added;
						case SPOILER_REMOVED -> R.string.edit_spoiler_removed;
						case SPOILER_CHANGED -> R.string.edit_spoiler_edited;
						case POLL_ADDED -> R.string.edit_poll_added;
						case POLL_REMOVED -> R.string.edit_poll_removed;
						case POLL_CHANGED -> R.string.edit_poll_edited;
						case MEDIA_ADDED -> R.string.edit_media_added;
						case MEDIA_REMOVED -> R.string.edit_media_removed;
						case MEDIA_REORDERED -> R.string.edit_media_reordered;
						case MARKED_SENSITIVE -> R.string.edit_marked_sensitive;
						case MARKED_NOT_SENSITIVE -> R.string.edit_marked_not_sensitive;
					});
				}else{
					action=getString(R.string.edit_multiple_changed);
				}
			}
			String sep = getString(R.string.sk_separator);
			items.add(0, new ReblogOrReplyLineCustomStatusDisplayItem(s.id, this, action+" "+sep+" "+date, Collections.emptyList(), 0));
			items.add(1, new DummyStatusDisplayItem(s.id, this));
		}
		items.addAll(StatusDisplayItem.buildItems(this, s, accountID, s, knownAccounts, null, StatusDisplayItem.FLAG_NO_FOOTER|StatusDisplayItem.FLAG_INSET|StatusDisplayItem.FLAG_NO_EMOJI_REACTIONS));
		return items;
	}

	@Override
	public boolean isItemEnabled(String id){
		return false;
	}

	@Override
	protected FilterContext getFilterContext() {
		return null;
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return Uri.parse(url);
	}

	private String createDiffText(String original, String modified) {
		diff_match_patch dmp=new diff_match_patch();
		LinkedList<diff_match_patch.Diff> diffs=dmp.diff_main(original, modified);
		dmp.diff_cleanupSemantic(diffs);

		StringBuilder stringBuilder=new StringBuilder();
		for(diff_match_patch.Diff diff : diffs){
			switch(diff.operation){
				case DELETE->{
					stringBuilder.append("<edit-diff-delete>");
					stringBuilder.append(diff.text);
					stringBuilder.append("</edit-diff-delete>");
				}
				case INSERT->{
					stringBuilder.append("<edit-diff-insert>");
					stringBuilder.append(diff.text);
					stringBuilder.append("</edit-diff-insert>");
				}
				default->stringBuilder.append(diff.text);
			}
		}
		return stringBuilder.toString();
	}
}
