package com.hootsuite.nachos.terminator;

import android.text.Editable;

import com.hootsuite.nachos.tokenizer.ChipTokenizer;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DefaultChipTerminatorHandler implements ChipTerminatorHandler {

    @Nullable
    private Map<Character, Integer> mChipTerminators;
    private int mPasteBehavior = BEHAVIOR_CHIPIFY_TO_TERMINATOR;

    @Override
    public void setChipTerminators(@Nullable Map<Character, Integer> chipTerminators) {
        mChipTerminators = chipTerminators;
    }

    @Override
    public void addChipTerminator(char character, int behavior) {
        if (mChipTerminators == null) {
            mChipTerminators = new HashMap<>();
        }

        mChipTerminators.put(character, behavior);
    }

    @Override
    public void setPasteBehavior(int pasteBehavior) {
        mPasteBehavior = pasteBehavior;
    }

    @Override
    public int findAndHandleChipTerminators(@NonNull ChipTokenizer tokenizer, @NonNull Editable text, int start, int end, boolean isPasteEvent) {
        // If we don't have a tokenizer or any chip terminators, there's nothing to look for
        if (mChipTerminators == null) {
            return -1;
        }

        TextIterator textIterator = new TextIterator(text, start, end);
        int selectionIndex = -1;

        characterLoop:
        while (textIterator.hasNextCharacter()) {
            char theChar = textIterator.nextCharacter();
            if (isChipTerminator(theChar)) {
                int behavior = (isPasteEvent && mPasteBehavior != PASTE_BEHAVIOR_USE_DEFAULT) ? mPasteBehavior : mChipTerminators.get(theChar);
                int newSelection = -1;
                switch (behavior) {
                    case BEHAVIOR_CHIPIFY_ALL:
                        selectionIndex = handleChipifyAll(textIterator, tokenizer);
                        break characterLoop;
                    case BEHAVIOR_CHIPIFY_CURRENT_TOKEN:
                        newSelection = handleChipifyCurrentToken(textIterator, tokenizer);
                        break;
                    case BEHAVIOR_CHIPIFY_TO_TERMINATOR:
                        newSelection = handleChipifyToTerminator(textIterator, tokenizer);
                        break;
                }

                if (newSelection != -1) {
                    selectionIndex = newSelection;
                }
            }
        }

        return selectionIndex;
    }

    private int handleChipifyAll(TextIterator textIterator, ChipTokenizer tokenizer) {
        textIterator.deleteCharacter(true);
        tokenizer.terminateAllTokens(textIterator.getText());
        return textIterator.totalLength();
    }

    private int handleChipifyCurrentToken(TextIterator textIterator, ChipTokenizer tokenizer) {
        textIterator.deleteCharacter(true);
        Editable text = textIterator.getText();
        int index = textIterator.getIndex();
        int tokenStart = tokenizer.findTokenStart(text, index);
        int tokenEnd = tokenizer.findTokenEnd(text, index);
        if (tokenStart < tokenEnd) {
            CharSequence chippedText = tokenizer.terminateToken(text.subSequence(tokenStart, tokenEnd), null);
            textIterator.replace(tokenStart, tokenEnd, chippedText);
            return tokenStart + chippedText.length();
        }
        return -1;
    }

    private int handleChipifyToTerminator(TextIterator textIterator, ChipTokenizer tokenizer) {
        Editable text = textIterator.getText();
        int index = textIterator.getIndex();
        if (index > 0) {
            int tokenStart = tokenizer.findTokenStart(text, index);
            if (tokenStart < index) {
                CharSequence chippedText = tokenizer.terminateToken(text.subSequence(tokenStart, index), null);
                textIterator.replace(tokenStart, index + 1, chippedText);
            } else {
                textIterator.deleteCharacter(false);
            }
        } else {
            textIterator.deleteCharacter(false);
        }
        return -1;
    }

    private boolean isChipTerminator(char character) {
        return mChipTerminators != null && mChipTerminators.keySet().contains(character);
    }
}
