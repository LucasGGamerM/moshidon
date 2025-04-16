package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import me.grishka.appkit.views.UsableRecyclerView;

public class EmojiReactionsRecyclerView extends UsableRecyclerView{
	public EmojiReactionsRecyclerView(Context context){
		super(context);
	}

	public EmojiReactionsRecyclerView(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	public EmojiReactionsRecyclerView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e){
		super.onTouchEvent(e);
		// to pass through touch events (i.e. clicking the status) to the parent view
		return false;
	}

	// https://stackoverflow.com/questions/55372837/is-there-a-way-to-make-recyclerview-requiresfadingedge-unaffected-by-paddingtop
	@Override
	protected boolean isPaddingOffsetRequired() {
		return true;
	}

	@Override
	protected int getLeftPaddingOffset(){
		return -getPaddingLeft();
	}

	@Override
	protected int getRightPaddingOffset() {
		return getPaddingRight();
	}
}
