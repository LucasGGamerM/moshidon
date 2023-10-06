package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.views.UsableRecyclerView;

public class MoshidonUsableRecyclerView extends UsableRecyclerView {

	public MoshidonUsableRecyclerView(Context context){
		super(context);
		overrideDefaultSelector();
	}

	public MoshidonUsableRecyclerView(Context context, AttributeSet attrs){
		super(context, attrs);
		overrideDefaultSelector();
	}

	public MoshidonUsableRecyclerView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		overrideDefaultSelector();
	}

	private void overrideDefaultSelector() {
		TypedArray ta = this.getContext().obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
		Drawable selectableDrawable = ta.getDrawable(0);
		ta.recycle();

		setSelector(new SelectorWrapper(selectableDrawable));
	}

	private class SelectorWrapper extends DrawableWrapper {

		public SelectorWrapper(@Nullable Drawable drawable){
			super(drawable);
		}

		private Runnable scheduledStateChange;

		private long pressedTimestamp = 0L;

		@Override
		protected boolean onStateChange(@NonNull int[] state){
			removeCallbacks(scheduledStateChange);

			if (state == PRESSED_ENABLED_FOCUSED_STATE_SET) {
				pressedTimestamp = System.currentTimeMillis();
				scheduledStateChange = () -> super.onStateChange(state);
				postDelayed(scheduledStateChange, 250L);
				return false;
			} else {
				if (System.currentTimeMillis() - pressedTimestamp <= ViewConfiguration.getTapTimeout()) {
					pressedTimestamp = 0L;
					super.onStateChange(PRESSED_ENABLED_FOCUSED_STATE_SET);
				}
				scheduledStateChange = () -> {};
				return super.onStateChange(state);
			}
		}
	}
}
