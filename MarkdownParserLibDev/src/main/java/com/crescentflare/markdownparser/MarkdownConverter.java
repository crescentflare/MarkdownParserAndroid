package com.crescentflare.markdownparser;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;

import com.crescentflare.markdownparser.helper.AlignedListSpan;
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
    /**
     * Static member to determine availability of the native core parser implementation
     */
    private static int nativeParserLibraryLoaded = 0;

    /**
     * HTML conversion handling
     */
    public static String toHtmlString(String markdownText)
    {
        MarkdownParser parser = obtainParser(markdownText);
        MarkdownTag[] foundTags = parser.findTags(markdownText);
        String htmlString = "";
        List<Integer> listCount = new ArrayList<>();
        MarkdownTag.Type prevSectionType = MarkdownTag.Type.Paragraph;
        boolean addedParagraph = true;
        for (int i = 0; i < foundTags.length; i++)
        {
            MarkdownTag sectionTag = foundTags[i];
            if (!addedParagraph && sectionTag.type == MarkdownTag.Type.Normal)
            {
                htmlString += "<br/>";
            }
            if (sectionTag.type == MarkdownTag.Type.OrderedList || sectionTag.type == MarkdownTag.Type.UnorderedList)
            {
                int matchedType = sectionTag.type == MarkdownTag.Type.OrderedList ? 0 : 1;
                if (listCount.size() == sectionTag.weight && listCount.size() > 0 && listCount.get(listCount.size() - 1) != matchedType)
                {
                    htmlString += listCount.get(listCount.size() - 1) == 0 ? "</ol>" : "</ul>";
                    listCount.remove(listCount.size() - 1);
                }
                for (int j = listCount.size(); j < sectionTag.weight; j++)
                {
                    listCount.add(sectionTag.type == MarkdownTag.Type.OrderedList ? 0 : 1);
                    htmlString += sectionTag.type == MarkdownTag.Type.OrderedList ? "<ol>" : "<ul>";
                }
                for (int j = listCount.size(); j > sectionTag.weight; j--)
                {
                    htmlString += listCount.get(listCount.size() - 1) == 0 ? "</ol>" : "</ul>";
                    listCount.remove(listCount.size() - 1);
                }
            }
            if (sectionTag.type == MarkdownTag.Type.Header || sectionTag.type == MarkdownTag.Type.OrderedList || sectionTag.type == MarkdownTag.Type.UnorderedList || sectionTag.type == MarkdownTag.Type.Normal)
            {
                List<MarkdownTag> handledTags = new ArrayList<>();
                htmlString += getHtmlTag(parser, markdownText, sectionTag, false);
                htmlString = appendHtmlString(parser, handledTags, htmlString, markdownText, foundTags, i);
                htmlString += getHtmlTag(parser, markdownText, sectionTag, true);
                i += handledTags.size() - 1;
                addedParagraph = sectionTag.type != MarkdownTag.Type.Normal;
            }
            else if (sectionTag.type == MarkdownTag.Type.Paragraph)
            {
                boolean nextNormal = i + 1 < foundTags.length && foundTags[i + 1].type == MarkdownTag.Type.Normal;
                if (prevSectionType == MarkdownTag.Type.Normal && nextNormal)
                {
                    for (int j = 0; j < sectionTag.weight + 1; j++)
                    {
                        htmlString += "<br/>";
                    }
                }
                addedParagraph = true;
                for (int j = listCount.size(); j > 0; j--)
                {
                    htmlString += listCount.get(listCount.size() - 1) == 0 ? "</ol>" : "</ul>";
                    listCount.remove(listCount.size() - 1);
                }
            }
            prevSectionType = sectionTag.type;
        }
        for (int j = listCount.size(); j > 0; j--)
        {
            htmlString += listCount.get(listCount.size() - 1) == 0 ? "</ol>" : "</ul>";
            listCount.remove(listCount.size() - 1);
        }
        return htmlString;
    }

    private static String appendHtmlString(MarkdownParser parser, List<MarkdownTag> handledTags, String htmlString, String markdownText, MarkdownTag[] foundTags, int start)
    {
        MarkdownTag curTag = foundTags[start];
        MarkdownTag intermediateTag = null;
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
                    handledTags.add(processingTag);
                    processingTag.type = curTag.type;
                    processingTag.weight = curTag.weight;
                    processingTag.startExtra = curTag.startExtra;
                    processingTag.endExtra = curTag.endExtra;
                    processingTag.startText = htmlString.length();
                    htmlString += parser.extractTextBetween(markdownText, curTag, nextTag, MarkdownParser.ExtractBetweenMode.StartToNext);
                    processingTag.endText = htmlString.length();
                }
                else
                {
                    htmlString += parser.extractTextBetween(markdownText, intermediateTag, nextTag, MarkdownParser.ExtractBetweenMode.IntermediateToNext);
                    processingTag.endText = htmlString.length();
                }
                int prevHandledTagSize = handledTags.size();
                htmlString += getHtmlTag(parser, markdownText, nextTag, false);
                htmlString = appendHtmlString(parser, handledTags, htmlString, markdownText, foundTags, checkPosition);
                htmlString += getHtmlTag(parser, markdownText, nextTag, true);
                intermediateTag = foundTags[checkPosition];
                checkPosition += handledTags.size() - prevHandledTagSize;
                processing = true;
            }
            else
            {
                if (processingTag == null)
                {
                    processingTag = new MarkdownTag();
                    handledTags.add(processingTag);
                    processingTag.type = curTag.type;
                    processingTag.weight = curTag.weight;
                    processingTag.startExtra = curTag.startExtra;
                    processingTag.endExtra = curTag.endExtra;
                    processingTag.startText = htmlString.length();
                    htmlString += parser.extractText(markdownText, curTag);
                    processingTag.endText = htmlString.length();
                }
                else
                {
                    htmlString += parser.extractTextBetween(markdownText, intermediateTag, curTag, MarkdownParser.ExtractBetweenMode.IntermediateToEnd);
                    processingTag.endText = htmlString.length();
                }
            }
        }
        return htmlString;
    }

    private static String getHtmlTag(MarkdownParser parser, String markdownText, MarkdownTag tag, boolean closingTag)
    {
        String start = closingTag ? "</" : "<";
        if (tag.type == MarkdownTag.Type.TextStyle)
        {
            switch (tag.weight)
            {
                case 1:
                    start += "i";
                    break;
                case 2:
                    start += "b";
                    break;
                case 3:
                    if (closingTag)
                    {
                        start += "b>" + start + "i";
                    }
                    else
                    {
                        start += "i>" + start + "b";
                    }
            }
        }
        else if (tag.type == MarkdownTag.Type.AlternativeTextStyle)
        {
            start += "strike";
        }
        else if (tag.type == MarkdownTag.Type.Header)
        {
            int headerSize = 6;
            if (tag.weight >= 1 && tag.weight < 7)
            {
                headerSize = tag.weight;
            }
            start += "h" + headerSize;
        }
        else if (tag.type == MarkdownTag.Type.OrderedList || tag.type == MarkdownTag.Type.UnorderedList)
        {
            start += "li";
        }
        else if (tag.type == MarkdownTag.Type.Link)
        {
            start += "a";
            if (!closingTag)
            {
                String linkLocation = parser.extractExtra(markdownText, tag);
                if (linkLocation.length() == 0)
                {
                    linkLocation = parser.extractText(markdownText, tag);
                }
                start += " href=" + linkLocation;
            }
        }
        else
        {
            return "";
        }
        return start + ">";
    }

    /**
     * Spannable conversion handling
     */
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
                appendSpannableBuilder(parser, convertedTags, builder, markdownText, foundTags, i);
                i += convertedTags.size() - 1;
                if (sectionTag.type == MarkdownTag.Type.OrderedList || sectionTag.type == MarkdownTag.Type.UnorderedList)
                {
                    String token = sectionTag.type == MarkdownTag.Type.OrderedList ? "" + listCount.get(listCount.size() - 1) + "." : bulletTokenForWeight(sectionTag.weight);
                    builder.setSpan(new AlignedListSpan(token, 30 + (sectionTag.weight - 1) * 15, 5), convertedTags.get(0).startText, convertedTags.get(0).endText, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
                        case Link:
                        {
                            String linkLocation = parser.extractExtra(markdownText, tag);
                            if (linkLocation.length() == 0)
                            {
                                linkLocation = builder.subSequence(tag.startText, tag.endText).toString();
                            }
                            builder.setSpan(new URLSpan(linkLocation), tag.startText, tag.endText, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        }
                    }
                }
                addedParagraph = false;
            }
            else if (sectionTag.type == MarkdownTag.Type.Paragraph)
            {
                if (sectionTag.weight > 0)
                {
                    builder.append("\n");
                    builder.setSpan(new RelativeSizeSpan(sectionTag.weight), builder.length() - 1, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                addedParagraph = true;
                listCount.clear();
            }
        }
        return builder;
    }

    private static void appendSpannableBuilder(MarkdownParser parser, List<MarkdownTag> convertedTags, SpannableStringBuilder builder, String markdownText, MarkdownTag[] foundTags, int start)
    {
        MarkdownTag curTag = foundTags[start];
        MarkdownTag intermediateTag = null;
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
                    processingTag.startExtra = curTag.startExtra;
                    processingTag.endExtra = curTag.endExtra;
                    processingTag.startText = builder.length();
                    builder.append(parser.extractTextBetween(markdownText, curTag, nextTag, MarkdownParser.ExtractBetweenMode.StartToNext));
                    processingTag.endText = builder.length();
                }
                else
                {
                    builder.append(parser.extractTextBetween(markdownText, intermediateTag, nextTag, MarkdownParser.ExtractBetweenMode.IntermediateToNext));
                    processingTag.endText = builder.length();
                }
                int prevConvertedTagSize = convertedTags.size();
                appendSpannableBuilder(parser, convertedTags, builder, markdownText, foundTags, checkPosition);
                intermediateTag = foundTags[checkPosition];
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
                    processingTag.startExtra = curTag.startExtra;
                    processingTag.endExtra = curTag.endExtra;
                    processingTag.startText = builder.length();
                    builder.append(parser.extractText(markdownText, curTag));
                    processingTag.endText = builder.length();
                }
                else
                {
                    builder.append(parser.extractTextBetween(markdownText, intermediateTag, curTag, MarkdownParser.ExtractBetweenMode.IntermediateToEnd));
                    processingTag.endText = builder.length();
                }
            }
        }
    }

    private static float sizeForHeader(int weight)
    {
        if (weight >= 1 && weight < 6)
        {
            return 1.5f - (float)(weight - 1) * 0.1f;
        }
        return 1.0f;
    }

    private static int textStyleForWeight(int weight)
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

    private static String bulletTokenForWeight(int weight)
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

    /**
     * Obtain parser instance based on requirements
     */
    private static MarkdownParser obtainParser(String text)
    {
        if (nativeParserLibraryLoaded == 0)
        {
            try
            {
                System.loadLibrary("markdownparser_native");
                nativeParserLibraryLoaded = 1;
            }
            catch (Throwable t)
            {
                nativeParserLibraryLoaded = -1;
            }
        }
        return text.length() > 128 && nativeParserLibraryLoaded == 1 ? new MarkdownNativeParser() : new MarkdownJavaParser();
    }
}
