package com.hootsuite.nachos.chip;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import org.joinmastodon.android.R;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A Span that displays text and an optional icon inside of a material design chip. The chip's dimensions, colors etc. can be extensively customized
 * through the various setter methods available in this class.
 *     The basic structure of the chip is the following:
 * For chips with the icon on right:
 * <pre>
 *
 *                                  (chip vertical spacing / 2)
 *                  ----------------------------------------------------------
 *                |                                                            |
 * (left margin)  |  (padding edge)   text   (padding between image)   icon    |   (right margin)
 *                |                                                            |
 *                  ----------------------------------------------------------
 *                                  (chip vertical spacing / 2)
 *
 *      </pre>
 * For chips with the icon on the left (see {@link #setShowIconOnLeft(boolean)}):
 * <pre>
 *
 *                                  (chip vertical spacing / 2)
 *                  ----------------------------------------------------------
 *                |                                                            |
 * (left margin)  |   icon  (padding between image)   text   (padding edge)    |   (right margin)
 *                |                                                            |
 *                  ----------------------------------------------------------
 *                                  (chip vertical spacing / 2)
 *     </pre>
 */
public class ChipSpan extends ImageSpan implements Chip {

    private static final float SCALE_PERCENT_OF_CHIP_HEIGHT = 0.70f;
    private static final boolean ICON_ON_LEFT_DEFAULT = true;

    private int[] mStateSet = new int[]{};

    private String mEllipsis;

    private ColorStateList mDefaultBackgroundColor;
    private ColorStateList mBackgroundColor;
    private int mTextColor;
    private int mCornerRadius = -1;
    private int mIconBackgroundColor;

    private int mTextSize = -1;
    private int mPaddingEdgePx;
    private int mPaddingBetweenImagePx;
    private int mLeftMarginPx;
    private int mRightMarginPx;
    private int mMaxAvailableWidth = -1;

    private CharSequence mText;
    private String mTextToDraw;

    private Drawable mIcon;
    private boolean mShowIconOnLeft = ICON_ON_LEFT_DEFAULT;

    private int mChipVerticalSpacing = 0;
    private int mChipHeight = -1;
    private int mChipWidth = -1;
    private int mIconWidth;

    private int mCachedSize = -1;

    private Object mData;

    /**
     * Constructs a new ChipSpan.
     *
     * @param context a {@link Context} that will be used to retrieve default configurations from resource files
     * @param text    the text for the ChipSpan to display
     * @param icon    an optional icon (can be {@code null}) for the ChipSpan to display
     */
    public ChipSpan(@NonNull Context context, @NonNull CharSequence text, @Nullable Drawable icon, Object data) {
        super(icon);
        mIcon = icon;
        mText = text;
        mTextToDraw = mText.toString();

        mEllipsis = context.getString(R.string.chip_ellipsis);

        mDefaultBackgroundColor = context.getColorStateList(R.color.chip_material_background);
        mBackgroundColor = mDefaultBackgroundColor;

        mTextColor = context.getColor(R.color.chip_default_text_color);
        mIconBackgroundColor = context.getColor(R.color.chip_default_icon_background_color);

        Resources resources = context.getResources();
        mPaddingEdgePx = resources.getDimensionPixelSize(R.dimen.chip_default_padding_edge);
        mPaddingBetweenImagePx = resources.getDimensionPixelSize(R.dimen.chip_default_padding_between_image);
        mLeftMarginPx = resources.getDimensionPixelSize(R.dimen.chip_default_left_margin);
        mRightMarginPx = resources.getDimensionPixelSize(R.dimen.chip_default_right_margin);

        mData = data;
    }

    /**
     * Copy constructor to recreate a ChipSpan from an existing one
     *
     * @param context  a {@link Context} that will be used to retrieve default configurations from resource files
     * @param chipSpan the ChipSpan to copy
     */
    public ChipSpan(@NonNull Context context, @NonNull ChipSpan chipSpan) {
        this(context, chipSpan.getText(), chipSpan.getDrawable(), chipSpan.getData());

        mDefaultBackgroundColor = chipSpan.mDefaultBackgroundColor;
        mTextColor = chipSpan.mTextColor;
        mIconBackgroundColor = chipSpan.mIconBackgroundColor;
        mCornerRadius = chipSpan.mCornerRadius;

        mTextSize = chipSpan.mTextSize;
        mPaddingEdgePx = chipSpan.mPaddingEdgePx;
        mPaddingBetweenImagePx = chipSpan.mPaddingBetweenImagePx;
        mLeftMarginPx = chipSpan.mLeftMarginPx;
        mRightMarginPx = chipSpan.mRightMarginPx;
        mMaxAvailableWidth = chipSpan.mMaxAvailableWidth;

        mShowIconOnLeft = chipSpan.mShowIconOnLeft;

        mChipVerticalSpacing = chipSpan.mChipVerticalSpacing;
        mChipHeight = chipSpan.mChipHeight;

        mStateSet = chipSpan.mStateSet;
    }

    @Override
    public Object getData() {
        return mData;
    }

    /**
     * Sets the height of the chip. This height should not include any extra spacing (for extra vertical spacing call {@link #setChipVerticalSpacing(int)}).
     * The background of the chip will fill the full height provided here. If this method is never called, the chip will have the height of one full line
     * of text by default. If {@code -1} is passed here, the chip will revert to this default behavior.
     *
     * @param chipHeight the height to set in pixels
     */
    public void setChipHeight(int chipHeight) {
        mChipHeight = chipHeight;
    }

    /**
     * Sets the vertical spacing to include in between chips. Half of the value set here will be placed as empty space above the chip and half the value
     * will be placed as empty space below the chip. Therefore chips on consecutive lines will have the full value as vertical space in between them.
     * This spacing is achieved by adjusting the font metrics used by the text view containing these chips; however it does not come into effect until
     * at least one chip is created. Note that vertical spacing is dependent on having a fixed chip height (set in {@link #setChipHeight(int)}). If a
     * height is not specified in that method, the value set here will be ignored.
     *
     * @param chipVerticalSpacing the vertical spacing to set in pixels
     */
    public void setChipVerticalSpacing(int chipVerticalSpacing) {
        mChipVerticalSpacing = chipVerticalSpacing;
    }

    /**
     * Sets the font size for the chip's text. If this method is never called, the chip text will have the same font size as the text in the TextView
     * containing this chip by default. If {@code -1} is passed here, the chip will revert to this default behavior.
     *
     * @param size the font size to set in pixels
     */
    public void setTextSize(int size) {
        mTextSize = size;
        invalidateCachedSize();
    }

    /**
     * Sets the color for the chip's text.
     *
     * @param color the color to set (as a hexadecimal number in the form 0xAARRGGBB)
     */
    public void setTextColor(int color) {
        mTextColor = color;
    }

    /**
     * Sets where the icon (if an icon was provided in the constructor) will appear.
     *
     * @param showIconOnLeft if true, the icon will appear on the left, otherwise the icon will appear on the right
     */
    public void setShowIconOnLeft(boolean showIconOnLeft) {
        this.mShowIconOnLeft = showIconOnLeft;
        invalidateCachedSize();
    }

    /**
     * Sets the left margin. This margin will appear as empty space (it will not share the chip's background color) to the left of the chip.
     *
     * @param leftMarginPx the left margin to set in pixels
     */
    public void setLeftMargin(int leftMarginPx) {
        mLeftMarginPx = leftMarginPx;
        invalidateCachedSize();
    }

    /**
     * Sets the right margin. This margin will appear as empty space (it will not share the chip's background color) to the right of the chip.
     *
     * @param rightMarginPx the right margin to set in pixels
     */
    public void setRightMargin(int rightMarginPx) {
        this.mRightMarginPx = rightMarginPx;
        invalidateCachedSize();
    }

    /**
     * Sets the background color. To configure which color in the {@link ColorStateList} is shown you can call {@link #setState(int[])}.
     * Passing {@code null} here will cause the chip to revert to it's default background.
     *
     * @param backgroundColor a {@link ColorStateList} containing backgrounds for different states.
     * @see #setState(int[])
     */
    public void setBackgroundColor(@Nullable ColorStateList backgroundColor) {
        mBackgroundColor = backgroundColor != null ? backgroundColor : mDefaultBackgroundColor;
    }

    /**
     * Sets the chip background corner radius.
     *
     * @param cornerRadius The corner radius value, in pixels.
     */
    public void setCornerRadius(@Dimension int cornerRadius) {
        mCornerRadius = cornerRadius;
    }

    /**
     * Sets the icon background color. This is the color of the circle that gets drawn behind the icon passed to the
     * {@link #ChipSpan(Context, CharSequence, Drawable, Object)}  constructor}
     *
     * @param iconBackgroundColor the icon background color to set (as a hexadecimal number in the form 0xAARRGGBB)
     */
    public void setIconBackgroundColor(int iconBackgroundColor) {
        mIconBackgroundColor = iconBackgroundColor;
    }

    public void setMaxAvailableWidth(int maxAvailableWidth) {
        mMaxAvailableWidth = maxAvailableWidth;
        invalidateCachedSize();
    }

    /**
     * Sets the UI state. This state will be reflected in the background color drawn for the chip.
     *
     * @param stateSet one of the state constants in {@link android.view.View}
     * @see #setBackgroundColor(ColorStateList)
     */
    @Override
    public void setState(int[] stateSet) {
        this.mStateSet = stateSet != null ? stateSet : new int[]{};
    }

    @Override
    public CharSequence getText() {
        return mText;
    }

    @Override
    public int getWidth() {
        // If we haven't actually calculated a chip width yet just return -1, otherwise return the chip width + margins
        return mChipWidth != -1 ? (mLeftMarginPx + mChipWidth + mRightMarginPx) : -1;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        boolean usingFontMetrics = (fm != null);

        // Adjust the font metrics regardless of whether or not there is a cached size so that the text view can maintain its height
        if (usingFontMetrics) {
            adjustFontMetrics(paint, fm);
        }

        if (mCachedSize == -1 && usingFontMetrics) {
            mIconWidth = (mIcon != null) ? calculateChipHeight(fm.top, fm.bottom) : 0;

            int actualWidth = calculateActualWidth(paint);
            mCachedSize = actualWidth;

            if (mMaxAvailableWidth != -1) {
                int maxAvailableWidthMinusMargins = mMaxAvailableWidth - mLeftMarginPx - mRightMarginPx;
                if (actualWidth > maxAvailableWidthMinusMargins) {
                    mTextToDraw = mText + mEllipsis;

                    while ((calculateActualWidth(paint) > maxAvailableWidthMinusMargins) && mTextToDraw.length() > 0) {
                        int lastCharacterIndex = mTextToDraw.length() - mEllipsis.length() - 1;
                        if (lastCharacterIndex < 0) {
                            break;
                        }
                        mTextToDraw = mTextToDraw.substring(0, lastCharacterIndex) + mEllipsis;
                    }

                    // Avoid a negative width
                    mChipWidth = Math.max(0, maxAvailableWidthMinusMargins);
                    mCachedSize = mMaxAvailableWidth;
                }
            }
        }

        return mCachedSize;
    }

    private int calculateActualWidth(Paint paint) {
        // Only change the text size if a text size was set
        if (mTextSize != -1) {
            paint.setTextSize(mTextSize);
        }

        int totalPadding = mPaddingEdgePx;

        // Find text width
        Rect bounds = new Rect();
        paint.getTextBounds(mTextToDraw, 0, mTextToDraw.length(), bounds);
        int textWidth = bounds.width();

        if (mIcon != null) {
            totalPadding += mPaddingBetweenImagePx;
        } else {
            totalPadding += mPaddingEdgePx;
        }

        mChipWidth = totalPadding + textWidth + mIconWidth;
        return getWidth();
    }

    public void invalidateCachedSize() {
        mCachedSize = -1;
    }

    /**
     * Adjusts the provided font metrics to make it seem like the font takes up {@code mChipHeight + mChipVerticalSpacing} pixels in height.
     * This effectively ensures that the TextView will have a height equal to {@code mChipHeight + mChipVerticalSpacing} + whatever padding it has set.
     * In {@link #draw(Canvas, CharSequence, int, int, float, int, int, int, Paint)} the chip itself is drawn to that it is vertically centered with
     * {@code mChipVerticalSpacing / 2} pixels of space above and below it
     *
     * @param paint the paint whose font metrics should be adjusted
     * @param fm    the font metrics object to populate through {@link Paint#getFontMetricsInt(Paint.FontMetricsInt)}
     */
    private void adjustFontMetrics(Paint paint, Paint.FontMetricsInt fm) {
        // Only actually adjust font metrics if we have a chip height set
        if (mChipHeight != -1) {
            paint.getFontMetricsInt(fm);
            int textHeight = fm.descent - fm.ascent;
            // Break up the vertical spacing in half because half will go above the chip, half will go below the chip
            int halfSpacing = mChipVerticalSpacing / 2;

            // Given that the text is centered vertically within the chip, the amount of space above or below the text (inbetween the text and chip)
            // is half their difference in height:
            int spaceBetweenChipAndText = (mChipHeight - textHeight) / 2;

            int textTop = fm.top;
            int chipTop = fm.top - spaceBetweenChipAndText;

            int textBottom = fm.bottom;
            int chipBottom = fm.bottom + spaceBetweenChipAndText;

            // The text may have been taller to begin with so we take the most negative coordinate (highest up) to be the top of the content
            int topOfContent = Math.min(textTop, chipTop);
            // Same as above but we want the largest positive coordinate (lowest down) to be the bottom of the content
            int bottomOfContent = Math.max(textBottom, chipBottom);

            // Shift the top up by halfSpacing and the bottom down by halfSpacing
            int topOfContentWithSpacing = topOfContent - halfSpacing;
            int bottomOfContentWithSpacing = bottomOfContent + halfSpacing;

            // Change the font metrics so that the TextView thinks the font takes up the vertical space of a chip + spacing
            fm.ascent = topOfContentWithSpacing;
            fm.descent = bottomOfContentWithSpacing;
            fm.top = topOfContentWithSpacing;
            fm.bottom = bottomOfContentWithSpacing;
        }
    }

    private int calculateChipHeight(int top, int bottom) {
        // If a chip height was set we can return that, otherwise calculate it from top and bottom
        return mChipHeight != -1 ? mChipHeight : bottom - top;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        // Shift everything mLeftMarginPx to the left to create an empty space on the left (creating the margin)
        x += mLeftMarginPx;
        if (mChipHeight != -1) {
            // If we set a chip height, adjust to vertically center chip in the line
            // Adding (bottom - top) / 2 shifts the chip down so the top of it will be centered vertically
            // Subtracting (mChipHeight / 2) shifts the chip back up so that the center of it will be centered vertically (as desired)
            top += ((bottom - top) / 2) - (mChipHeight / 2);
            bottom = top + mChipHeight;
        }

        // Perform actual drawing
        drawBackground(canvas, x, top, bottom, paint);
        drawText(canvas, x, top, bottom, paint, mTextToDraw);
        if (mIcon != null) {
            drawIcon(canvas, x, top, bottom, paint);
        }
    }

    private void drawBackground(Canvas canvas, float x, int top, int bottom, Paint paint) {
        int backgroundColor = mBackgroundColor.getColorForState(mStateSet, mBackgroundColor.getDefaultColor());
        paint.setColor(backgroundColor);
        int height = calculateChipHeight(top, bottom);
        RectF rect = new RectF(x, top, x + mChipWidth, bottom);
        int cornerRadius = (mCornerRadius != -1) ? mCornerRadius : height / 2;
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
        paint.setColor(mTextColor);
    }

    private void drawText(Canvas canvas, float x, int top, int bottom, Paint paint, CharSequence text) {
        if (mTextSize != -1) {
            paint.setTextSize(mTextSize);
        }
        int height = calculateChipHeight(top, bottom);
        Paint.FontMetrics fm = paint.getFontMetrics();

        // The top value provided here is the y coordinate for the very top of the chip
        // The y coordinate we are calculating is where the baseline of the text will be drawn
        // Our objective is to have the midpoint between the top and baseline of the text be in line with the vertical center of the chip
        // First we add height / 2 which will put the baseline at the vertical center of the chip
        // Then we add half the height of the text which will lower baseline so that the midpoint is at the vertical center of the chip as desired
        float adjustedY = top + ((height / 2) + ((-fm.top - fm.bottom) / 2));

        // The x coordinate provided here is the left-most edge of the chip
        // If there is no icon or the icon is on the right, then the text will start at the left-most edge, but indented with the edge padding, so we
        // add mPaddingEdgePx
        // If there is an icon and it's on the left, the text will start at the left-most edge, but indented by the combined width of the icon and
        // the padding between the icon and text, so we add (mIconWidth + mPaddingBetweenImagePx)
        float adjustedX = x + ((mIcon == null || !mShowIconOnLeft) ? mPaddingEdgePx : (mIconWidth + mPaddingBetweenImagePx));

        canvas.drawText(text, 0, text.length(), adjustedX, adjustedY, paint);
    }

    private void drawIcon(Canvas canvas, float x, int top, int bottom, Paint paint) {
        drawIconBackground(canvas, x, top, bottom, paint);
        drawIconBitmap(canvas, x, top, bottom, paint);
    }

    private void drawIconBackground(Canvas canvas, float x, int top, int bottom, Paint paint) {
        int height = calculateChipHeight(top, bottom);

        paint.setColor(mIconBackgroundColor);

        // Since it's a circle the diameter is equal to the height, so the radius == diameter / 2 == height / 2
        int radius = height / 2;
        // The coordinates that get passed to drawCircle are for the center of the circle
        // x is the left edge of the chip, (x + mChipWidth) is the right edge of the chip
        // So the center of the circle is one radius distance from either the left or right edge (depending on which side the icon is being drawn on)
        float circleX = mShowIconOnLeft ? (x + radius) : (x + mChipWidth - radius);
        // The y coordinate is always just one radius distance from the top
        canvas.drawCircle(circleX, top + radius, radius, paint);

        paint.setColor(mTextColor);
    }

    private void drawIconBitmap(Canvas canvas, float x, int top, int bottom, Paint paint) {
        int height = calculateChipHeight(top, bottom);

        // Create a scaled down version of the bitmap to fit within the circle (whose diameter == height)
        Bitmap iconBitmap = Bitmap.createBitmap(mIcon.getIntrinsicWidth(), mIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Bitmap scaledIconBitMap = scaleDown(iconBitmap, (float) height * SCALE_PERCENT_OF_CHIP_HEIGHT, true);
        iconBitmap.recycle();
        Canvas bitmapCanvas = new Canvas(scaledIconBitMap);
        mIcon.setBounds(0, 0, bitmapCanvas.getWidth(), bitmapCanvas.getHeight());
        mIcon.draw(bitmapCanvas);

        // We are drawing a square icon inside of a circle
        // The coordinates we pass to canvas.drawBitmap have to be for the top-left corner of the bitmap
        // The bitmap should be inset by half of (circle width - bitmap width)
        // Since it's a circle, the circle's width is equal to it's height which is equal to the chip height
        float xInsetWithinCircle = (height - bitmapCanvas.getWidth()) / 2;

        // The icon x coordinate is going to be insetWithinCircle pixels away from the left edge of the circle
        // If the icon is on the left, the left edge of the circle is just x
        // If the icon is on the right, the left edge of the circle is x + mChipWidth - height
        float iconX = mShowIconOnLeft ? (x + xInsetWithinCircle) : (x + mChipWidth - height + xInsetWithinCircle);

        // The y coordinate works the same way (only it's always from the top edge)
        float yInsetWithinCircle = (height - bitmapCanvas.getHeight()) / 2;
        float iconY = top + yInsetWithinCircle;

        canvas.drawBitmap(scaledIconBitMap, iconX, iconY, paint);
    }

    private Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        float ratio = Math.min(maxImageSize / realImage.getWidth(), maxImageSize / realImage.getHeight());
        int width = Math.round(ratio * realImage.getWidth());
        int height = Math.round(ratio * realImage.getHeight());
        return Bitmap.createScaledBitmap(realImage, width, height, filter);
    }

    @Override
    public String toString() {
        return mText.toString();
    }
}
