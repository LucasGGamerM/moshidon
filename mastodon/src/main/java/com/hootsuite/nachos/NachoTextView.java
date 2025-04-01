package com.hootsuite.nachos;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Editable;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListAdapter;
import android.widget.MultiAutoCompleteTextView;

import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.chip.ChipInfo;
import com.hootsuite.nachos.chip.ChipSpan;
import com.hootsuite.nachos.chip.ChipSpanChipCreator;
import com.hootsuite.nachos.terminator.ChipTerminatorHandler;
import com.hootsuite.nachos.terminator.DefaultChipTerminatorHandler;
import com.hootsuite.nachos.tokenizer.ChipTokenizer;
import com.hootsuite.nachos.tokenizer.SpanChipTokenizer;
import com.hootsuite.nachos.validator.ChipifyingNachoValidator;
import com.hootsuite.nachos.validator.IllegalCharacterIdentifier;
import com.hootsuite.nachos.validator.NachoValidator;

import org.joinmastodon.android.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An editable TextView extending {@link MultiAutoCompleteTextView} that supports "chipifying" pieces of text and displaying suggestions for segments of the text.
 * <h1>The ChipTokenizer</h1>
 *     To customize chipifying with this class you can provide a custom {@link ChipTokenizer} by calling {@link #setChipTokenizer(ChipTokenizer)}.
 *     By default the {@link SpanChipTokenizer} is used.
 * <h1>Chip Terminators</h1>
 *     To set which characters trigger the creation of a chip, call {@link #addChipTerminator(char, int)} or {@link #setChipTerminators(Map)}.
 *     For example if tapping enter should cause all unchipped text to become chipped, call
 *     {@code chipSuggestionTextView.addChipTerminator('\n', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_ALL);}
 *     To completely customize how chips are created when text is entered in this text view you can provide a custom {@link ChipTerminatorHandler}
 *     through {@link #setChipTerminatorHandler(ChipTerminatorHandler)}
 * <h1>Illegal Characters</h1>
 *     To prevent a character from being typed you can call {@link #setIllegalCharacterIdentifier(IllegalCharacterIdentifier)}} to identify characters
 *     that should be considered illegal.
 * <h1>Suggestions</h1>
 *     To provide suggestions you must provide an {@link Adapter} by calling {@link #setAdapter(ListAdapter)}
 * <h1>UI Customization</h1>
 *     This view defines six custom attributes (all of which are optional):
 *     <ul>
 *         <li>chipHorizontalSpacing - the horizontal space between chips</li>
 *         <li>chipBackground - the background color of the chip</li>
 *         <li>chipCornerRadius - the corner radius of the chip background</li>
 *         <li>chipTextColor - the color of the chip text</li>
 *         <li>chipTextSize - the font size of the chip text</li>
 *         <li>chipHeight - the height of a single chip</li>
 *         <li>chipVerticalSpacing - the vertical space between chips on consecutive lines
 *             <ul>
 *                 <li>Note: chipVerticalSpacing is only used if a chipHeight is also set</li>
 *             </ul>
 *         </li>
 *     </ul>
 *     The values of these attributes will be passed to the ChipTokenizer through {@link ChipTokenizer#applyConfiguration(Editable, ChipConfiguration)}
 * <h1>Validation</h1>
 *     This class can perform validation when certain events occur (such as losing focus). When the validation occurs is decided by
 *     {@link AutoCompleteTextView}. To perform validation, set a {@link NachoValidator}:
 *     <pre>
 *         nachoTextView.setNachoValidator(new ChipifyingNachoValidator());
 *     </pre>
 *     Note: The NachoValidator will be ignored if a ChipTokenizer is not set. To perform validation without a ChipTokenizer you can use
 *     {@link AutoCompleteTextView}'s built-in {@link Validator Validator} through {@link #setValidator(Validator)}
 * <h1>Editing Chips</h1>
 *     This class also supports editing chips on touch. To enable this behavior call {@link #enableEditChipOnTouch(boolean, boolean)}. To disable this
 *     behavior you can call {@link #disableEditChipOnTouch()}
 * <h1>Example Setup:</h1>
 *     A standard setup for this class could look something like the following:
 *     <pre>
 *         String[] suggestions = new String[]{"suggestion 1", "suggestion 2"};
 *         ArrayAdapter&lt;String&gt; adapter = new ArrayAdapter&lt;&gt;(this, android.R.layout.simple_dropdown_item_1line, suggestions);
 *         nachoTextView.setAdapter(adapter);
 *         nachoTextView.addChipTerminator('\n', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_ALL);
 *         nachoTextView.addChipTerminator(' ', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_TO_TERMINATOR);
 *         nachoTextView.setIllegalCharacters('@');
 *         nachoTextView.setNachoValidator(new ChipifyingNachoValidator());
 *         nachoTextView.enableEditChipOnTouch(true, true);
 *         nachoTextView.setOnChipClickListener(new NachoTextView.OnChipClickListener() {
 *            {@literal @Override}
 *             public void onChipClick(Chip chip, MotionEvent motionEvent) {
 *                 // Handle click event
 *             }
 *         });
 *         nachoTextView.setOnChipRemoveListener(new NachoTextView.OnChipRemoveListener() {
 *            {@literal @Override}
 *             public void onChipRemove(Chip chip) {
 *                 // Handle remove event
 *             }
 *         });
 *     </pre>
 *
 * @see SpanChipTokenizer
 * @see DefaultChipTerminatorHandler
 * @see ChipifyingNachoValidator
 */
public class NachoTextView extends MultiAutoCompleteTextView implements TextWatcher, AdapterView.OnItemClickListener {

    // UI Attributes
    private int mChipHorizontalSpacing = -1;
    private ColorStateList mChipBackground = null;
    private int mChipCornerRadius = -1;
    private int mChipTextColor = Color.TRANSPARENT;
    private int mChipTextSize = -1;
    private int mChipHeight = -1;
    private int mChipVerticalSpacing = -1;

    private int mDefaultPaddingTop = 0;
    private int mDefaultPaddingBottom = 0;
    /**
     * Flag to keep track of the padding state so we only update the padding when necessary
     */
    private boolean mUsingDefaultPadding = true;

    // Touch events
    @Nullable
    private OnChipClickListener mOnChipClickListener;
    private GestureDetector singleTapDetector;
    private boolean mEditChipOnTouchEnabled;
    private boolean mMoveChipToEndOnEdit;
    private boolean mChipifyUnterminatedTokensOnEdit;

    // Text entry
    @Nullable
    private ChipTokenizer mChipTokenizer;
    @Nullable
    private ChipTerminatorHandler mChipTerminatorHandler;
    @Nullable
    private NachoValidator mNachoValidator;
    @Nullable
    private IllegalCharacterIdentifier illegalCharacterIdentifier;

    @Nullable
    private OnChipRemoveListener mOnChipRemoveListener;
    private List<Chip> mChipsToRemove = new ArrayList<>();
    private boolean mIgnoreTextChangedEvents;
    private int mTextChangedStart;
    private int mTextChangedEnd;
    private boolean mIsPasteEvent;

    // Measurement
    private boolean mMeasured;

    // Layout
    private boolean mLayoutComplete;

    public NachoTextView(Context context) {
        super(context);
        init(null);
    }

    public NachoTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public NachoTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        Context context = getContext();

        if (attrs != null) {
            TypedArray attributes = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.NachoTextView,
                    0,
                    R.style.DefaultChipSuggestionTextView);

            try {
                mChipHorizontalSpacing = attributes.getDimensionPixelSize(R.styleable.NachoTextView_chipHorizontalSpacing, -1);
                mChipBackground = attributes.getColorStateList(R.styleable.NachoTextView_chipBackground);
                mChipCornerRadius = attributes.getDimensionPixelSize(R.styleable.NachoTextView_chipCornerRadius, -1);
                mChipTextColor = attributes.getColor(R.styleable.NachoTextView_chipTextColor, Color.TRANSPARENT);
                mChipTextSize = attributes.getDimensionPixelSize(R.styleable.NachoTextView_chipTextSize, -1);
                mChipHeight = attributes.getDimensionPixelSize(R.styleable.NachoTextView_chipHeight, -1);
                mChipVerticalSpacing = attributes.getDimensionPixelSize(R.styleable.NachoTextView_chipVerticalSpacing, -1);
            } finally {
                attributes.recycle();
            }
        }

        mDefaultPaddingTop = getPaddingTop();
        mDefaultPaddingBottom = getPaddingBottom();

        singleTapDetector = new GestureDetector(getContext(), new SingleTapListener());

        setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
        addTextChangedListener(this);
        setChipTokenizer(new SpanChipTokenizer<>(context, new ChipSpanChipCreator(), ChipSpan.class));
        setChipTerminatorHandler(new DefaultChipTerminatorHandler());
        setOnItemClickListener(this);

        updatePadding();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mMeasured && getWidth() > 0) {
            // Refresh the tokenizer for width changes
            invalidateChips();
            mMeasured = true;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (!mLayoutComplete) {
            invalidateChips();
            mLayoutComplete = true;
        }
    }

    /**
     * Updates the padding based on whether or not any chips are present to avoid the view from changing heights when chips are inserted/deleted.
     * Extra padding is added when there are no chips. When there are chips the padding is reverted to its defaults. This only affects top and bottom
     * padding because the chips only affect the height of the view.
     */
    private void updatePadding() {
        if (mChipHeight != -1) {
            boolean chipsArePresent = !getAllChips().isEmpty();
            if (!chipsArePresent && mUsingDefaultPadding) {
                mUsingDefaultPadding = false;
                Paint paint = getPaint();
                Paint.FontMetricsInt fm = paint.getFontMetricsInt();
                int textHeight = fm.descent - fm.ascent;
                // Calculate how tall the view should be if there were chips
                int newTextHeight = mChipHeight + (mChipVerticalSpacing != -1 ? mChipVerticalSpacing : 0);
                // We need to add half our missing height above and below the text by increasing top and bottom padding
                int paddingAdjustment = (newTextHeight - textHeight) / 2;
                super.setPadding(getPaddingLeft(), mDefaultPaddingTop + paddingAdjustment, getPaddingRight(), mDefaultPaddingBottom + paddingAdjustment);
            } else if (chipsArePresent && !mUsingDefaultPadding) {
                // If there are chips we can revert to default padding
                mUsingDefaultPadding = true;
                super.setPadding(getPaddingLeft(), mDefaultPaddingTop, getPaddingRight(), mDefaultPaddingBottom);
            }
        }
    }

    /**
     * Sets the padding on this View. The left and right padding will be handled as they normally would in a TextView. The top and bottom padding passed
     * here will be the padding that is used when there are one or more chips in the text view. When there are no chips present, the padding will be
     * increased to make sure the overall height of the text view stays the same, since chips take up more vertical space than plain text.
     *
     * @param left   the left padding in pixels
     * @param top    the top padding in pixels
     * @param right  the right padding in pixels
     * @param bottom the bottom padding in pixels
     */
    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        // Call the super method so that left and right padding are updated
        // top and bottom padding will be handled in updatePadding()
        super.setPadding(left, top, right, bottom);
        mDefaultPaddingTop = top;
        mDefaultPaddingBottom = bottom;
        updatePadding();
    }

    public int getChipHorizontalSpacing() {
        return mChipHorizontalSpacing;
    }

    public void setChipHorizontalSpacing(@DimenRes int chipHorizontalSpacingResId) {
        mChipHorizontalSpacing = getContext().getResources().getDimensionPixelSize(chipHorizontalSpacingResId);
        invalidateChips();
    }

    public ColorStateList getChipBackground() {
        return mChipBackground;
    }

    public void setChipBackgroundResource(@ColorRes int chipBackgroundResId) {
        setChipBackground(getContext().getColorStateList(chipBackgroundResId));
    }

    public void setChipBackground(ColorStateList chipBackground) {
        mChipBackground = chipBackground;
        invalidateChips();
    }

    /**
     * @return The chip background corner radius value, in pixels.
     */
    @Dimension
    public int getChipCornerRadius() {
        return mChipCornerRadius;
    }

    /**
     * Sets the chip background corner radius.
     *
     * @param chipCornerRadiusResId The dimension resource with the corner radius value.
     */
    public void setChipCornerRadiusResource(@DimenRes int chipCornerRadiusResId) {
        setChipCornerRadius(getContext().getResources().getDimensionPixelSize(chipCornerRadiusResId));
    }

    /**
     * Sets the chip background corner radius.
     *
     * @param chipCornerRadius The corner radius value, in pixels.
     */
    public void setChipCornerRadius(@Dimension int chipCornerRadius) {
        mChipCornerRadius = chipCornerRadius;
        invalidateChips();
    }


    public int getChipTextColor() {
        return mChipTextColor;
    }

    public void setChipTextColorResource(@ColorRes int chipTextColorResId) {
        setChipTextColor(getContext().getColor(chipTextColorResId));
    }

    public void setChipTextColor(@ColorInt int chipTextColor) {
        mChipTextColor = chipTextColor;
        invalidateChips();
    }

    public int getChipTextSize() {
        return mChipTextSize;
    }

    public void setChipTextSize(@DimenRes int chipTextSizeResId) {
        mChipTextSize = getContext().getResources().getDimensionPixelSize(chipTextSizeResId);
        invalidateChips();
    }

    public int getChipHeight() {
        return mChipHeight;
    }

    public void setChipHeight(@DimenRes int chipHeightResId) {
        mChipHeight = getContext().getResources().getDimensionPixelSize(chipHeightResId);
        invalidateChips();
    }

    public int getChipVerticalSpacing() {
        return mChipVerticalSpacing;
    }

    public void setChipVerticalSpacing(@DimenRes int chipVerticalSpacingResId) {
        mChipVerticalSpacing = getContext().getResources().getDimensionPixelSize(chipVerticalSpacingResId);
        invalidateChips();
    }

    @Nullable
    public ChipTokenizer getChipTokenizer() {
        return mChipTokenizer;
    }

    /**
     * Sets the {@link ChipTokenizer} to be used by this ChipSuggestionTextView.
     * Note that a Tokenizer set here will override any Tokenizer set by {@link #setTokenizer(Tokenizer)}
     *
     * @param chipTokenizer the {@link ChipTokenizer} to set
     */
    public void setChipTokenizer(@Nullable ChipTokenizer chipTokenizer) {
        mChipTokenizer = chipTokenizer;
        if (mChipTokenizer != null) {
            setTokenizer(new ChipTokenizerWrapper(mChipTokenizer));
        } else {
            setTokenizer(null);
        }
        invalidateChips();
    }

    public void setOnChipClickListener(@Nullable OnChipClickListener onChipClickListener) {
        mOnChipClickListener = onChipClickListener;
    }

    public void setOnChipRemoveListener(@Nullable OnChipRemoveListener onChipRemoveListener) {
        mOnChipRemoveListener = onChipRemoveListener;
    }

    public void setChipTerminatorHandler(@Nullable ChipTerminatorHandler chipTerminatorHandler) {
        mChipTerminatorHandler = chipTerminatorHandler;
    }

    public void setNachoValidator(@Nullable NachoValidator nachoValidator) {
        mNachoValidator = nachoValidator;
    }

    /**
     * @see ChipTerminatorHandler#setChipTerminators(Map)
     */
    public void setChipTerminators(@Nullable Map<Character, Integer> chipTerminators) {
        if (mChipTerminatorHandler != null) {
            mChipTerminatorHandler.setChipTerminators(chipTerminators);
        }
    }

    /**
     * @see ChipTerminatorHandler#addChipTerminator(char, int)
     */
    public void addChipTerminator(char character, int behavior) {
        if (mChipTerminatorHandler != null) {
            mChipTerminatorHandler.addChipTerminator(character, behavior);
        }
    }

    /**
     * @see ChipTerminatorHandler#setPasteBehavior(int)
     */
    public void setPasteBehavior(int pasteBehavior) {
        if (mChipTerminatorHandler != null) {
            mChipTerminatorHandler.setPasteBehavior(pasteBehavior);
        }
    }

    /**
     * Sets the {@link IllegalCharacterIdentifier} that will identify characters that should
     * not show up in the field when typed (i.e. they will be deleted as soon as they are entered).
     * If a character is listed as both a chip terminator character and an illegal character,
     * it will be treated as an illegal character.
     *
     * @param illegalCharacterIdentifier the identifier to use
     */
    public void setIllegalCharacterIdentifier(@Nullable IllegalCharacterIdentifier illegalCharacterIdentifier) {
        this.illegalCharacterIdentifier = illegalCharacterIdentifier;
    }

    /**
     * Applies any updated configuration parameters to any existing chips and all future chips in the text view.
     *
     * @see ChipTokenizer#applyConfiguration(Editable, ChipConfiguration)
     */
    public void invalidateChips() {
        beginUnwatchedTextChange();

        if (mChipTokenizer != null) {
            Editable text = getText();
            int availableWidth = getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
            ChipConfiguration configuration = new ChipConfiguration(
                    mChipHorizontalSpacing,
                    mChipBackground,
                    mChipCornerRadius,
                    mChipTextColor,
                    mChipTextSize,
                    mChipHeight,
                    mChipVerticalSpacing,
                    availableWidth);

            mChipTokenizer.applyConfiguration(text, configuration);
        }

        endUnwatchedTextChange();
    }

    /**
     * Enables editing chips on touch events. When a touch event occurs, the touched chip will be put in editing mode. To later disable this behavior
     * call {@link #disableEditChipOnTouch()}.
     * <p>
     *     Note: If an {@link OnChipClickListener} is set it's behavior will override the behavior described here if it's
     *     {@link OnChipClickListener#onChipClick(Chip, MotionEvent)} method returns true. If that method returns false, the touched chip will be put
     *     in editing mode as expected.
     * </p>
     *
     * @param moveChipToEnd             if true, the chip will also be moved to the end of the text when it is put in editing mode
     * @param chipifyUnterminatedTokens if true, all unterminated tokens will be chipified before the touched chip is put in editing mode
     * @see #disableEditChipOnTouch()
     */
    public void enableEditChipOnTouch(boolean moveChipToEnd, boolean chipifyUnterminatedTokens) {
        mEditChipOnTouchEnabled = true;
        mMoveChipToEndOnEdit = moveChipToEnd;
        mChipifyUnterminatedTokensOnEdit = chipifyUnterminatedTokens;
    }

    /**
     * Disables editing chips on touch events. To re-enable this behavior call {@link #enableEditChipOnTouch(boolean, boolean)}.
     *
     * @see #enableEditChipOnTouch(boolean, boolean)
     */
    public void disableEditChipOnTouch() {
        mEditChipOnTouchEnabled = false;
    }

    /**
     * Puts the provided Chip in editing mode (i.e. reverts it to an unchipified token whose text can be edited).
     *
     * @param chip          the chip to edit
     * @param moveChipToEnd if true, the chip will also be moved to the end of the text
     */
    public void setEditingChip(Chip chip, boolean moveChipToEnd) {
        if (mChipTokenizer == null) {
            return;
        }

        beginUnwatchedTextChange();

        Editable text = getText();
        if (moveChipToEnd) {
            // Move the chip text to the end of the text
            text.append(chip.getText());
            // Delete the existing chip
            mChipTokenizer.deleteChipAndPadding(chip, text);
            // Move the cursor to the end of the text
            setSelection(text.length());
        } else {
            int chipStart = mChipTokenizer.findChipStart(chip, text);
            mChipTokenizer.revertChipToToken(chip, text);
            setSelection(mChipTokenizer.findTokenEnd(text, chipStart));
        }

        endUnwatchedTextChange();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        boolean wasHandled = false;
        clearChipStates();
        Chip touchedChip = findTouchedChip(event);
        if (touchedChip != null && isFocused() && singleTapDetector.onTouchEvent(event)) {
            touchedChip.setState(View.PRESSED_SELECTED_STATE_SET);
            if (onChipClicked(touchedChip)) {
                wasHandled = true;
            }
            if (mOnChipClickListener != null) {
                mOnChipClickListener.onChipClick(touchedChip, event);
            }
        }

        // Getting NullPointerException inside Editor.updateFloatingToolbarVisibility (Editor.java:1520)
        // primarily seen in Samsung Nougat devices.
        boolean superOnTouch = false;
        try {
            superOnTouch = super.onTouchEvent(event);
        } catch (NullPointerException e) {
            Log.w("Nacho", String.format("Error during touch event of type [%d]", event.getAction()), e);
            // can't handle or reproduce, but will monitor the error
        }

        return wasHandled || superOnTouch;
    }

    @Nullable
    private Chip findTouchedChip(MotionEvent event) {
        if (mChipTokenizer == null) {
            return null;
        }

        Editable text = getText();
        int offset = getOffsetForPosition(event.getX(), event.getY());
        List<Chip> chips = getAllChips();
        for (Chip chip : chips) {
            int chipStart = mChipTokenizer.findChipStart(chip, text);
            int chipEnd = mChipTokenizer.findChipEnd(chip, text); // This is actually the index of the character just past the end of the chip
            // When a touch event occurs getOffsetForPosition will either return the index of the first character of the span or the index of the
            // character one past the end of the span
            // This matches up perfectly with chipStart and chipEnd so we can just directly compare them...
            if (chipStart <= offset && offset <= chipEnd) {
                float startX = getXForIndex(chipStart);
                float endX = getXForIndex(chipEnd - 1);
                float eventX = event.getX();
                // ... however, when comparing the x coordinate we need to use (chipEnd - 1) because chipEnd will give us the x coordinate of the
                // beginning of the next span since that is actually what chipEnd holds. We want the x coordinate of the end of the current span so
                // we use (chipEnd - 1)
                if (startX <= eventX && eventX <= endX) {
                    return chip;
                }
            }
        }
        return null;
    }

    /**
     * Implement this method to handle chip clicked events.
     *
     * @param chip the chip that was clicked
     * @return true if the event was handled, otherwise false
     */
    public boolean onChipClicked(Chip chip) {
        boolean wasHandled = false;
        if (mEditChipOnTouchEnabled) {
            if (mChipifyUnterminatedTokensOnEdit) {
                chipifyAllUnterminatedTokens();
            }
            setEditingChip(chip, mMoveChipToEndOnEdit);
            wasHandled = true;
        }
        return wasHandled;
    }

    private float getXForIndex(int index) {
        Layout layout = getLayout();
        return layout.getPrimaryHorizontal(index);
    }

    private void clearChipStates() {
        for (Chip chip : getAllChips()) {
            chip.setState(View.EMPTY_STATE_SET);
        }
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        switch (id) {
            case android.R.id.cut:
                try {
                    setClipboardData(ClipData.newPlainText(null, getTextWithPlainTextSpans(start, end)));
                } catch (StringIndexOutOfBoundsException e) {
                    throw new StringIndexOutOfBoundsException(
                            String.format(
                                    "%s \nError cutting text index [%s, %s] for text [%s] and substring [%s]",
                                    e.getMessage(),
                                    start,
                                    end,
                                    getText().toString(),
                                    getText().subSequence(start, end)));
                }
                getText().delete(getSelectionStart(), getSelectionEnd());
                return true;
            case android.R.id.copy:
                try {
                    setClipboardData(ClipData.newPlainText(null, getTextWithPlainTextSpans(start, end)));
                } catch (StringIndexOutOfBoundsException e) {
                    throw new StringIndexOutOfBoundsException(
                            String.format(
                                    "%s \nError copying text index [%s, %s] for text [%s] and substring [%s]",
                                    e.getMessage(),
                                    start,
                                    end,
                                    getText().toString(),
                                    getText().subSequence(start, end)));
                }
                return true;
            case android.R.id.paste:
                mIsPasteEvent = true;
                boolean returnValue = super.onTextContextMenuItem(id);
                mIsPasteEvent = false;
                return returnValue;
            default:
                return super.onTextContextMenuItem(id);
        }
    }

    private void setClipboardData(ClipData clip) {
        ClipboardManager clipboard = (ClipboardManager) getContext().
                getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(clip);
    }

    /**
     * If a {@link Validator Validator} was set, this method will validate the entire text.
     * (Overrides the superclass method which only validates the current token)
     */
    @Override
    public void performValidation() {
        if (mNachoValidator == null || mChipTokenizer == null) {
            super.performValidation();
            return;
        }

        CharSequence text = getText();
        if (!TextUtils.isEmpty(text) && !mNachoValidator.isValid(mChipTokenizer, text)) {
            setRawText(mNachoValidator.fixText(mChipTokenizer, text));
        }
    }

    /**
     * From the point this method is called to when {@link #endUnwatchedTextChange()} is called, all TextChanged events will be ignored
     */
    private void beginUnwatchedTextChange() {
        mIgnoreTextChangedEvents = true;
    }

    /**
     * After this method is called TextChanged events will resume being handled.
     * This method also calls {@link #updatePadding()} in case the unwatched changed created/destroyed chips
     */
    private void endUnwatchedTextChange() {
        updatePadding();
        mIgnoreTextChangedEvents = false;
    }

    /**
     * Sets the contents of this text view without performing any processing (nothing will be chipified, no characters will be removed etc.)
     *
     * @param text the text to set
     */
    private void setRawText(CharSequence text) {
        beginUnwatchedTextChange();
        super.setText(text);
        endUnwatchedTextChange();
    }

    /**
     * Sets the contents of this text view to contain the provided list of strings. The text view will be cleared then each string in the list will
     * be chipified and appended to the text.
     *
     * @param chipValues the list of strings to chipify and set as the contents of the text view or null to clear the text view
     */
    public void setText(@Nullable List<String> chipValues) {
        if (mChipTokenizer == null) {
            return;
        }
        beginUnwatchedTextChange();

        Editable text = getText();
        text.clear();

        if (chipValues != null) {
            for (String chipValue : chipValues) {
                CharSequence chippedText = mChipTokenizer.terminateToken(chipValue, null);
                text.append(chippedText);
            }
        }
        setSelection(text.length());

        endUnwatchedTextChange();
    }

    public void setTextWithChips(@Nullable List<ChipInfo> chips) {
        if (mChipTokenizer == null) {
            return;
        }
        beginUnwatchedTextChange();

        Editable text = getText();
        text.clear();

        if (chips != null) {
            for (ChipInfo chipInfo : chips) {
                CharSequence chippedText = mChipTokenizer.terminateToken(chipInfo.getText(), chipInfo.getData());
                text.append(chippedText);
            }
        }
        setSelection(text.length());
        endUnwatchedTextChange();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (mChipTokenizer == null) {
            return;
        }
        Adapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        beginUnwatchedTextChange();

        Object data = getDataForSuggestion(adapter, position);
        CharSequence text = getFilter().convertResultToString(adapter.getItem(position));

        clearComposingText();
        int end = getSelectionEnd();
        Editable editable = getText();
        int start = mChipTokenizer.findTokenStart(editable, end);

        // guard against java.lang.StringIndexOutOfBoundsException
        start = Math.min(Math.max(0, start), editable.length());
        end = Math.min(Math.max(0, end), editable.length());
        if (end < start) {
            end = start;
        }

        editable.replace(start, end, mChipTokenizer.terminateToken(text, data));

        endUnwatchedTextChange();
    }

    /**
     * Returns a object that will be associated with a chip that is about to be created for the item at {@code position} in {@code adapter} because that
     * item was just tapped.
     *
     * @param adapter  the adapter supplying the suggestions
     * @param position the position of the suggestion that was tapped
     * @return the data object
     */
    protected Object getDataForSuggestion(@NonNull Adapter adapter, int position) {
        return adapter.getItem(position);
    }

    /**
     * If there is a ChipTokenizer set, this method will do nothing. Instead we wait until the OnItemClickListener is triggered to actually perform
     * the text replacement so we can also associate the suggestion data with it.
     * <p>
     * If there is no ChipTokenizer set, we call through to the super method.
     *
     * @param text the text to be chipified
     */
    @Override
    protected void replaceText(CharSequence text) {
        // If we have a ChipTokenizer, this will be handled by our OnItemClickListener so we can do nothing here.
        // If we don't have a ChipTokenizer, we'll use the default behavior
        if (mChipTokenizer == null) {
            super.replaceText(text);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (mIgnoreTextChangedEvents) {
            return;
        }

        mTextChangedStart = start;
        mTextChangedEnd = start + after;

        // Check for backspace
        if (mChipTokenizer != null) {
            if (count > 0 && after < count) {
                int end = start + count;
                Editable message = getText();
                Chip[] chips = mChipTokenizer.findAllChips(start, end, message);

                for (Chip chip : chips) {
                    int spanStart = mChipTokenizer.findChipStart(chip, message);
                    int spanEnd = mChipTokenizer.findChipEnd(chip, message);
                    if ((spanStart < end) && (spanEnd > start)) {
                        // Add to remove list
                        mChipsToRemove.add(chip);
                    }
                }
            }
        }
    }

    @Override
    public void onTextChanged(@NonNull CharSequence textChanged, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable message) {
        if (mIgnoreTextChangedEvents) {
            return;
        }

        // Avoid triggering text changed events from changes we make in this method
        beginUnwatchedTextChange();

        // Handle backspace key
        if (mChipTokenizer != null) {
            Iterator<Chip> iterator = mChipsToRemove.iterator();
            while (iterator.hasNext()) {
                Chip chip = iterator.next();
                iterator.remove();
                mChipTokenizer.deleteChip(chip, message);
                if (mOnChipRemoveListener != null) {
                    mOnChipRemoveListener.onChipRemove(chip);
                }
            }
        }

        // Handle an illegal or chip terminator character
        if (message.length() >= mTextChangedEnd && message.length() >= mTextChangedStart) {
            handleTextChanged(mTextChangedStart, mTextChangedEnd);
        }

        endUnwatchedTextChange();
    }

    private void handleTextChanged(int start, int end) {
        if (start == end) {
            // If start and end are the same there was text deleted, so this type of event can be ignored
            return;
        }

        // First remove any illegal characters
        Editable text = getText();
        CharSequence subText = text.subSequence(start, end);
        CharSequence withoutIllegalCharacters = removeIllegalCharacters(subText);

        // Check if illegal characters were found
        if (withoutIllegalCharacters.length() < subText.length()) {
            text.replace(start, end, withoutIllegalCharacters);
            end = start + withoutIllegalCharacters.length();
            clearComposingText();
        }

        if (start == end) {
            // If start and end are the same here, it means only illegal characters were inserted so there's nothing left to do
            return;
        }

        // Then handle chip terminator characters
        if (mChipTokenizer != null && mChipTerminatorHandler != null) {
            int newSelectionIndex = mChipTerminatorHandler.findAndHandleChipTerminators(mChipTokenizer, getText(), start, end, mIsPasteEvent);
            if (newSelectionIndex > 0) {
                setSelection(newSelectionIndex);
            }
        }
    }

    private CharSequence removeIllegalCharacters(CharSequence text) {
        StringBuilder newText = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char theChar = text.charAt(i);
            if (!isIllegalCharacter(theChar)) {
                newText.append(theChar);
            }
        }

        return newText;
    }

    private boolean isIllegalCharacter(char character) {
        if (illegalCharacterIdentifier != null) {
            return illegalCharacterIdentifier.isCharacterIllegal(character);
        }
        return false;
    }

    /**
     * Chipifies all existing plain text in the field
     */
    public void chipifyAllUnterminatedTokens() {
        beginUnwatchedTextChange();
        chipifyAllUnterminatedTokens(getText());
        endUnwatchedTextChange();
    }

    private void chipifyAllUnterminatedTokens(Editable text) {
        if (mChipTokenizer != null) {
            mChipTokenizer.terminateAllTokens(text);
        }
    }

    /**
     * Replaces the text from start (inclusive) to end (exclusive) with a chip
     * containing the same text
     *
     * @param start the index of the first character to replace
     * @param end   one more than the index of the last character to replace
     */
    public void chipify(int start, int end) {
        beginUnwatchedTextChange();
        chipify(start, end, getText(), null);
        endUnwatchedTextChange();
    }

    private void chipify(int start, int end, Editable text, Object data) {
        if (mChipTokenizer != null) {
            CharSequence textToChip = text.subSequence(start, end);
            CharSequence chippedText = mChipTokenizer.terminateToken(textToChip, data);
            text.replace(start, end, chippedText);
        }
    }

    private CharSequence getTextWithPlainTextSpans(int start, int end) {
        Editable editable = getText();
        String selectedText = editable.subSequence(start, end).toString();

        if (mChipTokenizer != null) {
            List<Chip> chips = Arrays.asList(mChipTokenizer.findAllChips(start, end, editable));
            Collections.reverse(chips);
            for (Chip chip : chips) {
                String chipText = chip.getText().toString();
                int chipStart = mChipTokenizer.findChipStart(chip, editable) - start;
                int chipEnd = mChipTokenizer.findChipEnd(chip, editable) - start;
                selectedText = selectedText.substring(0, chipStart) + chipText + selectedText.substring(chipEnd, selectedText.length());
            }
        }
        return selectedText;
    }

    /**
     * @return all of the chips currently in the text view - this does not include any unchipped text
     */
    @NonNull
    public List<Chip> getAllChips() {
        Editable text = getText();
        return mChipTokenizer != null ? Arrays.asList(mChipTokenizer.findAllChips(0, text.length(), text)) : new ArrayList<Chip>();
    }

    /**
     * Returns a List of the string values of all the chips in the text (obtained through {@link Chip#getText()}).
     * This does not include the text of any unterminated tokens.
     *
     * @return the List of chip values
     */
    @NonNull
    public List<String> getChipValues() {
        List<String> chipValues = new ArrayList<>();

        List<Chip> chips = getAllChips();
        for (Chip chip : chips) {
            chipValues.add(chip.getText().toString());
        }

        return chipValues;
    }

    /**
     * Returns a List of the string values of all the tokens (unchipped text) in the text
     * (obtained through {@link ChipTokenizer#findAllTokens(CharSequence)}). This does not include any chipped text.
     *
     * @return the List of token values
     */
    @NonNull
    public List<String> getTokenValues() {
        List<String> tokenValues = new ArrayList<>();

        if (mChipTokenizer != null) {
            Editable text = getText();
            List<Pair<Integer, Integer>> unterminatedTokenIndexes = mChipTokenizer.findAllTokens(text);
            for (Pair<Integer, Integer> indexes : unterminatedTokenIndexes) {
                String tokenValue = text.subSequence(indexes.first, indexes.second).toString();
                tokenValues.add(tokenValue);
            }
        }

        return tokenValues;
    }

    /**
     * Returns a combination of the chip values and token values in the text.
     *
     * @return the List of all chip and token values
     * @see #getChipValues()
     * @see #getTokenValues()
     */
    @NonNull
    public List<String> getChipAndTokenValues() {
        List<String> chipAndTokenValues = new ArrayList<>();
        chipAndTokenValues.addAll(getChipValues());
        chipAndTokenValues.addAll(getTokenValues());
        return chipAndTokenValues;
    }

    @Override
    public String toString() {
        try {
            return getTextWithPlainTextSpans(0, getText().length()).toString();
        } catch (ClassCastException ex) {  // Exception is thrown by cast in getText() on some LG devices
            return super.toString();
        } catch (StringIndexOutOfBoundsException e) {
            throw new StringIndexOutOfBoundsException(String.format("%s \nError converting toString() [%s]", e.getMessage(), getText().toString()));
        }
    }

    private class ChipTokenizerWrapper implements Tokenizer {

        @NonNull
        private ChipTokenizer mChipTokenizer;

        public ChipTokenizerWrapper(@NonNull ChipTokenizer chipTokenizer) {
            mChipTokenizer = chipTokenizer;
        }

        @Override
        public int findTokenStart(CharSequence text, int cursor) {
            return mChipTokenizer.findTokenStart(text, cursor);
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor) {
            return mChipTokenizer.findTokenEnd(text, cursor);
        }

        @Override
        public CharSequence terminateToken(CharSequence text) {
            return mChipTokenizer.terminateToken(text, null);
        }
    }

    public interface OnChipClickListener {

        /**
         * Called when a chip in this TextView is touched. This callback is triggered by the {@link MotionEvent#ACTION_UP} event.
         *
         * @param chip  the {@link Chip} that was touched
         * @param event the {@link MotionEvent} that caused the touch
         */
        void onChipClick(Chip chip, MotionEvent event);
    }

    public interface OnChipRemoveListener {

        /**
         * Called when a chip in this TextView is removed
         *
         * @param chip  the {@link Chip} that was removed
         */
        void onChipRemove(Chip chip);
    }

    private class SingleTapListener extends GestureDetector.SimpleOnGestureListener {

        /**
         * @param e the {@link MotionEvent} passed to the GestureDetector
         * @return true if singleTapUp (click) was detected
         */
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }
    }
}
