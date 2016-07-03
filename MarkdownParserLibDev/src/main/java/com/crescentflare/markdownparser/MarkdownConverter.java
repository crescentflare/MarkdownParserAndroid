package com.crescentflare.markdownparser;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;

import com.crescentflare.markdownparsercore.MarkdownJavaParser;
import com.crescentflare.markdownparsercore.MarkdownNativeParser;
import com.crescentflare.markdownparsercore.MarkdownParser;
import com.crescentflare.markdownparsercore.MarkdownTag;

/**
 * Markdown library: markdown text converter
 * Convert markdown to other formats usable for Android (like html or spannable strings)
 */
public class MarkdownConverter
{
    public static String toHtmlString(String markdownText)
    {
        MarkdownParser parser = obtainParser(markdownText);
        MarkdownTag[] foundTags = parser.findTags(markdownText);
        String htmlString = "";
        for (MarkdownTag tag : foundTags)
        {
            switch (tag.type)
            {
                case Normal:
                    htmlString += parser.extractText(markdownText, tag).replaceAll("\\n", "<br/>");
                    break;
                case Paragraph:
                    htmlString += "<br/><br/>";
                    break;
                case TextStyle:
                {
                    String openTag = "", closeTag = "";
                    if ((tag.weight & 1) > 0)
                    {
                        openTag += "<i>";
                        closeTag = "</i>" + closeTag;
                    }
                    if ((tag.weight & 2) > 0)
                    {
                        openTag += "<b>";
                        closeTag = "</b>" + closeTag;
                    }
                    htmlString += openTag + parser.extractText(markdownText, tag).replaceAll("\\n", "<br/>") + closeTag;
                    break;
                }
                case AlternativeTextStyle:
                    htmlString += "<strike>" + parser.extractText(markdownText, tag).replaceAll("\\n", "<br/>") + "</strike>";
                    break;
                case Header:
                    htmlString += "<h" + tag.weight + ">" + parser.extractText(markdownText, tag) + "</h" + tag.weight + ">";
                    break;
            }
        }
        return htmlString;
    }

    public static Spanned toSpannable(String markdownText)
    {
        MarkdownParser parser = obtainParser(markdownText);
        MarkdownTag[] foundTags = parser.findTags(markdownText);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        MarkdownTag.Type prevType = null;
        for (MarkdownTag tag : foundTags)
        {
            switch (tag.type)
            {
                case Normal:
                {
                    String text = parser.extractText(markdownText, tag);
                    if (prevType == MarkdownTag.Type.Header || prevType == MarkdownTag.Type.Normal)
                    {
                        builder.append("\n\n");
                    }
                    builder.append(text);
                    break;
                }
                case Paragraph:
                {
                    builder.append("\n\n");
                }
                case TextStyle:
                {
                    String text = parser.extractText(markdownText, tag);
                    if (prevType == MarkdownTag.Type.Header)
                    {
                        builder.append("\n\n");
                    }
                    builder.append(text);
                    if (tag.weight > 0)
                    {
                        int typeface = Typeface.NORMAL;
                        switch (tag.weight)
                        {
                            case 1:
                                typeface = Typeface.ITALIC;
                                break;
                            case 2:
                                typeface = Typeface.BOLD;
                                break;
                            case 3:
                                typeface = Typeface.BOLD_ITALIC;
                                break;
                        }
                        builder.setSpan(new StyleSpan(typeface), builder.length() - text.length(), builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    break;
                }
                case AlternativeTextStyle:
                {
                    String text = parser.extractText(markdownText, tag);
                    if (prevType == MarkdownTag.Type.Header)
                    {
                        builder.append("\n\n");
                    }
                    builder.append(text);
                    builder.setSpan(new StrikethroughSpan(), builder.length() - text.length(), builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                }
                case Header:
                {
                    float size = 1.0f;
                    switch (tag.weight)
                    {
                        case 1:
                            size = 1.5f;
                            break;
                        case 2:
                            size = 1.4f;
                            break;
                        case 3:
                            size = 1.3f;
                            break;
                        case 4:
                            size = 1.2f;
                            break;
                        case 5:
                            size = 1.1f;
                            break;
                        case 6:
                            size = 1.0f;
                            break;
                    }
                    String text = parser.extractText(markdownText, tag);
                    if (prevType != null)
                    {
                        builder.append("\n\n");
                    }
                    builder.append(text);
                    builder.setSpan(new RelativeSizeSpan(size), builder.length() - text.length(), builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.setSpan(new StyleSpan(Typeface.BOLD), builder.length() - text.length(), builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                }
            }
            prevType = tag.type;
        }
        return builder;
    }

    private static MarkdownParser obtainParser(String text)
    {
        return text.length() > 128 ? new MarkdownNativeParser() : new MarkdownJavaParser();
    }
}
