package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Space;

import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.fragments.BaseStatusListFragment;

import me.grishka.appkit.utils.V;

public class DummyStatusDisplayItem extends StatusDisplayItem {

	public DummyStatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment) {
		super(parentID, parentFragment);
	}

	@Override
	public Type getType() {
		return Type.DUMMY;
	}

	public static class Holder extends StatusDisplayItem.Holder<DummyStatusDisplayItem> {
		private final RecyclerView.LayoutParams params;

		public Holder(Context context) {
			super(new Space(context));
			params=new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);

			// BetterItemAnimator appears not to handle InsetStatusItemDecoration's getItemOffsets
			// correctly, causing removed inset views to jump while animating. i don't quite
			// understand it, but this workaround appears to work.
			// see InsetStatusItemDecoration#getItemOffsets
			params.setMargins(0, 0, 0, V.dp(16));
			itemView.setLayoutParams(params);
		}

		@Override
		public void onBind(DummyStatusDisplayItem item) {}
	}
}
