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
    int bytePos;
    int chrPos;
}STRING_POSITION;

typedef struct
{
    MARKDOWN_TAG_TYPE type;
    STRING_POSITION startPosition;
    STRING_POSITION endPosition;
    STRING_POSITION startText;
    STRING_POSITION endText;
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
    return 8;
}

void fillTagToArray(const MARKDOWN_TAG *tag, jint *ptr)
{
    if (tag && ptr)
    {
        ptr[0] = tag->type;
        ptr[1] = 0;
        ptr[2] = tag->startPosition.chrPos;
        ptr[3] = tag->endPosition.chrPos;
        ptr[4] = tag->startText.chrPos;
        ptr[5] = tag->endText.chrPos;
        ptr[6] = tag->startPosition.bytePos;
        ptr[7] = tag->startText.bytePos;
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
MARKDOWN_TAG *makeNormalTag(const char *markdownText, const STRING_POSITION position, const STRING_POSITION endPosition)
{
    MARKDOWN_TAG *tag = malloc(sizeof(MARKDOWN_TAG));
    if (tag == NULL)
    {
        return NULL;
    }
    tag->type = MARKDOWN_TAG_NORMAL;
    tag->startPosition = position;
    tag->startText = position;
    tag->endPosition = endPosition;
    tag->endText = endPosition;
    if (markdownText[endPosition.bytePos - 1] == '\n')
    {
        tag->endPosition.bytePos--;
        tag->endPosition.chrPos--;
        tag->endText.bytePos--;
        tag->endText.chrPos--;
    }
    if (tag->startPosition.chrPos < tag->endPosition.chrPos)
    {
        return tag;
    }
    free(tag);
    return NULL;
}

MARKDOWN_TAG *makeHeaderTag(const char *markdownText, const STRING_POSITION maxLength, const STRING_POSITION position)
{
    MARKDOWN_TAG *tag = malloc(sizeof(MARKDOWN_TAG));
    if (tag == NULL)
    {
        return NULL;
    }
    STRING_POSITION defaultPos = { -1, -1 };
    int headerSize = 0;
    tag->startPosition = position;
    tag->endPosition = defaultPos;
    tag->startText = defaultPos;
    tag->endText = defaultPos;
    STRING_POSITION i = position;
    while (i.bytePos < maxLength.bytePos)
    {
        char chr = markdownText[i.bytePos];
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
        if (tag->startText.chrPos < 0)
        {
            if (chr == '#' && headerSize < 6)
            {
                headerSize++;
            }
            else if (chr != ' ')
            {
                tag->startText = i;
                tag->type = MARKDOWN_TAG_HEADER1 + (headerSize - 1);
            }
        }
        else
        {
            if (chr == '\n')
            {
                tag->endPosition.bytePos = i.bytePos + charSize;
                tag->endPosition.chrPos = i.chrPos + 1;
                tag->endText = i;
                break;
            }
        }
        i.bytePos += charSize;
        i.chrPos++;
    }
    if (tag->endPosition.chrPos < 0)
    {
        tag->endPosition = maxLength;
        tag->endText = maxLength;
    }
    if (tag->startText.chrPos >= 0)
    {
        return tag;
    }
    free(tag);
    return NULL;
}

MARKDOWN_TAG *makeTextStyleTag(const char *markdownText, const STRING_POSITION maxLength, const STRING_POSITION position)
{
    MARKDOWN_TAG *tag = malloc(sizeof(MARKDOWN_TAG));
    if (tag == NULL)
    {
        return NULL;
    }
    STRING_POSITION defaultPos = { -1, -1 };
    int styleStrength = 0;
    int needStyleStrength = 0;
    char tagChr = markdownText[position.bytePos];
    tag->startPosition = position;
    tag->endPosition = defaultPos;
    tag->startText = defaultPos;
    tag->endText = defaultPos;
    STRING_POSITION i = position;
    while (i.bytePos < maxLength.bytePos)
    {
        char chr = markdownText[i.bytePos];
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
        if (tag->startText.chrPos < 0)
        {
            if (chr == tagChr && styleStrength < 3)
            {
                styleStrength++;
            }
            else
            {
                tag->startText = i;
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
                    tag->endPosition.bytePos = i.bytePos + charSize;
                    tag->endPosition.chrPos = i.chrPos + 1;
                    tag->endText.bytePos = i.bytePos + charSize - styleStrength;
                    tag->endText.chrPos = i.chrPos + 1 - styleStrength;
                    break;
                }
            }
            else if (needStyleStrength != styleStrength)
            {
                needStyleStrength = styleStrength;
            }
        }
        i.bytePos += charSize;
        i.chrPos++;
    }
    if (tag->startText.chrPos >= 0 && tag->endText.chrPos >= 0)
    {
        return tag;
    }
    free(tag);
    return NULL;
}

/**
 * Search for types of tag
 */
MARKDOWN_TAG *searchStylingTag(const char *markdownText, const STRING_POSITION maxLength, const STRING_POSITION position)
{
    char prevChr = 0;
    STRING_POSITION i = position;
    while (i.bytePos < maxLength.bytePos)
    {
        char chr = markdownText[i.bytePos];
        unsigned char charSize = UTF_CHAR_SIZE(chr);
        if (charSize != 1)
        {
            if (charSize == 0)
            {
                return NULL;
            }
            chr = PARSER_IGNORE_CHAR;
        }
        if (chr == '#' && (prevChr == '\n' || prevChr == 0 || i.chrPos == 0))
        {
            return makeHeaderTag(markdownText, maxLength, i);
        }
        if (chr == '*' || chr == '_')
        {
            return makeTextStyleTag(markdownText, maxLength, i);
        }
        prevChr = chr;
        i.bytePos += charSize;
        i.chrPos++;
    }
    return NULL;
}

void addNestedStylingTags(MARKDOWN_TAG_MEMORY_BLOCK *tagList, MARKDOWN_TAG *stylingTag, const char *markdownText, const STRING_POSITION maxLength)
{
    MARKDOWN_TAG *nestedTag = searchStylingTag(markdownText, maxLength, stylingTag->startText);
    if (nestedTag && !isHeaderTag(nestedTag) && nestedTag->endPosition.chrPos < stylingTag->endPosition.chrPos)
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
            beforeNestedTag->startPosition = stylingTag->startPosition;
            beforeNestedTag->startText = stylingTag->startText;
            beforeNestedTag->endPosition = nestedTag->startPosition;
            beforeNestedTag->endText = nestedTag->startPosition;
            if (beforeNestedTag->startText.chrPos < beforeNestedTag->endText.chrPos)
            {
                addTagToBlock(tagList, beforeNestedTag);
            }
            free(beforeNestedTag);
        }

        //Add found nested style tag recursively
        addNestedStylingTags(tagList, nestedTag, markdownText, maxLength);

        //Add part of found styling tag after nested style
        MARKDOWN_TAG *afterNestedTag = malloc(sizeof(MARKDOWN_TAG));
        if (afterNestedTag)
        {
            afterNestedTag->type = stylingTag->type;
            afterNestedTag->startPosition = nestedTag->endPosition;
            afterNestedTag->startText = nestedTag->endPosition;
            afterNestedTag->endPosition = stylingTag->endPosition;
            afterNestedTag->endText = stylingTag->endText;
            if (afterNestedTag->startText.chrPos < afterNestedTag->endText.chrPos)
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

void addParagraphedNormalTag(MARKDOWN_TAG_MEMORY_BLOCK *tagList, MARKDOWN_TAG *stylingTag, const char *markdownText, const STRING_POSITION maxLength)
{
    int newlineCount = 0;
    STRING_POSITION endParagraph = { 0, 0 };
    STRING_POSITION i = stylingTag->startText;
    while (i.chrPos < stylingTag->endText.chrPos)
    {
        char chr = markdownText[i.bytePos];
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
                endParagraph = i;
            }
            if (newlineCount > 1)
            {
                MARKDOWN_TAG *tag = malloc(sizeof(MARKDOWN_TAG));
                if (tag)
                {
                    tag->type = MARKDOWN_TAG_NORMAL;
                    tag->startPosition = stylingTag->startPosition;
                    tag->startText = stylingTag->startText;
                    tag->endPosition = endParagraph;
                    tag->endText = endParagraph;
                    addTagToBlock(tagList, tag);
                    stylingTag->startPosition.bytePos = i.bytePos + 1;
                    stylingTag->startPosition.chrPos = i.chrPos + 1;
                    stylingTag->startText.bytePos = i.bytePos + 1;
                    stylingTag->startText.chrPos = i.chrPos + 1;
                    free(tag);
                }
                newlineCount = 0;
            }
        }
        else if (chr != ' ')
        {
            newlineCount = 0;
        }
        i.bytePos += charSize;
        i.chrPos++;
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
    STRING_POSITION maxLength = { strlen(markdownText), (*env)->GetStringLength(env, markdownText_) };
    STRING_POSITION position = { 0, 0 };
    char processing;
    do
    {
        MARKDOWN_TAG *stylingTag = searchStylingTag(markdownText, maxLength, position);
        if (stylingTag)
        {
            if (stylingTag->startPosition.chrPos > position.chrPos)
            {
                MARKDOWN_TAG *tag = makeNormalTag(markdownText, position, stylingTag->startPosition);
                if (tag)
                {
                    addParagraphedNormalTag(&tagList, tag, markdownText, maxLength);
                }
            }
            if (isTextStyleTag(stylingTag))
            {
                addNestedStylingTags(&tagList, stylingTag, markdownText, maxLength);
            }
            else
            {
                addTagToBlock(&tagList, stylingTag);
            }
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
    if (position.chrPos < maxLength.chrPos)
    {
        MARKDOWN_TAG *tag = makeNormalTag(markdownText, position, maxLength);
        if (tag)
        {
            addParagraphedNormalTag(&tagList, tag, markdownText, maxLength);
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
