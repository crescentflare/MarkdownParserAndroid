#include <jni.h>
#include <stdlib.h>

/**
 * Constant, struct, macro's and utility functions to traverse UTF8 strings
 */
static const char PARSER_IGNORE_CHAR = 'A';

typedef struct
{
    int bytePos;
    int chrPos;
}STRING_POSITION;

#define UTF_CHAR_SIZE(chr) (unsigned char)((chr & 0x80) == 0x0 ? 1 : ((chr & 0xE0) == 0xC0 ? 2 : ((chr & 0xF0) == 0xE0 ? 3 : ((chr & 0xF8) == 0xF0 ? 4 : 1))))
#define INCREASE_STRING_POSITION(pos, txt) (pos.bytePos += UTF_CHAR_SIZE(txt[pos.bytePos]), pos.chrPos++)


/**
 * Constants, enum, struct and utility functions for markdown tags
 */
static const int MARKDOWN_FLAG_NONE = 0x0;
static const int MARKDOWN_FLAG_ITALICS = 0x1;
static const int MARKDOWN_FLAG_BOLD = 0x2;
static const int MARKDOWN_FLAG_BOLDITALICS = 0x3; //MARKDOWN_FLAG_BOLD | MARKDOWN_FLAG_ITALICS;
static const int MARKDOWN_FLAG_STRIKETHROUGH = 0x4;
static const int MARKDOWN_FLAG_TEXTSTYLE = 0x7; //MARKDOWN_FLAG_ITALICS | MARKDOWN_FLAG_BOLD | MARKDOWN_FLAG_STRIKETHROUGH;
static const int MARKDOWN_FLAG_ESCAPED = 0x40000000;

typedef enum
{
    MARKDOWN_TAG_NORMAL = 0,
    MARKDOWN_TAG_TEXTSTYLE,
    MARKDOWN_TAG_HEADER
}MARKDOWN_TAG_TYPE;

typedef struct
{
    MARKDOWN_TAG_TYPE type;
    int flags;
    STRING_POSITION startPosition;
    STRING_POSITION endPosition;
    STRING_POSITION startText;
    STRING_POSITION endText;
    int sizeForType;
}MARKDOWN_TAG;

const unsigned char tagFieldCount()
{
    return 9;
}

void fillTagToArray(const MARKDOWN_TAG *tag, jint *ptr)
{
    if (tag && ptr)
    {
        ptr[0] = tag->type;
        ptr[1] = tag->flags;
        ptr[2] = tag->startPosition.chrPos;
        ptr[3] = tag->endPosition.chrPos;
        ptr[4] = tag->startText.chrPos;
        ptr[5] = tag->endText.chrPos;
        ptr[6] = tag->startPosition.bytePos;
        ptr[7] = tag->startText.bytePos;
        ptr[8] = tag->sizeForType;
    }
}

void tagDefaults(MARKDOWN_TAG *tag)
{
    STRING_POSITION defaultPosition = { -1, -1 };
    tag->type = MARKDOWN_TAG_NORMAL;
    tag->flags = 0;
    tag->startPosition = defaultPosition;
    tag->endPosition = defaultPosition;
    tag->startText = defaultPosition;
    tag->endText = defaultPosition;
    tag->sizeForType = 1;
}

int flagsForTextStrength(int textStrength)
{
    switch (textStrength)
    {
        case 1:
            return MARKDOWN_FLAG_ITALICS;
        case 2:
            return MARKDOWN_FLAG_BOLD;
        case 3:
            return MARKDOWN_FLAG_BOLDITALICS;
    }
    return 0;
}


/**
 * Struct and functions to handle the memory block of found markdown tags
 */
typedef struct
{
    void *memoryBlock;
    size_t allocatedMemory;
    int tagCount;
}MARKDOWN_TAG_MEMORY_BLOCK;

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
    tagDefaults(tag);
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
    tagDefaults(tag);
    int headerSize = 0;
    tag->startPosition = position;
    STRING_POSITION i;
    for (i = position; i.bytePos < maxLength.bytePos; INCREASE_STRING_POSITION(i, markdownText))
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
        if (chr == '\\' && markdownText[i.bytePos + 1] != '\n')
        {
            tag->flags |= MARKDOWN_FLAG_ESCAPED;
            i.bytePos += charSize;
            i.chrPos++;
            continue;
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
                tag->type = MARKDOWN_TAG_HEADER;
                tag->sizeForType = headerSize;
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
    tagDefaults(tag);
    int styleStrength = 0;
    int needStyleStrength = 0;
    char tagChr = markdownText[position.bytePos];
    tag->startPosition = position;
    STRING_POSITION i;
    for (i = position; i.bytePos < maxLength.bytePos; INCREASE_STRING_POSITION(i, markdownText))
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
        if (chr == '\\' && markdownText[i.bytePos + 1] != '\n')
        {
            tag->flags |= MARKDOWN_FLAG_ESCAPED;
            i.bytePos += charSize;
            i.chrPos++;
            continue;
        }
        if (tag->startText.chrPos < 0)
        {
            if (chr == tagChr && ((tagChr != '~' && styleStrength < 3) || (tagChr == '~' && styleStrength < 2)))
            {
                styleStrength++;
            }
            else if (tagChr != '~' || styleStrength == 2)
            {
                tag->startText = i;
                tag->type = MARKDOWN_TAG_TEXTSTYLE;
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
                    if (tagChr != '~')
                    {
                        tag->flags |= flagsForTextStrength(styleStrength);
                    }
                    else
                    {
                        tag->flags |= MARKDOWN_FLAG_STRIKETHROUGH;
                    }
                    tag->endPosition.bytePos = i.bytePos + charSize;
                    tag->endPosition.chrPos = i.chrPos + 1;
                    tag->endText.bytePos = i.bytePos + charSize - styleStrength;
                    tag->endText.chrPos = i.chrPos + 1 - styleStrength;
                    break;
                }
            }
            else if (needStyleStrength != styleStrength)
            {
                if (tagChr != '~' && styleStrength - needStyleStrength > 0)
                {
                    tag->flags |= flagsForTextStrength(styleStrength - needStyleStrength);
                    tag->startText.bytePos -= needStyleStrength;
                    tag->startText.chrPos -= needStyleStrength;
                    tag->endPosition = i;
                    tag->endText.bytePos = i.bytePos - (styleStrength - needStyleStrength);
                    tag->endText.chrPos = i.bytePos - (styleStrength - needStyleStrength);
                    break;
                }
                else
                {
                    needStyleStrength = styleStrength;
                }
            }
        }
    }
    if (tag->startText.chrPos >= 0 && tag->endText.chrPos >= 0 && (tag->flags & MARKDOWN_FLAG_TEXTSTYLE) > 0)
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
    STRING_POSITION i;
    for (i = position; i.bytePos < maxLength.bytePos; INCREASE_STRING_POSITION(i, markdownText))
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
        if (chr == '\\')
        {
            i.bytePos += charSize;
            i.chrPos++;
            continue;
        }
        if (chr == '#' && (prevChr == '\n' || prevChr == 0 || i.chrPos == 0))
        {
            return makeHeaderTag(markdownText, maxLength, i);
        }
        if (chr == '*' || chr == '_' || chr == '~')
        {
            return makeTextStyleTag(markdownText, maxLength, i);
        }
        prevChr = chr;
    }
    return NULL;
}

void addNestedStylingTags(MARKDOWN_TAG_MEMORY_BLOCK *tagList, MARKDOWN_TAG *stylingTag, const char *markdownText, const STRING_POSITION maxLength)
{
    MARKDOWN_TAG *nestedTag = searchStylingTag(markdownText, stylingTag->endText, stylingTag->startText);
    if (nestedTag && nestedTag->type != MARKDOWN_TAG_HEADER && nestedTag->endPosition.chrPos < stylingTag->endPosition.chrPos)
    {
        //Combine found styling tag with nested tag
        nestedTag->flags |= stylingTag->flags & MARKDOWN_FLAG_TEXTSTYLE;

        //Add part of found styling tag before nested style
        MARKDOWN_TAG *beforeNestedTag = malloc(sizeof(MARKDOWN_TAG));
        if (beforeNestedTag)
        {
            tagDefaults(beforeNestedTag);
            beforeNestedTag->type = stylingTag->type;
            beforeNestedTag->startPosition = stylingTag->startPosition;
            beforeNestedTag->startText = stylingTag->startText;
            beforeNestedTag->endPosition = nestedTag->startPosition;
            beforeNestedTag->endText = nestedTag->startPosition;
            beforeNestedTag->flags = stylingTag->flags;
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
            tagDefaults(afterNestedTag);
            afterNestedTag->type = stylingTag->type;
            afterNestedTag->startPosition = nestedTag->endPosition;
            afterNestedTag->startText = nestedTag->endPosition;
            afterNestedTag->endPosition = stylingTag->endPosition;
            afterNestedTag->endText = stylingTag->endText;
            afterNestedTag->flags = stylingTag->flags;
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
    char foundEscapedChar = 0;
    int newlineCount = 0;
    STRING_POSITION endParagraph = { 0, 0 };
    STRING_POSITION i;
    for (i = stylingTag->startText; i.chrPos < stylingTag->endText.chrPos; INCREASE_STRING_POSITION(i, markdownText))
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
        if (chr == '\\' && markdownText[i.bytePos + 1] != '\n')
        {
            foundEscapedChar = 1;
            continue;
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
                    tagDefaults(tag);
                    tag->type = MARKDOWN_TAG_NORMAL;
                    tag->startPosition = stylingTag->startPosition;
                    tag->startText = stylingTag->startText;
                    tag->endPosition = endParagraph;
                    tag->endText = endParagraph;
                    if (foundEscapedChar)
                    {
                        tag->flags |= MARKDOWN_FLAG_ESCAPED;
                    }
                    addTagToBlock(tagList, tag);
                    stylingTag->startPosition.bytePos = i.bytePos + 1;
                    stylingTag->startPosition.chrPos = i.chrPos + 1;
                    stylingTag->startText.bytePos = i.bytePos + 1;
                    stylingTag->startText.chrPos = i.chrPos + 1;
                    free(tag);
                }
                foundEscapedChar = 0;
                newlineCount = 0;
            }
        }
        else if (chr != ' ')
        {
            newlineCount = 0;
        }
    }
    if (foundEscapedChar)
    {
        stylingTag->flags |= MARKDOWN_FLAG_ESCAPED;
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
            if (stylingTag->type == MARKDOWN_TAG_TEXTSTYLE)
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
        free(convertedValues);
    }
    (*env)->ReleaseStringUTFChars(env, markdownText_, markdownText);
    deleteMarkdownTagMemoryBlock(&tagList);
    return returnArray;
}


/**
 * JNI function to extract an escaped string
 */
JNIEXPORT jstring JNICALL
Java_com_crescentflare_markdownparsercore_MarkdownNativeParser_escapedSubstring(JNIEnv *env, jobject instance, jstring text_, jint bytePosition, jint length)
{
    jstring returnValue = NULL;
    const char *text = (*env)->GetStringUTFChars(env, text_, 0);
    char *extractedText = malloc((size_t)length * 4);
    if (extractedText)
    {
        int srcPos = bytePosition;
        int destPos = 0;
        int i;
        size_t bytesTraversed = 0;
        for (i = 0; i < length; i++)
        {
            char chr = text[srcPos];
            int charSize = UTF_CHAR_SIZE(chr);
            if (charSize != 1)
            {
                if (charSize == 0)
                {
                    break;
                }
                chr = PARSER_IGNORE_CHAR;
            }
            if (chr == '\\' && text[srcPos + 1] != '\n')
            {
                if (bytesTraversed > 0)
                {
                    memcpy(&extractedText[destPos], &text[srcPos - bytesTraversed], bytesTraversed);
                    destPos += bytesTraversed;
                    bytesTraversed = 0;
                }
            }
            else
            {
                bytesTraversed += charSize;
            }
            srcPos += charSize;
        }
        if (bytesTraversed > 0)
        {
            memcpy(&extractedText[destPos], &text[srcPos - bytesTraversed], bytesTraversed);
            destPos += bytesTraversed;
        }
        extractedText[destPos] = 0;
        returnValue = (*env)->NewStringUTF(env, extractedText);
        free(extractedText);
    }
    (*env)->ReleaseStringUTFChars(env, text_, text);
    return returnValue;
}