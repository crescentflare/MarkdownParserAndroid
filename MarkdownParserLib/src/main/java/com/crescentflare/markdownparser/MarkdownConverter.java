package com.crescentflare.markdownparser;

import com.crescentflare.markdownparsercore.MarkdownJavaParser;
import com.crescentflare.markdownparsercore.MarkdownNativeParser;
import com.crescentflare.markdownparsercore.MarkdownParser;
import com.crescentflare.markdownparsercore.MarkdownTag;

/**
 * Markdown library: markdown text converter
 * Convert markdown to other formats usable for Android (like html or, in the future, spannable strings)
 */
public class MarkdownConverter
{
    public static String toHtmlString(String markdownText)
    {
        MarkdownParser parser = markdownText.length() > 128 ? new MarkdownNativeParser() : new MarkdownJavaParser();
        MarkdownTag[] foundTags = parser.findTags(markdownText);
        String htmlString = "";
        for (MarkdownTag tag : foundTags)
        {
            switch (tag.type)
            {
                case Normal:
                    htmlString += parser.extractText(markdownText, tag);
                    break;
                case TextStyle:
                {
                    String openTag = "", closeTag = "";
                    if ((tag.flags & MarkdownTag.FLAG_ITALICS) > 0)
                    {
                        openTag += "<i>";
                        closeTag = "</i>" + closeTag;
                    }
                    if ((tag.flags & MarkdownTag.FLAG_BOLD) > 0)
                    {
                        openTag += "<b>";
                        closeTag = "</b>" + closeTag;
                    }
                    if ((tag.flags & MarkdownTag.FLAG_STRIKETHROUGH) > 0)
                    {
                        openTag += "<strike>";
                        closeTag = "</strike>" + closeTag;
                    }
                    htmlString += openTag + parser.extractText(markdownText, tag) + closeTag.replaceAll("\\n", "<br/>");
                    break;
                }
                case Header:
                    htmlString += "<h" + tag.sizeForType + ">" + parser.extractText(markdownText, tag) + "</h" + tag.sizeForType + ">";
                    break;
            }
        }
        return htmlString;
    }
}
