package com.hootsuite.nachos.terminator;

import android.text.Editable;

public class TextIterator {

    private Editable mText;
    private int mStart;
    private int mEnd;

    private int mIndex;

    public TextIterator(Editable text, int start, int end) {
        mText = text;
        mStart = start;
        mEnd = end;

        mIndex = mStart - 1; // Subtract 1 so that the first call to nextCharacter() will return the first character
    }

    public int totalLength() {
        return mText.length();
    }

    public int windowLength() {
        return mEnd - mStart;
    }

    public Editable getText() {
        return mText;
    }

    public int getIndex() {
        return mIndex;
    }

    public boolean hasNextCharacter() {
        return (mIndex + 1) < mEnd;
    }

    public char nextCharacter() {
        mIndex++;
        return mText.charAt(mIndex);
    }

    public void deleteCharacter(boolean maintainIndex) {
        mText.replace(mIndex, mIndex + 1, "");
        if (!maintainIndex) {
            mIndex--;
        }
        mEnd--;
    }

    public void replace(int replaceStart, int replaceEnd, CharSequence chippedText) {
        mText.replace(replaceStart, replaceEnd, chippedText);

        // Update indexes
        int newLength = chippedText.length();
        int oldLength = replaceEnd - replaceStart;
        mIndex = replaceStart + newLength - 1;
        mEnd += newLength - oldLength;
    }
}
