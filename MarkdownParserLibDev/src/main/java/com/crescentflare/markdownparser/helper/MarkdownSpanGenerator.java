package com.crescentflare.markdownparser.helper;

import android.text.SpannableStringBuilder;

import com.crescentflare.markdownparsercore.MarkdownTag;

/**
 * Markdown library: helper class
 * An interface to generate spans for markdown tags
 * Provide an implementation to customize styling
 */
public interface MarkdownSpanGenerator
{
    void applySpan(SpannableStringBuilder builder, MarkdownTag.Type type, int weight, int start, int end, String extra);
    String getListToken(MarkdownTag.Type type, int weight, int index);
}
