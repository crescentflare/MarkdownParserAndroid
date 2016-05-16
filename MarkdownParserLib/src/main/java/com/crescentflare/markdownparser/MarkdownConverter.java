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
                case Italics:
                    htmlString += "<i>" + parser.extractText(markdownText, tag) + "</i>".replaceAll("\\n", "<br/>");
                    break;
                case Bold:
                    htmlString += "<b>" + parser.extractText(markdownText, tag) + "</b>".replaceAll("\\n", "<br/>");
                    break;
                case BoldItalics:
                    htmlString += "<i><b>" + parser.extractText(markdownText, tag) + "</b></i>".replaceAll("\\n", "<br/>");
                    break;
                case Header1:
                    htmlString += "<h1>" + parser.extractText(markdownText, tag) + "</h1>";
                    break;
                case Header2:
                    htmlString += "<h2>" + parser.extractText(markdownText, tag) + "</h2>";
                    break;
                case Header3:
                    htmlString += "<h3>" + parser.extractText(markdownText, tag) + "</h3>";
                    break;
                case Header4:
                    htmlString += "<h4>" + parser.extractText(markdownText, tag) + "</h4>";
                    break;
                case Header5:
                    htmlString += "<h5>" + parser.extractText(markdownText, tag) + "</h5>";
                    break;
                case Header6:
                    htmlString += "<h6>" + parser.extractText(markdownText, tag) + "</h6>";
                    break;
            }
        }
        return htmlString;
    }
}
