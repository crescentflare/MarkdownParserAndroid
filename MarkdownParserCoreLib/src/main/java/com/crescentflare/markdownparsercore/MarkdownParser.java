package com.crescentflare.markdownparsercore;

/**
 * Markdown core library: interface class
 * The interface to do the core (low-level) markdown parsing
 * It returns ranges for the markdown tags which is used within the library
 * Use manually if the output needs to be highly customizable
 */
public interface MarkdownParser
{
    MarkdownTag[] findTags(String markdownText);
    String extractText(String markdownText, MarkdownTag tag);
    String extractFull(String markdownText, MarkdownTag tag);
}
