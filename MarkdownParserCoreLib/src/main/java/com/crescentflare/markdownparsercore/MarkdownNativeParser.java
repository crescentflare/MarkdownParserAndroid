package com.crescentflare.markdownparsercore;

/**
 * Markdown core library: native parser implementation
 * Parses the markdown data in fast native code (useful for big documents)
 */
public class MarkdownNativeParser implements MarkdownParser
{
    /**
     * Import native library
     */
    static
    {
        System.loadLibrary("markdownparser_native");
    }

    /**
     * Wrapper for finding markdown tags natively, most of the work is being done in the C source file
     */
    public MarkdownTag[] findTags(String markdownText)
    {
        int[] nativeTags = findNativeTags(markdownText);
        int count = getTagCount(nativeTags);
        if (count > 0)
        {
            MarkdownTag[] tags = new MarkdownTag[count];
            for (int i = 0; i < count; i++)
            {
                tags[i] = getConvertedTag(nativeTags, i);
            }
            return tags;
        }
        return new MarkdownTag[0];
    }

    private native int[] findNativeTags(String markdownText);

    /**
     * Convert tags from native int array to java object
     */
    private static final int FIELD_COUNT = 9;

    private int getTagCount(final int[] nativeTags)
    {
        if (nativeTags == null)
        {
            return 0;
        }
        return nativeTags.length / FIELD_COUNT;
    }

    private MarkdownTag getConvertedTag(final int[] nativeTags, int position)
    {
        MarkdownTag tag = new MarkdownTag();
        position *= FIELD_COUNT;
        tag.type = getConvertedTagType(nativeTags[position]);
        tag.flags = nativeTags[position + 1];
        tag.startPosition = nativeTags[position + 2];
        tag.endPosition = nativeTags[position + 3];
        tag.startText = nativeTags[position + 4];
        tag.endText = nativeTags[position + 5];
        tag.weight = nativeTags[position + 8];
        tag.nativeInfo = new int[2];
        tag.nativeInfo[0] = nativeTags[position + 6];
        tag.nativeInfo[1] = nativeTags[position + 7];
        return tag;
    }

    private MarkdownTag.Type getConvertedTagType(int nativeEnumValue)
    {
        switch (nativeEnumValue)
        {
            case 1:
                return MarkdownTag.Type.Normal;
            case 2:
                return MarkdownTag.Type.Paragraph;
            case 3:
                return MarkdownTag.Type.TextStyle;
            case 4:
                return MarkdownTag.Type.AlternativeTextStyle;
            case 5:
                return MarkdownTag.Type.Header;
            case 6:
                return MarkdownTag.Type.OrderedList;
            case 7:
                return MarkdownTag.Type.UnorderedList;
        }
        return MarkdownTag.Type.Normal;
    }

    /**
     * Extract markdown text components
     */
    public String extractText(String markdownText, MarkdownTag tag)
    {
        if ((tag.flags & MarkdownTag.FLAG_ESCAPED) > 0)
        {
            if (tag.nativeInfo != null)
            {
                return escapedSubstring(markdownText, tag.nativeInfo[1], tag.endText - tag.startText);
            }
        }
        return markdownText.substring(tag.startText, tag.endText);
    }

    public String extractFull(String markdownText, MarkdownTag tag)
    {
        if ((tag.flags & MarkdownTag.FLAG_ESCAPED) > 0)
        {
            if (tag.nativeInfo != null)
            {
                return escapedSubstring(markdownText, tag.nativeInfo[0], tag.endPosition - tag.startPosition);
            }
        }
        return markdownText.substring(tag.startPosition, tag.endPosition);
    }

    private native String escapedSubstring(String text, int bytePosition, int length);
}
