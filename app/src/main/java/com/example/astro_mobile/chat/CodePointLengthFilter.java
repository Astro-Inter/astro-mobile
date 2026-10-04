package com.example.astro_mobile.chat;

import android.text.InputFilter;
import android.text.Spanned;

/** Conta caracteres Unicode sem cortar um emoji ao colar uma mensagem longa. */
final class CodePointLengthFilter implements InputFilter {
    private final int maxLength;
    CodePointLengthFilter(int maxLength) { this.maxLength = maxLength; }

    @Override public CharSequence filter(CharSequence source, int start, int end,
                                         Spanned dest, int dstart, int dend) {
        int remaining = maxLength - (Character.codePointCount(dest, 0, dest.length())
                - Character.codePointCount(dest, dstart, dend));
        if (remaining <= 0) return "";
        if (Character.codePointCount(source, start, end) <= remaining) return null;
        return source.subSequence(start, Character.offsetByCodePoints(source, start, remaining));
    }
}
