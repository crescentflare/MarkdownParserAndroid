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
        Bold,
        Italics,
        BoldItalics,
        Header1,
        Header2,
        Header3,
        Header4,
        Header5,
        Header6;

        public static Type headerForSize(int size)
        {
            switch (size)
            {
                case 1:
                    return Header1;
                case 2:
                    return Header2;
                case 3:
                    return Header3;
                case 4:
                    return Header4;
                case 5:
                    return Header5;
                case 6:
                    return Header6;
            }
            return Normal;
        }

        public static Type textStyleForStrength(int strength)
        {
            switch (strength)
            {
                case 1:
                    return Italics;
                case 2:
                    return Bold;
                case 3:
                    return BoldItalics;
            }
            return Normal;
        }

        public boolean isHeader()
        {
            return this == Header1 || this == Header2 || this == Header3 || this == Header4 || this == Header5 || this == Header6;
        }

        public boolean isTextStyle()
        {
            return this == Bold || this == Italics || this == BoldItalics;
        }
    }

    public Type type = Type.Normal;
    public int startPosition = -1;
    public int endPosition = -1;
    public int startText = -1;
    public int endText = -1;
    public int nativeInfo[] = null;
}
