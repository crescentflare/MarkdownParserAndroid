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
                if (stylingTag.type.isTextStyle())
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
        return markdownText.substring(tag.startText, tag.endText);
    }

    public String extractFull(String markdownText, MarkdownTag tag)
    {
        return markdownText.substring(tag.startPosition, tag.endPosition);
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
            if (chr == '#' && (prevChr == '\n' || prevChr == 0 || i == 0))
            {
                return makeHeaderTag(markdownText, maxLength, i);
            }
            if (chr == '*' || chr == '_')
            {
                return makeTextStyleTag(markdownText, maxLength, i);
            }
            prevChr = chr;
        }
        return null;
    }

    private void addNestedStylingTags(final List<MarkdownTag> foundTags, final MarkdownTag stylingTag, final String markdownText, final int maxLength)
    {
        MarkdownTag nestedTag = searchStylingTag(markdownText, maxLength, stylingTag.startText);
        if (nestedTag != null && !nestedTag.type.isHeader() && nestedTag.endPosition < stylingTag.endPosition)
        {
            //Combine found styling tag with nested tag
            if ((stylingTag.type == MarkdownTag.Type.Italics && nestedTag.type == MarkdownTag.Type.Bold) || (stylingTag.type == MarkdownTag.Type.Bold && nestedTag.type == MarkdownTag.Type.Italics))
            {
                nestedTag.type = MarkdownTag.Type.BoldItalics;
            }

            //Add part of found styling tag before nested style
            MarkdownTag beforeNestedTag = new MarkdownTag();
            beforeNestedTag.type = stylingTag.type;
            beforeNestedTag.startPosition = stylingTag.startPosition;
            beforeNestedTag.startText = stylingTag.startText;
            beforeNestedTag.endPosition = nestedTag.startPosition;
            beforeNestedTag.endText = nestedTag.startPosition;
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
        int newlineCount = 0, endParagraph = 0;
        for (int i = stylingTag.startText; i < stylingTag.endText; i++)
        {
            char chr = markdownText.charAt(i);
            if (chr == '\n')
            {
                newlineCount++;
                if (newlineCount == 1)
                {
                    endParagraph = i;
                }
                if (newlineCount > 1)
                {
                    MarkdownTag paragraphTag = new MarkdownTag();
                    paragraphTag.type = MarkdownTag.Type.Normal;
                    paragraphTag.startPosition = stylingTag.startPosition;
                    paragraphTag.startText = stylingTag.startText;
                    paragraphTag.endPosition = endParagraph;
                    paragraphTag.endText = endParagraph;
                    foundTags.add(paragraphTag);
                    stylingTag.startPosition = i + 1;
                    stylingTag.startText = i + 1;
                    newlineCount = 0;
                }
            }
            else if (chr != ' ')
            {
                newlineCount = 0;
            }
        }
        foundTags.add(stylingTag);
    }

    /**
     * Find tags based on start position and iterating to find the end position
     */
    private MarkdownTag makeNormalTag(final String markdownText, final int position, final int endPosition)
    {
        MarkdownTag normalTag = new MarkdownTag();
        normalTag.type = MarkdownTag.Type.Normal;
        normalTag.startPosition = position;
        normalTag.startText = position;
        normalTag.endPosition = endPosition;
        normalTag.endText = endPosition;
        if (markdownText.charAt(endPosition - 1) == '\n')
        {
            normalTag.endPosition--;
            normalTag.endText--;
        }
        if (normalTag.startPosition < normalTag.endPosition)
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
        tag.endPosition = -1;
        tag.startText = -1;
        tag.endText = -1;
        for (int i = position; i < maxLength; i++)
        {
            final char chr = markdownText.charAt(i);
            if (tag.startText < 0)
            {
                if (chr == '#' && headerSize < 6)
                {
                    headerSize++;
                }
                else if (chr != ' ')
                {
                    tag.startText = i;
                    tag.type = MarkdownTag.Type.headerForSize(headerSize);
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
        tag.endPosition = -1;
        tag.startText = -1;
        tag.endText = -1;
        for (int i = position; i < maxLength; i++)
        {
            final char chr = markdownText.charAt(i);
            if (tag.startText < 0)
            {
                if (chr == tagChr && styleStrength < 3)
                {
                    styleStrength++;
                }
                else
                {
                    tag.startText = i;
                    tag.type = MarkdownTag.Type.textStyleForStrength(styleStrength);
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
                        tag.endPosition = i + 1;
                        tag.endText = i + 1 - styleStrength;
                        break;
                    }
                }
                else if (needStyleStrength != styleStrength)
                {
                    needStyleStrength = styleStrength;
                }
            }
        }
        if (tag.startText >= 0 && tag.endText >= 0)
        {
            return tag;
        }
        return null;
    }
}
