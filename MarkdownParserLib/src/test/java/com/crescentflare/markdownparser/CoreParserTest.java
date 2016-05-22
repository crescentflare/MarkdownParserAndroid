package com.crescentflare.markdownparser;

import com.crescentflare.markdownparsercore.MarkdownJavaParser;
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
                "The caption above is a bit smaller. Below add more lines to start a new \\*paragraph\\*.",
                "",
                "New paragraph here with ~~strike through text in **bold**~~."
        };
        SimpleMarkdownTag[] expectedTags = new SimpleMarkdownTag[]
        {
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Some text "),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, MarkdownTag.FLAG_BOLD, "before"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, " the captions"),
                new SimpleMarkdownTag(MarkdownTag.Type.Header, 1, MarkdownTag.FLAG_NONE, "Caption 1"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Some lines of "),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, MarkdownTag.FLAG_ITALICS, "styled and "),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, MarkdownTag.FLAG_BOLDITALICS, "double styled"),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, MarkdownTag.FLAG_ITALICS, " text"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, " which should be formatted correctly.\nAlso new lines should work properly."),
                new SimpleMarkdownTag(MarkdownTag.Type.Header, 3, MarkdownTag.FLAG_NONE, "Caption 3"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_ESCAPED, "The caption above is a bit smaller. Below add more lines to start a new *paragraph*."),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "New paragraph here with "),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, MarkdownTag.FLAG_STRIKETHROUGH, "strike through text in "),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, MarkdownTag.FLAG_STRIKETHROUGH | MarkdownTag.FLAG_BOLD, "bold"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, ".")
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

    @Test
    public void testFindTagsEdgeCases()
    {
        //Test case with given text and expected markdown tags
        String[] markdownTextLines = new String[]
        {
                "A strange ***combination** tag*."
        };
        SimpleMarkdownTag[] expectedTags = new SimpleMarkdownTag[]
        {
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "A strange "),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, MarkdownTag.FLAG_BOLD, "*combination"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, " tag*.")
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
        private int flags;
        private int sizeForType;
        private String text;

        public SimpleMarkdownTag(MarkdownTag.Type type, int flags, String text)
        {
            this.type = type;
            this.flags = flags;
            this.sizeForType = 1;
            this.text = text;
        }

        public SimpleMarkdownTag(MarkdownTag.Type type, int sizeForType, int flags, String text)
        {
            this.type = type;
            this.sizeForType = sizeForType;
            this.flags = flags;
            this.text = text;
        }

        public SimpleMarkdownTag(String markdownText, MarkdownTag tag)
        {
            this.type = tag.type;
            this.sizeForType = tag.sizeForType;
            this.flags = tag.flags;
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
            if (sizeForType != that.sizeForType)
            {
                return false;
            }
            if (flags != that.flags)
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
                    ", flags=" + flags +
                    ", sizeForType=" + sizeForType +
                    ", text='" + text + '\'' +
                    '}';
        }
    }
}
