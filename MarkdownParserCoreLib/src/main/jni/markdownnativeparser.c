#include <jni.h>
#include <stdlib.h>

/**
 * Macros
 */
#define UTF_CHAR_SIZE(chr) (unsigned char)((chr & 0x80) == 0x0 ? 1 : ((chr & 0xE0) == 0xC0 ? 2 : ((chr & 0xF0) == 0xE0 ? 3 : ((chr & 0xF8) == 0xF0 ? 1 : 0))))

/**
 * Constants
 */
static const char PARSER_IGNORE_CHAR = 'A';

/**
 * Markdown tag type enum
 */
typedef enum
{
    MARKDOWN_TAG_NORMAL = 0,
    MARKDOWN_TAG_ITALICS,
    MARKDOWN_TAG_BOLD,
    MARKDOWN_TAG_BOLDITALICS,
    MARKDOWN_TAG_HEADER1,
    MARKDOWN_TAG_HEADER2,
    MARKDOWN_TAG_HEADER3,
    MARKDOWN_TAG_HEADER4,
    MARKDOWN_TAG_HEADER5,
    MARKDOWN_TAG_HEADER6
}MARKDOWN_TAG_TYPE;

/**
 * Structures for native markdown tag and memory block
 */
typedef struct
{
    MARKDOWN_TAG_TYPE type;
    int startPosition;
    int endPosition;
    int startText;
    int endText;
    int startPositionNative;
    int endPositionNative;
    int startTextNative;
    int endTextNative;
}MARKDOWN_TAG;

typedef struct
{
    void *memoryBlock;
    size_t allocatedMemory;
    int tagCount;
}MARKDOWN_TAG_MEMORY_BLOCK;

/**
 * Utility functions for markdown tags
 */
const unsigned char tagFieldCount()
{
    return 9;
}

void fillTagToArray(const MARKDOWN_TAG *tag, jint *ptr)
{
    if (tag && ptr)
    {
        ptr[0] = tag->type;
        ptr[1] = tag->startPositionNative;
        ptr[2] = tag->startPosition;
        ptr[3] = tag->endPositionNative;
        ptr[4] = tag->endPosition;
        ptr[5] = tag->startTextNative;
        ptr[6] = tag->startText;
        ptr[7] = tag->endTextNative;
        ptr[8] = tag->endText;
    }
}

const char isHeaderTag(const MARKDOWN_TAG *tag)
{
    if (tag)
    {
        return tag->type >= MARKDOWN_TAG_HEADER1 && tag->type <= MARKDOWN_TAG_HEADER6;
    }
    return NULL;
}

const char isTextStyleTag(const MARKDOWN_TAG *tag)
{
    if (tag)
    {
        return tag->type == MARKDOWN_TAG_ITALICS || tag->type == MARKDOWN_TAG_BOLD || tag->type == MARKDOWN_TAG_BOLDITALICS;
    }
    return NULL;
}

/**
 * Functions to handle the memory block of found markdown tags
 */
MARKDOWN_TAG_MEMORY_BLOCK newMarkdownTagMemoryBlock()
{
    MARKDOWN_TAG_MEMORY_BLOCK block;
    block.allocatedMemory = 1024;
    block.memoryBlock = malloc(block.allocatedMemory);
    block.tagCount = 0;
    return block;
}

void deleteMarkdownTagMemoryBlock(MARKDOWN_TAG_MEMORY_BLOCK *block)
{
    if (!block)
    {
        return;
    }
    if (block->memoryBlock)
    {
        free(block->memoryBlock);
        block->memoryBlock = NULL;
    }
    block->tagCount = 0;
    block->allocatedMemory = 0;
}

void addTagToBlock(MARKDOWN_TAG_MEMORY_BLOCK *block, const MARKDOWN_TAG *tag)
{
    if (!block)
    {
        return;
    }
    if (!block->memoryBlock)
    {
        return;
    }
    int tagSize = (sizeof(MARKDOWN_TAG) + 7) / 8 * 8;
    int needBlockSize = (block->tagCount + 1) * tagSize;
    if (needBlockSize > block->allocatedMemory)
    {
        size_t wantAllocatedMemory = block->allocatedMemory + 1024;
        block->memoryBlock = realloc(block->memoryBlock, wantAllocatedMemory);
        if (!block->memoryBlock)
        {
            return;
        }
        block->allocatedMemory = wantAllocatedMemory;
    }
    memcpy(&((unsigned char *)block->memoryBlock)[block->tagCount * tagSize], tag, sizeof(MARKDOWN_TAG));
    block->tagCount++;
}

MARKDOWN_TAG *tagFromBlock(const MARKDOWN_TAG_MEMORY_BLOCK *block, const int position)
{
    if (position < 0 || position > block->tagCount)
    {
        return NULL;
    }
    int tagSize = (sizeof(MARKDOWN_TAG) + 7) / 8 * 8;
    return (MARKDOWN_TAG *)&((unsigned char *)block->memoryBlock)[position * tagSize];
}

/**
 * Find tags based on start position and iterating to find the end position
 */
MARKDOWN_TAG *makeNormalTag(const char *markdownText, const int positionNative, const int position, const int endPositionNative, const int endPosition)
{
    MARKDOWN_TAG *tag = malloc(sizeof(MARKDOWN_TAG));
    if (tag == NULL)
    {
        return NULL;
    }
    tag->type = MARKDOWN_TAG_NORMAL;
    tag->startPositionNative = positionNative;
    tag->startPosition = position;
    tag->startTextNative = positionNative;
    tag->startText = position;
    tag->endPositionNative = endPositionNative;
    tag->endPosition = endPosition;
    tag->endTextNative = endPositionNative;
    tag->endText = endPosition;
    if (markdownText[endPositionNative - 1] == '\n')
    {
        tag->endPositionNative--;
        tag->endPosition--;
        tag->endTextNative--;
        tag->endText--;
    }
    if (tag->startPosition < tag->endPosition)
    {
        return tag;
    }
    free(tag);
    return NULL;
}

MARKDOWN_TAG *makeHeaderTag(const char *markdownText, const int maxLengthNative, const int positionNative, const int maxLength, int position)
{
    MARKDOWN_TAG *tag = malloc(sizeof(MARKDOWN_TAG));
    if (tag == NULL)
    {
        return NULL;
    }
    int headerSize = 0;
    tag->startPositionNative = positionNative;
    tag->startPosition = position;
    tag->endPositionNative = -1;
    tag->endPosition = -1;
    tag->startTextNative = -1;
    tag->startText = -1;
    tag->endTextNative = -1;
    tag->endText = -1;
    int i = positionNative;
    while (i < maxLengthNative)
    {
        char chr = markdownText[i];
        unsigned char charSize = UTF_CHAR_SIZE(chr);
        if (charSize != 1)
        {
            if (charSize == 0)
            {
                free(tag);
                return NULL;
            }
            chr = PARSER_IGNORE_CHAR;
        }
        if (tag->startTextNative < 0)
        {
            if (chr == '#' && headerSize < 6)
            {
                headerSize++;
            }
            else if (chr != ' ')
            {
                tag->startTextNative = i;
                tag->startText = position;
                tag->type = MARKDOWN_TAG_HEADER1 + (headerSize - 1);
            }
        }
        else
        {
            if (chr == '\n')
            {
                tag->endPositionNative = i + charSize;
                tag->endPosition = position + 1;
                tag->endTextNative = i;
                tag->endText = position;
                break;
            }
        }
        i += charSize;
        position++;
    }
    if (tag->endPositionNative < 0)
    {
        tag->endPositionNative = maxLengthNative;
        tag->endPosition = maxLength;
        tag->endTextNative = maxLengthNative;
        tag->endText = maxLength;
    }
    if (tag->startTextNative >= 0)
    {
        return tag;
    }
    free(tag);
    return NULL;
}

MARKDOWN_TAG *makeTextStyleTag(const char *markdownText, const int maxLengthNative, const int positionNative, const int maxLength, int position)
{
    MARKDOWN_TAG *tag = malloc(sizeof(MARKDOWN_TAG));
    if (tag == NULL)
    {
        return NULL;
    }
    int styleStrength = 0;
    int needStyleStrength = 0;
    char tagChr = markdownText[positionNative];
    tag->startPositionNative = positionNative;
    tag->startPosition = position;
    tag->endPositionNative = -1;
    tag->endPosition = -1;
    tag->startTextNative = -1;
    tag->startText = -1;
    tag->endTextNative = -1;
    tag->endText = -1;
    int i = positionNative;
    while (i < maxLengthNative)
    {
        char chr = markdownText[i];
        unsigned char charSize = UTF_CHAR_SIZE(chr);
        if (charSize != 1)
        {
            if (charSize == 0)
            {
                free(tag);
                return NULL;
            }
            chr = PARSER_IGNORE_CHAR;
        }
        if (tag->startText < 0)
        {
            if (chr == tagChr && styleStrength < 3)
            {
                styleStrength++;
            }
            else
            {
                tag->startTextNative = i;
                tag->startText = position;
                tag->type = MARKDOWN_TAG_ITALICS + (styleStrength - 1);
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
                    tag->endPositionNative = i + charSize;
                    tag->endPosition = position + 1;
                    tag->endTextNative = i + charSize - styleStrength;
                    tag->endText = position + 1 - styleStrength;
                    break;
                }
            }
            else if (needStyleStrength != styleStrength)
            {
                needStyleStrength = styleStrength;
            }
        }
        i += charSize;
        position++;
    }
    if (tag->startText >= 0 && tag->endText >= 0)
    {
        return tag;
    }
    free(tag);
    return NULL;
}

/**
 * Search for types of tag
 */
MARKDOWN_TAG *searchStylingTag(const char *markdownText, const int maxLengthNative, const int positionNative, const int maxLength, int position)
{
    char prevChr = 0;
    int i = positionNative;
    while (i < maxLengthNative)
    {
        char chr = markdownText[i];
        unsigned char charSize = UTF_CHAR_SIZE(chr);
        if (charSize != 1)
        {
            if (charSize == 0)
            {
                return NULL;
            }
            chr = PARSER_IGNORE_CHAR;
        }
        if (chr == '#' && (prevChr == '\n' || prevChr == 0 || i == 0))
        {
            return makeHeaderTag(markdownText, maxLengthNative, i, maxLength, position);
        }
        if (chr == '*' || chr == '_')
        {
            return makeTextStyleTag(markdownText, maxLengthNative, i, maxLength, position);
        }
        prevChr = chr;
        i += charSize;
        position++;
    }
    return NULL;
}

void addNestedStylingTags(MARKDOWN_TAG_MEMORY_BLOCK *tagList, MARKDOWN_TAG *stylingTag, const char *markdownText, const int maxLengthNative, const int maxLength)
{
    MARKDOWN_TAG *nestedTag = searchStylingTag(markdownText, maxLengthNative, stylingTag->startTextNative, maxLength, stylingTag->startText);
    if (nestedTag && !isHeaderTag(nestedTag) && nestedTag->endPosition < stylingTag->endPosition)
    {
        //Combine found styling tag with nested tag
        if ((stylingTag->type == MARKDOWN_TAG_ITALICS && nestedTag->type == MARKDOWN_TAG_BOLD) || (stylingTag->type == MARKDOWN_TAG_BOLD && nestedTag->type == MARKDOWN_TAG_ITALICS))
        {
            nestedTag->type = MARKDOWN_TAG_BOLDITALICS;
        }

        //Add part of found styling tag before nested style
        MARKDOWN_TAG *beforeNestedTag = malloc(sizeof(MARKDOWN_TAG));
        if (beforeNestedTag)
        {
            beforeNestedTag->type = stylingTag->type;
            beforeNestedTag->startPositionNative = stylingTag->startPositionNative;
            beforeNestedTag->startPosition = stylingTag->startPosition;
            beforeNestedTag->startTextNative = stylingTag->startTextNative;
            beforeNestedTag->startText = stylingTag->startText;
            beforeNestedTag->endPositionNative = nestedTag->startPositionNative;
            beforeNestedTag->endPosition = nestedTag->startPosition;
            beforeNestedTag->endTextNative = nestedTag->startPositionNative;
            beforeNestedTag->endText = nestedTag->startPosition;
            if (beforeNestedTag->startText < beforeNestedTag->endText)
            {
                addTagToBlock(tagList, beforeNestedTag);
            }
            free(beforeNestedTag);
        }

        //Add found nested style tag recursively
        addNestedStylingTags(tagList, nestedTag, markdownText, maxLengthNative, maxLength);

        //Add part of found styling tag after nested style
        MARKDOWN_TAG *afterNestedTag = malloc(sizeof(MARKDOWN_TAG));
        if (afterNestedTag)
        {
            afterNestedTag->type = stylingTag->type;
            afterNestedTag->startPositionNative = nestedTag->endPositionNative;
            afterNestedTag->startPosition = nestedTag->endPosition;
            afterNestedTag->startTextNative = nestedTag->endPositionNative;
            afterNestedTag->startText = nestedTag->endPosition;
            afterNestedTag->endPositionNative = stylingTag->endPositionNative;
            afterNestedTag->endPosition = stylingTag->endPosition;
            afterNestedTag->endTextNative = stylingTag->endTextNative;
            afterNestedTag->endText = stylingTag->endText;
            if (afterNestedTag->startText < afterNestedTag->endText)
            {
                addTagToBlock(tagList, afterNestedTag);
            }
            free(afterNestedTag);
        }
    }
    else
    {
        addTagToBlock(tagList, stylingTag);
    }
    if (nestedTag)
    {
        free(nestedTag);
    }
}

void addParagraphedNormalTag(MARKDOWN_TAG_MEMORY_BLOCK *tagList, MARKDOWN_TAG *stylingTag, const char *markdownText, const int maxLengthNative, const int maxLength)
{
    int newlineCount = 0;
    int endParagraphNative = 0;
    int endParagraph = 0;
    int i = stylingTag->startTextNative;
    int position = stylingTag->startText;
    while (i < stylingTag->endTextNative)
    {
        char chr = markdownText[i];
        unsigned char charSize = UTF_CHAR_SIZE(chr);
        if (charSize != 1)
        {
            if (charSize == 0)
            {
                return;
            }
            chr = PARSER_IGNORE_CHAR;
        }
        if (chr == '\n')
        {
            newlineCount++;
            if (newlineCount == 1)
            {
                endParagraphNative = position;
                endParagraph = i;
            }
            if (newlineCount > 1)
            {
                MARKDOWN_TAG *tag = malloc(sizeof(MARKDOWN_TAG));
                if (tag)
                {
                    tag->type = MARKDOWN_TAG_NORMAL;
                    tag->startPositionNative = stylingTag->startPositionNative;
                    tag->startPosition = stylingTag->startPosition;
                    tag->startTextNative = stylingTag->startTextNative;
                    tag->startText = stylingTag->startText;
                    tag->endPositionNative = endParagraphNative;
                    tag->endPosition = endParagraph;
                    tag->endTextNative = endParagraphNative;
                    tag->endText = endParagraph;
                    addTagToBlock(tagList, tag);
                    stylingTag->startPositionNative = position + 1;
                    stylingTag->startPosition = i + 1;
                    stylingTag->startTextNative = position + 1;
                    stylingTag->startText = i + 1;
                    free(tag);
                }
                newlineCount = 0;
            }
        }
        else if (chr != ' ')
        {
            newlineCount = 0;
        }
        i += charSize;
        position++;
    }
    addTagToBlock(tagList, stylingTag);
}

/**
 * JNI function to find all supported markdown tags
 */
JNIEXPORT jintArray JNICALL
Java_com_crescentflare_markdownparsercore_MarkdownNativeParser_findNativeTags(JNIEnv *env, jobject instance, jstring markdownText_)
{
    //Loop over string and find tags
    MARKDOWN_TAG_MEMORY_BLOCK tagList = newMarkdownTagMemoryBlock();
    const char *markdownText = (*env)->GetStringUTFChars(env, markdownText_, 0);
    int maxLengthNative = strlen(markdownText);
    int maxLength = (*env)->GetStringLength(env, markdownText_);
    int positionNative = 0;
    int position = 0;
    char processing;
    do
    {
        MARKDOWN_TAG *stylingTag = searchStylingTag(markdownText, maxLengthNative, positionNative, maxLength, position);
        if (stylingTag)
        {
            if (stylingTag->startPosition > position)
            {
                MARKDOWN_TAG *tag = makeNormalTag(markdownText, positionNative, position, stylingTag->startPositionNative, stylingTag->startPosition);
                if (tag)
                {
                    addParagraphedNormalTag(&tagList, tag, markdownText, maxLengthNative, maxLength);
                }
            }
            if (isTextStyleTag(stylingTag))
            {
                addNestedStylingTags(&tagList, stylingTag, markdownText, maxLengthNative, maxLength);
            }
            else
            {
                addTagToBlock(&tagList, stylingTag);
            }
            positionNative = stylingTag->endPositionNative;
            position = stylingTag->endPosition;
            processing = 1;
            free(stylingTag);
        }
        else
        {
            processing = 0;
        }
    }
    while (processing);

    //Add final tag if there is a bit of string left to handle
    if (position < maxLength)
    {
        MARKDOWN_TAG *tag = makeNormalTag(markdownText, positionNative, position, maxLengthNative, maxLength);
        if (tag)
        {
            addParagraphedNormalTag(&tagList, tag, markdownText, maxLengthNative, maxLength);
        }
    }

    //Convert tags into a java array, clean up and return
    jintArray returnArray = NULL;
    jint *convertedValues = malloc(tagList.tagCount * tagFieldCount() * sizeof(jint));
    if (convertedValues)
    {
        returnArray = (*env)->NewIntArray(env, tagList.tagCount * tagFieldCount());
        int i;
        for (i = 0; i < tagList.tagCount; i++)
        {
            fillTagToArray(tagFromBlock(&tagList, i), &convertedValues[i * tagFieldCount()]);
        }
        (*env)->SetIntArrayRegion(env, returnArray, 0, tagList.tagCount * tagFieldCount(), convertedValues);
    }
    (*env)->ReleaseStringUTFChars(env, markdownText_, markdownText);
    deleteMarkdownTagMemoryBlock(&tagList);
    return returnArray;
}
