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
    public void testFindTagsNewlines()
    {
        //Test case with given text and expected markdown tags
        String[] markdownTextLines = new String[]
        {
                "",
                "",
                "Text",
                "",
                "",
                "",
                "Another",
                ""
        };
        SimpleMarkdownTag[] expectedTags = new SimpleMarkdownTag[]
        {
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Text"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Another")
        };
        assertTags(markdownTextLines, expectedTags);
    }

    @Test
    public void testFindTagsHeaders()
    {
        //Test case with given text and expected markdown tags
        String[] markdownTextLines = new String[]
        {
                "Some text",
                "",
                "#First header",
                "Additional text",
                "And more",
                "",
                "  ##   Last header",
                "",
                "Final text"
        };
        SimpleMarkdownTag[] expectedTags = new SimpleMarkdownTag[]
        {
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Some text"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 2, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Header, 1, MarkdownTag.FLAG_NONE, "First header"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Additional text"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "And more"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 2, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Header, 2, MarkdownTag.FLAG_NONE, "Last header"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Final text")
        };
        assertTags(markdownTextLines, expectedTags);
    }

    @Test
    public void testFindTagsSections()
    {
        //Test case with given text and expected markdown tags
        String[] markdownTextLines = new String[]
        {
                "",
                "",
                "  #A strange indented header",
                "Another piece of text",
                "  ",
                "Text with a space separator to prevent paragraph",
                "",
                "New paragraph",
                "  # Sudden header",
                "Text",
                "",
                "* Bullet item",
                "* Second item",
                "  With some text",
                "",
                "New paragraph"
        };
        SimpleMarkdownTag[] expectedTags = new SimpleMarkdownTag[]
        {
                new SimpleMarkdownTag(MarkdownTag.Type.Header, 1, MarkdownTag.FLAG_NONE, "A strange indented header"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Another piece of text"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Text with a space separator to prevent paragraph"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "New paragraph"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 2, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Header, 1, MarkdownTag.FLAG_NONE, "Sudden header"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Text"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.UnorderedList, 1, MarkdownTag.FLAG_NONE, "Bullet item"),
                new SimpleMarkdownTag(MarkdownTag.Type.UnorderedList, 1, MarkdownTag.FLAG_NONE, "Second item"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "With some text"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "New paragraph")
        };
        assertTags(markdownTextLines, expectedTags);
    }

    @Test
    public void testFindTagsStyling()
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
                "New paragraph here with ~~strike through text in **bold**~~.",
                "",
                "+ A bullet list",
                "- Second bullet item",
                "  * A nested item",
                "* Third bullet item",
                "  1. Nested first item",
                "  2. Nested second item",
                "",
                "And some text afterwards."
        };
        SimpleMarkdownTag[] expectedTags = new SimpleMarkdownTag[]
        {
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Some text **before** the captions"),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, 2, MarkdownTag.FLAG_NONE, "before"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 2, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Header, 1, MarkdownTag.FLAG_NONE, "Caption 1"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Some lines of _styled and **double styled** text_ which should be formatted correctly."),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, 1, MarkdownTag.FLAG_NONE, "styled and **double styled** text"),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, 2, MarkdownTag.FLAG_NONE, "double styled"),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "Also new lines should work properly."),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 2, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Header, 3, MarkdownTag.FLAG_NONE, "Caption 3"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_ESCAPED, "The caption above is a bit smaller. Below add more lines to start a new *paragraph*."),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "New paragraph here with ~~strike through text in **bold**~~."),
                new SimpleMarkdownTag(MarkdownTag.Type.AlternativeTextStyle, 2, MarkdownTag.FLAG_NONE, "strike through text in **bold**"),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, 2, MarkdownTag.FLAG_NONE, "bold"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.UnorderedList, 1, MarkdownTag.FLAG_NONE, "A bullet list"),
                new SimpleMarkdownTag(MarkdownTag.Type.UnorderedList, 1, MarkdownTag.FLAG_NONE, "Second bullet item"),
                new SimpleMarkdownTag(MarkdownTag.Type.UnorderedList, 2, MarkdownTag.FLAG_NONE, "A nested item"),
                new SimpleMarkdownTag(MarkdownTag.Type.UnorderedList, 1, MarkdownTag.FLAG_NONE, "Third bullet item"),
                new SimpleMarkdownTag(MarkdownTag.Type.OrderedList, 2, MarkdownTag.FLAG_NONE, "Nested first item"),
                new SimpleMarkdownTag(MarkdownTag.Type.OrderedList, 2, MarkdownTag.FLAG_NONE, "Nested second item"),
                new SimpleMarkdownTag(MarkdownTag.Type.Paragraph, 1, MarkdownTag.FLAG_NONE, ""),
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "And some text afterwards.")
        };
        assertTags(markdownTextLines, expectedTags);
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
                new SimpleMarkdownTag(MarkdownTag.Type.Normal, MarkdownTag.FLAG_NONE, "A strange ***combination** tag*."),
                new SimpleMarkdownTag(MarkdownTag.Type.TextStyle, 2, MarkdownTag.FLAG_NONE, "*combination")
        };
        assertTags(markdownTextLines, expectedTags);
    }

    /**
     * Helpers
     */
    private void assertTags(String[] markdownTextLines, SimpleMarkdownTag[] expectedTags)
    {
        MarkdownParser parser = new MarkdownJavaParser();
        String markdownText = joinWithNewlines(markdownTextLines);
        MarkdownTag[] foundTags = parser.findTags(markdownText);
        for (int i = 0; i < foundTags.length && i < expectedTags.length; i++)
        {
            Assert.assertEquals(expectedTags[i], new SimpleMarkdownTag(markdownText, foundTags[i]));
        }
        Assert.assertEquals(expectedTags.length, foundTags.length);
    }

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
        private int weight;
        private String text;

        public SimpleMarkdownTag(MarkdownTag.Type type, int flags, String text)
        {
            this.type = type;
            this.flags = flags;
            this.weight = 0;
            this.text = text;
        }

        public SimpleMarkdownTag(MarkdownTag.Type type, int weight, int flags, String text)
        {
            this.type = type;
            this.weight = weight;
            this.flags = flags;
            this.text = text;
        }

        public SimpleMarkdownTag(String markdownText, MarkdownTag tag)
        {
            this.type = tag.type;
            this.weight = tag.weight;
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
            if (weight != that.weight)
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
                    ", weight=" + weight +
                    ", text='" + text + '\'' +
                    '}';
        }
    }
}
