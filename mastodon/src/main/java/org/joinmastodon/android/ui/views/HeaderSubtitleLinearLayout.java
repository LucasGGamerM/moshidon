package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;

/**
 * A LinearLayout for TextViews. First child TextView will get truncated if it doesn't fit, remaining will always wrap content.
 */
public class HeaderSubtitleLinearLayout extends LinearLayout{
	private float firstFraction;

	public HeaderSubtitleLinearLayout(Context context){
		this(context, null);
	}

	public HeaderSubtitleLinearLayout(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public HeaderSubtitleLinearLayout(Context context, AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
		TypedArray ta=context.obtainStyledAttributes(attrs, R.styleable.HeaderSubtitleLinearLayout);
		firstFraction=ta.getFraction(R.styleable.HeaderSubtitleLinearLayout_firstFraction, 1, 1, 0.5f);
		ta.recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		if(getLayoutChildCount()>1){
			int fullWidth=MeasureSpec.getSize(widthMeasureSpec);
			int remainingWidth=fullWidth;
			for(int i=1;i<getChildCount();i++){
				View v=getChildAt(i);
				if(v.getVisibility()==GONE)
					continue;
				v.measure(MeasureSpec.getSize(widthMeasureSpec) | MeasureSpec.AT_MOST, heightMeasureSpec);
				LayoutParams lp=(LayoutParams) v.getLayoutParams();
				remainingWidth-=v.getMeasuredWidth()+lp.leftMargin+lp.rightMargin;
			}
			View first=getChildAt(0);
			if(first instanceof TextView){
				((TextView) first).setMaxWidth(Math.max(remainingWidth, (int)(firstFraction*fullWidth)));
			}
		}else{
			View first=getChildAt(0);
			if(first instanceof TextView){
				((TextView) first).setMaxWidth(Integer.MAX_VALUE);
			}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	private int getLayoutChildCount(){
		int count=0;
		for(int i=0;i<getChildCount();i++){
			if(getChildAt(i).getVisibility()!=GONE)
				count++;
		}
		return count;
	}

	public void setFirstFraction(float firstFraction){
		this.firstFraction=firstFraction;
	}

	public float getFirstFraction(){
		return firstFraction;
	}
}
