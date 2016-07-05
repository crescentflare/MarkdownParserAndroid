package com.crescentflare.markdownparser;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;

import com.crescentflare.markdownparsercore.MarkdownJavaParser;
import com.crescentflare.markdownparsercore.MarkdownNativeParser;
import com.crescentflare.markdownparsercore.MarkdownParser;
import com.crescentflare.markdownparsercore.MarkdownTag;

import java.util.ArrayList;
import java.util.List;

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

    private static void appendSpannableBuilder(List<MarkdownTag> convertedTags, SpannableStringBuilder builder, String markdownText, MarkdownTag[] foundTags, int start)
    {
        MarkdownTag curTag = foundTags[start];
        MarkdownTag processingTag = null;
        int checkPosition = start + 1;
        boolean processing = true;
        while (processing)
        {
            MarkdownTag nextTag = checkPosition < foundTags.length ? foundTags[checkPosition] : null;
            processing = false;
            if (nextTag != null && nextTag.startPosition < curTag.endPosition)
            {
                if (processingTag == null)
                {
                    processingTag = new MarkdownTag();
                    convertedTags.add(processingTag);
                    processingTag.type = curTag.type;
                    processingTag.weight = curTag.weight;
                    processingTag.startText = builder.length();
                    builder.append(markdownText.substring(curTag.startText, nextTag.startPosition)); //TODO: don't use substring but parser in-between extraction
                    processingTag.endText = builder.length();
                }
                else
                {
                    builder.append(markdownText.substring(processingTag.endPosition, nextTag.startPosition)); //TODO: don't use substring but parser in-between extraction
                    processingTag.endText = builder.length();
                }
                int prevConvertedTagSize = convertedTags.size();
                appendSpannableBuilder(convertedTags, builder, markdownText, foundTags, checkPosition);
                processingTag.endPosition = foundTags[checkPosition].endPosition;
                checkPosition += convertedTags.size() - prevConvertedTagSize;
                processing = true;
            }
            else
            {
                if (processingTag == null)
                {
                    processingTag = new MarkdownTag();
                    convertedTags.add(processingTag);
                    processingTag.type = curTag.type;
                    processingTag.weight = curTag.weight;
                    processingTag.startText = builder.length();
                    builder.append(markdownText.substring(curTag.startText, curTag.endText)); //TODO: don't use substring but parser in-between extraction
                    processingTag.endText = builder.length();
                }
                else
                {
                    builder.append(markdownText.substring(processingTag.endPosition, curTag.endText)); //TODO: don't use substring but parser in-between extraction
                    processingTag.endText = builder.length();
                }
            }
        }
    }

    public static float sizeForHeader(int weight)
    {
        if (weight >= 1 && weight < 6)
        {
            return 1.5f - (float)(weight - 1) * 0.1f;
        }
        return 1.0f;
    }

    public static int textStyleForWeight(int weight)
    {
        switch (weight)
        {
            case 1:
                return Typeface.ITALIC;
            case 2:
                return Typeface.BOLD;
            case 3:
                return Typeface.BOLD_ITALIC;
        }
        return Typeface.NORMAL;
    }

    public static String bulletTokenForWeight(int weight)
    {
        if (weight == 2)
        {
            return "\u25E6 ";
        }
        else if (weight >= 3)
        {
            return "\u25AA ";
        }
        return "\u2022 ";
    }

    public static Spanned toSpannable(String markdownText)
    {
        MarkdownParser parser = obtainParser(markdownText);
        MarkdownTag[] foundTags = parser.findTags(markdownText);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        List<Integer> listCount = new ArrayList<>();
        boolean addedParagraph = true;
        for (int i = 0; i < foundTags.length; i++)
        {
            MarkdownTag sectionTag = foundTags[i];
            if (!addedParagraph)
            {
                builder.append("\n");
            }
            if (sectionTag.type == MarkdownTag.Type.OrderedList || sectionTag.type == MarkdownTag.Type.UnorderedList)
            {
                for (int j = listCount.size(); j < sectionTag.weight; j++)
                {
                    listCount.add(0);
                }
                for (int j = listCount.size(); j > sectionTag.weight; j--)
                {
                    listCount.remove(listCount.size() - 1);
                }
                if (sectionTag.type == MarkdownTag.Type.OrderedList)
                {
                    listCount.set(listCount.size() - 1, listCount.get(listCount.size() - 1) + 1);
                }
            }
            if (sectionTag.type == MarkdownTag.Type.Header || sectionTag.type == MarkdownTag.Type.OrderedList || sectionTag.type == MarkdownTag.Type.UnorderedList || sectionTag.type == MarkdownTag.Type.Normal)
            {
                List<MarkdownTag> convertedTags = new ArrayList<>();
                appendSpannableBuilder(convertedTags, builder, markdownText, foundTags, i);
                i += convertedTags.size() - 1;
                if (sectionTag.type == MarkdownTag.Type.OrderedList || sectionTag.type == MarkdownTag.Type.UnorderedList)
                {
                    String token = sectionTag.type == MarkdownTag.Type.OrderedList ? "" + listCount.get(listCount.size() - 1) + "." : bulletTokenForWeight(sectionTag.weight);
                    builder.setSpan(new MarkdownListSpan(token, 60 + (sectionTag.weight - 1) * 30, 10), convertedTags.get(0).startText, convertedTags.get(0).endText, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                for (MarkdownTag tag : convertedTags)
                {
                    switch (tag.type)
                    {
                        case Header:
                            builder.setSpan(new RelativeSizeSpan(sizeForHeader(tag.weight)), tag.startText, tag.endText, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            builder.setSpan(new StyleSpan(Typeface.BOLD), tag.startText, tag.endText, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case TextStyle:
                            builder.setSpan(new StyleSpan(textStyleForWeight(tag.weight)), tag.startText, tag.endText, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        case AlternativeTextStyle:
                            builder.setSpan(new StrikethroughSpan(), tag.startText, tag.endText, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                    }
                }
                addedParagraph = false;
            }
            else if (sectionTag.type == MarkdownTag.Type.Paragraph)
            {
                for (int j = 0; j < sectionTag.weight; j++)
                {
                    builder.append("\n");
                }
                addedParagraph = true;
                listCount.clear();
            }
        }
        return builder;
    }

    private static MarkdownParser obtainParser(String text)
    {
        return text.length() > 128 ? new MarkdownNativeParser() : new MarkdownJavaParser();
    }

    /**
     * Span to draw lists and unordered list in a better way
     */
    static class MarkdownListSpan implements LeadingMarginSpan
    {
        private String listToken = "";
        private int margin = 0;
        private int offset = 0;

        MarkdownListSpan(String listToken, int margin, int offset)
        {
            this.listToken = listToken;
            this.margin = margin;
            this.offset = offset;
        }

        @Override
        public int getLeadingMargin(boolean first)
        {
            return margin;
        }

        @Override
        public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout)
        {
            if (first)
            {
                Paint.Style orgStyle = p.getStyle();
                p.setStyle(Paint.Style.FILL);
                c.drawText(listToken, x + getLeadingMargin(true) - offset - p.measureText(listToken), bottom - p.descent(), p);
                p.setStyle(orgStyle);
            }
        }
    }
}
