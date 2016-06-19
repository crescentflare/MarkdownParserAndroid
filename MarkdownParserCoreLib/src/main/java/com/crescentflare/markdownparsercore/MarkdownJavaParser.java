package com.crescentflare.markdownparsercore;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown core library: java parser implementation
 * Parses the markdown data in java (if speed is not important or if the document is very small)
 */
public class MarkdownJavaParser implements MarkdownParser
{
    /**
     * Public function to find all supported markdown tags
     */
    public MarkdownTag[] findTags(String markdownText)
    {
        final List<MarkdownTag> foundTags = new ArrayList<>();
        final int maxLength = markdownText.length();
        int position = 0;
        boolean processing;
        do
        {
            MarkdownTag stylingTag = searchStylingTag(markdownText, maxLength, position);
            if (stylingTag != null)
            {
                if (stylingTag.startPosition > position)
                {
                    MarkdownTag normalTag = makeNormalTag(markdownText, position, stylingTag.startPosition);
                    if (normalTag != null)
                    {
                        addParagraphedNormalTag(foundTags, normalTag, markdownText, maxLength);
                    }
                }
                if (stylingTag.type == MarkdownTag.Type.TextStyle)
                {
                    addNestedStylingTags(foundTags, stylingTag, markdownText, maxLength);
                }
                else
                {
                    foundTags.add(stylingTag);
                }
                position = stylingTag.endPosition;
                processing = true;
            }
            else
            {
                processing = false;
            }
        }
        while (processing);
        if (position < maxLength)
        {
            MarkdownTag normalTag = makeNormalTag(markdownText, position, maxLength);
            if (normalTag != null)
            {
                addParagraphedNormalTag(foundTags, normalTag, markdownText, maxLength);
            }
        }
        return foundTags.toArray(new MarkdownTag[foundTags.size()]);
    }

    /**
     * Extract markdown text components
     */
    public String extractText(String markdownText, MarkdownTag tag)
    {
        if ((tag.flags & MarkdownTag.FLAG_ESCAPED) > 0)
        {
            return escapedSubstring(markdownText, tag.startText, tag.endText);
        }
        return markdownText.substring(tag.startText, tag.endText);
    }

    public String extractFull(String markdownText, MarkdownTag tag)
    {
        if ((tag.flags & MarkdownTag.FLAG_ESCAPED) > 0)
        {
            return escapedSubstring(markdownText, tag.startPosition, tag.endPosition);
        }
        return markdownText.substring(tag.startPosition, tag.endPosition);
    }

    private String escapedSubstring(String text, int startPosition, int endPosition)
    {
        String filteredText = "";
        for (int i = startPosition; i < endPosition; i++)
        {
            char chr = text.charAt(i);
            if (chr == '\\' && text.charAt(i + 1) != '\n')
            {
                filteredText += text.charAt(i + 1);
                i++;
                continue;
            }
            filteredText += chr;
        }
        return filteredText;
    }

    /**
     * Search for types of tag
     */
    private MarkdownTag searchStylingTag(final String markdownText, final int maxLength, final int position)
    {
        char prevChr = 0;
        for (int i = position; i < maxLength; i++)
        {
            final char chr = markdownText.charAt(i);
            if (chr == '\\')
            {
                i++;
                continue;
            }
            if (chr == '#' && (prevChr == '\n' || prevChr == 0 || i == 0))
            {
                return makeHeaderTag(markdownText, maxLength, i);
            }
            if (chr == '*' || chr == '_' || chr == '~')
            {
                return makeTextStyleTag(markdownText, maxLength, i);
            }
            prevChr = chr;
        }
        return null;
    }

    private void addNestedStylingTags(final List<MarkdownTag> foundTags, final MarkdownTag stylingTag, final String markdownText, final int maxLength)
    {
        MarkdownTag nestedTag = searchStylingTag(markdownText, stylingTag.endText, stylingTag.startText);
        if (nestedTag != null && nestedTag.type != MarkdownTag.Type.Header && nestedTag.endPosition < stylingTag.endPosition)
        {
            //Combine found styling tag with nested tag
            nestedTag.flags |= stylingTag.flags & MarkdownTag.FLAG_TEXTSTYLE;

            //Add part of found styling tag before nested style
            MarkdownTag beforeNestedTag = new MarkdownTag();
            beforeNestedTag.type = stylingTag.type;
            beforeNestedTag.startPosition = stylingTag.startPosition;
            beforeNestedTag.startText = stylingTag.startText;
            beforeNestedTag.endPosition = nestedTag.startPosition;
            beforeNestedTag.endText = nestedTag.startPosition;
            beforeNestedTag.flags = stylingTag.flags;
            if (beforeNestedTag.startText < beforeNestedTag.endText)
            {
                foundTags.add(beforeNestedTag);
            }

            //Add found nested style tag recursively
            addNestedStylingTags(foundTags, nestedTag, markdownText, maxLength);

            //Add part of found styling tag after nested style
            MarkdownTag afterNestedTag = new MarkdownTag();
            afterNestedTag.type = stylingTag.type;
            afterNestedTag.startPosition = nestedTag.endPosition;
            afterNestedTag.startText = nestedTag.endPosition;
            afterNestedTag.endPosition = stylingTag.endPosition;
            afterNestedTag.endText = stylingTag.endText;
            afterNestedTag.flags = stylingTag.flags;
            if (afterNestedTag.startText < afterNestedTag.endText)
            {
                foundTags.add(afterNestedTag);
            }
        }
        else
        {
            foundTags.add(stylingTag);
        }
    }

    private void addParagraphedNormalTag(final List<MarkdownTag> foundTags, final MarkdownTag stylingTag, final String markdownText, final int maxLength)
    {
        boolean foundEscapedChar = false, foundParagraphBreak = false;
        int lineStart = stylingTag.startText, endParagraph = -1, foundPrintableCharAt = -1;
        int newlineCount = 0;
        if (stylingTag.startText > 0 && markdownText.charAt(stylingTag.startText - 1) != '\n')
        {
            foundPrintableCharAt = stylingTag.startText;
        }
        for (int i = stylingTag.startText; i < stylingTag.endText; i++)
        {
            char chr = markdownText.charAt(i);
            if (chr == '\\' && markdownText.charAt(i + 1) != '\n')
            {
                foundEscapedChar = true;
                continue;
            }
            if (foundPrintableCharAt < 0 && !isWhitespace(chr))
            {
                foundPrintableCharAt = lineStart;
            }
            if (chr == '\n')
            {
                lineStart = i + 1;
            }
            if (chr == '\n' && foundPrintableCharAt >= 0)
            {
                newlineCount++;
                if (newlineCount == 1)
                {
                    endParagraph = i;
                }
                if (newlineCount > 1)
                {
                    if (foundParagraphBreak)
                    {
                        foundTags.add(makeParagraphTag(stylingTag.startText));
                    }
                    MarkdownTag normalTag = makeNormalTag(markdownText, stylingTag.startPosition, endParagraph, foundPrintableCharAt, endParagraph);
                    if (foundEscapedChar)
                    {
                        normalTag.flags |= MarkdownTag.FLAG_ESCAPED;
                    }
                    foundTags.add(normalTag);
                    stylingTag.startPosition = i + 1;
                    stylingTag.startText = i + 1;
                    foundEscapedChar = false;
                    foundParagraphBreak = true;
                    foundPrintableCharAt = -1;
                    newlineCount = 0;
                }
            }
            else if (chr != ' ')
            {
                newlineCount = 0;
            }
        }
        if (foundPrintableCharAt >= 0)
        {
            if (foundParagraphBreak)
            {
                foundTags.add(makeParagraphTag(stylingTag.startText));
            }
            stylingTag.startText = foundPrintableCharAt;
            if (foundEscapedChar)
            {
                stylingTag.flags |= MarkdownTag.FLAG_ESCAPED;
            }
            if (stylingTag.startText < stylingTag.endText)
            {
                foundTags.add(stylingTag);
            }
        }
    }

    /**
     * Find tags based on start position and iterating to find the end position
     */
    private MarkdownTag makeParagraphTag(final int position)
    {
        MarkdownTag paragraphTag = new MarkdownTag();
        paragraphTag.type = MarkdownTag.Type.Paragraph;
        paragraphTag.startPosition = position;
        paragraphTag.startText = position;
        paragraphTag.endPosition = position;
        paragraphTag.endText = position;
        return paragraphTag;
    }

    private MarkdownTag makeNormalTag(final String markdownText, final int startText, final int endText)
    {
        return makeNormalTag(markdownText, startText, endText, startText, endText);
    }

    private MarkdownTag makeNormalTag(final String markdownText, final int startPosition, final int endPosition, final int startText, final int endText)
    {
        MarkdownTag normalTag = new MarkdownTag();
        normalTag.type = MarkdownTag.Type.Normal;
        normalTag.startPosition = startPosition;
        normalTag.startText = startText;
        normalTag.endPosition = endPosition;
        normalTag.endText = endText;
        if (markdownText.charAt(endPosition - 1) == '\n')
        {
            normalTag.endText--;
        }
        if (normalTag.startText < normalTag.endText)
        {
            return normalTag;
        }
        return null;
    }

    private MarkdownTag makeHeaderTag(final String markdownText, final int maxLength, final int position)
    {
        MarkdownTag tag = new MarkdownTag();
        int headerSize = 0;
        tag.startPosition = position;
        for (int i = position; i < maxLength; i++)
        {
            final char chr = markdownText.charAt(i);
            if (chr == '\\' && markdownText.charAt(i + 1) != '\n')
            {
                tag.flags |= MarkdownTag.FLAG_ESCAPED;
                i++;
                continue;
            }
            if (tag.startText < 0)
            {
                if (chr == '#' && headerSize < 6)
                {
                    headerSize++;
                }
                else if (chr != ' ')
                {
                    tag.startText = i;
                    tag.type = MarkdownTag.Type.Header;
                    tag.sizeForType = headerSize;
                }
            }
            else
            {
                if (chr == '\n')
                {
                    tag.endPosition = i + 1;
                    tag.endText = i;
                    break;
                }
            }
        }
        if (tag.endPosition < 0)
        {
            tag.endPosition = maxLength;
            tag.endText = maxLength;
        }
        if (tag.startText >= 0)
        {
            return tag;
        }
        return null;
    }

    private MarkdownTag makeTextStyleTag(final String markdownText, final int maxLength, final int position)
    {
        MarkdownTag tag = new MarkdownTag();
        int styleStrength = 0;
        int needStyleStrength = 0;
        char tagChr = markdownText.charAt(position);
        tag.startPosition = position;
        for (int i = position; i < maxLength; i++)
        {
            final char chr = markdownText.charAt(i);
            if (chr == '\\' && markdownText.charAt(i + 1) != '\n')
            {
                tag.flags |= MarkdownTag.FLAG_ESCAPED;
                i++;
                continue;
            }
            if (tag.startText < 0)
            {
                if (chr == tagChr && ((tagChr != '~' && styleStrength < 3) || (tagChr == '~' && styleStrength < 2)))
                {
                    styleStrength++;
                }
                else if (tagChr != '~' || styleStrength == 2)
                {
                    tag.startText = i;
                    tag.type = MarkdownTag.Type.TextStyle;
                    needStyleStrength = styleStrength;
                }
            }
            else
            {
                if (chr == tagChr)
                {
                    needStyleStrength--;
                    if (needStyleStrength == 0)
                    {
                        if (tagChr != '~')
                        {
                            tag.flags |= flagsForTextStrength(styleStrength);
                        }
                        else
                        {
                            tag.flags |= MarkdownTag.FLAG_STRIKETHROUGH;
                        }
                        tag.endPosition = i + 1;
                        tag.endText = i + 1 - styleStrength;
                        break;
                    }
                }
                else if (needStyleStrength != styleStrength)
                {
                    if (tagChr != '~' && styleStrength - needStyleStrength > 0)
                    {
                        tag.flags |= flagsForTextStrength(styleStrength - needStyleStrength);
                        tag.startText -= needStyleStrength;
                        tag.endPosition = i;
                        tag.endText = i - (styleStrength - needStyleStrength);
                        break;
                    }
                    else
                    {
                        needStyleStrength = styleStrength;
                    }
                }
            }
        }
        if (tag.startText >= 0 && tag.endText >= 0 && (tag.flags & MarkdownTag.FLAG_TEXTSTYLE) > 0)
        {
            return tag;
        }
        return null;
    }

    /**
     * Helpers
     */
    private int flagsForTextStrength(final int textStrength)
    {
        switch (textStrength)
        {
            case 1:
                return MarkdownTag.FLAG_ITALICS;
            case 2:
                return MarkdownTag.FLAG_BOLD;
            case 3:
                return MarkdownTag.FLAG_BOLDITALICS;
        }
        return 0;
    }

    private boolean isWhitespace(char chr)
    {
        return chr == ' ' || chr == '\n' || chr == '\r' || chr == '\t';
    }
}
