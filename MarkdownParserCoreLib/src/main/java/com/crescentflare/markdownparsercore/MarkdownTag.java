package com.crescentflare.markdownparsercore;

/**
 * Markdown core library: markdown tag
 * Used to indicate a piece of text within the given string being formatted by a markdown tag
 */
public class MarkdownTag
{
    public enum Type
    {
        Normal,
        TextStyle,
        Header
    }

    public static int FLAG_NONE = 0x0;
    public static int FLAG_ITALICS = 0x1;
    public static int FLAG_BOLD = 0x2;
    public static int FLAG_BOLDITALICS = FLAG_BOLD | FLAG_ITALICS;
    public static int FLAG_STRIKETHROUGH = 0x4;
    public static int FLAG_TEXTSTYLE = FLAG_ITALICS | FLAG_BOLD | FLAG_STRIKETHROUGH;
    public static int FLAG_ESCAPED = 0x40000000;

    public Type type = Type.Normal;
    public int flags = FLAG_NONE;
    public int startPosition = -1;
    public int endPosition = -1;
    public int startText = -1;
    public int endText = -1;
    public int sizeForType = 1;
    public int nativeInfo[] = null;
}
