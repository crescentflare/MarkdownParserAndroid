package com.crescentflare.markdownparser;

import com.crescentflare.markdownparsercore.MarkdownJavaParser;
import com.crescentflare.markdownparsercore.MarkdownNativeParser;
import com.crescentflare.markdownparsercore.MarkdownParser;
import com.crescentflare.markdownparsercore.MarkdownTag;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit test: core parser
 * Tests the core parser library (native and java)
 */
public class CoreParserTest
{
    /**
     * Tests
     */
    @Test
    public void testFindTags()
    {
        //Test case with given text and expected markdown tags
        String[] markdownTextLines = new String[]
        {
                "Some text **before** the captions",
                "# Caption 1",
                "Some lines of _styled and **double styled** text_ which should be formatted correctly.",
                "Also new lines should work properly.",
                "### Caption 3",
                "The caption above is a bit smaller. Below add more lines to start a new paragraph.",
                "",
                "New paragraph here."
        };
        SimpleMarkdownTag[] expectedTags = new SimpleMarkdownTag[]
        {
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, "Some text "),
                new SimpleMarkdownTag(MarkdownTag.Type.Bold, "before"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, " the captions"),
                new SimpleMarkdownTag(MarkdownTag.Type.Header1, "Caption 1"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, "Some lines of "),
                new SimpleMarkdownTag(MarkdownTag.Type.Italics, "styled and "),
                new SimpleMarkdownTag(MarkdownTag.Type.BoldItalics, "double styled"),
                new SimpleMarkdownTag(MarkdownTag.Type.Italics, " text"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, " which should be formatted correctly.\nAlso new lines should work properly."),
                new SimpleMarkdownTag(MarkdownTag.Type.Header3, "Caption 3"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, "The caption above is a bit smaller. Below add more lines to start a new paragraph."),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, "New paragraph here.")
        };

        //Parse and compare
        MarkdownParser parser = new MarkdownJavaParser();
        String markdownText = joinWithNewlines(markdownTextLines);
        MarkdownTag[] foundTags = parser.findTags(markdownText);
        for (int i = 0; i < foundTags.length && i < expectedTags.length; i++)
        {
            Assert.assertEquals(expectedTags[i], new SimpleMarkdownTag(markdownText, foundTags[i]));
        }
        Assert.assertEquals(expectedTags.length, foundTags.length);
    }

    /**
     * Helpers
     */
    private String joinWithNewlines(String[] stringArray)
    {
        String joinedText = "";
        boolean firstLine = true;
        for (String string : stringArray)
        {
            if (!firstLine)
            {
                joinedText += "\n";
            }
            joinedText += string;
            firstLine = false;
        }
        return joinedText;
    }

    /**
     * Helper class to simply compare tags
     */
    private static class SimpleMarkdownTag
    {
        private MarkdownTag.Type type;
        private String text;

        public SimpleMarkdownTag(MarkdownTag.Type type, String text)
        {
            this.type = type;
            this.text = text;
        }

        public SimpleMarkdownTag(String markdownText, MarkdownTag tag)
        {
            this.type = tag.type;
            this.text = new MarkdownJavaParser().extractText(markdownText, tag);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            SimpleMarkdownTag that = (SimpleMarkdownTag)o;
            if (type != that.type)
            {
                return false;
            }
            return text.equals(that.text);
        }

        @Override
        public String toString()
        {
            return "SimpleMarkdownTag{" +
                    "type=" + type +
                    ", text='" + text + '\'' +
                    '}';
        }
    }
}
