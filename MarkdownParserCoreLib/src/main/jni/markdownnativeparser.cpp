#include <jni.h>
#include <stdlib.h>
#include "utfstring.h"

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
    MARKDOWN_TAG_PARAGRAPH,
    MARKDOWN_TAG_TEXTSTYLE,
    MARKDOWN_TAG_HEADER
}MARKDOWN_TAG_TYPE;

typedef struct
{
    MARKDOWN_TAG_TYPE type;
    int flags;
    UTFStringIndex startPosition;
    UTFStringIndex endPosition;
    UTFStringIndex startText;
    UTFStringIndex endText;
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
    tag->type = MARKDOWN_TAG_NORMAL;
    tag->flags = 0;
    tag->startPosition = UTFStringIndex(nullptr);
    tag->endPosition = UTFStringIndex(nullptr);
    tag->startText = UTFStringIndex(nullptr);
    tag->endText = UTFStringIndex(nullptr);
    tag->sizeForType = 1;
}

MARKDOWN_TAG *newTag()
{
    MARKDOWN_TAG *tag = (MARKDOWN_TAG *)malloc(sizeof(MARKDOWN_TAG));
    if (tag)
    {
        tagDefaults(tag);
    }
    return tag;
}

void freeTag(MARKDOWN_TAG *tag)
{
    if (tag)
    {
        free(tag);
    }
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

char isWhitespace(int chr)
{
    return chr == ' ' || chr == '\n' || chr == '\r' || chr == '\t';
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
        block->memoryBlock = nullptr;
    }
    block->tagCount = 0;
    block->allocatedMemory = 0;
}

void addTagToBlock(MARKDOWN_TAG_MEMORY_BLOCK *block, const MARKDOWN_TAG *tag)
{
    if (!block || !tag)
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

void addTagToBlockAndFree(MARKDOWN_TAG_MEMORY_BLOCK *block, MARKDOWN_TAG *tag)
{
    addTagToBlock(block, tag);
    if (tag)
    {
        freeTag(tag);
    }
}

MARKDOWN_TAG *tagFromBlock(const MARKDOWN_TAG_MEMORY_BLOCK *block, const int position)
{
    if (position < 0 || position > block->tagCount)
    {
        return nullptr;
    }
    int tagSize = (sizeof(MARKDOWN_TAG) + 7) / 8 * 8;
    return (MARKDOWN_TAG *)&((unsigned char *)block->memoryBlock)[position * tagSize];
}


/**
 * Find tags based on start position and iterating to find the end position
 */
MARKDOWN_TAG *makeParagraphTag(const UTFStringIndex position)
{
    MARKDOWN_TAG *paragraphTag = newTag();
    if (!paragraphTag)
    {
        return nullptr;
    }
    paragraphTag->type = MARKDOWN_TAG_PARAGRAPH;
    paragraphTag->startPosition = position;
    paragraphTag->startText = position;
    paragraphTag->endPosition = position;
    paragraphTag->endText = position;
    return paragraphTag;
}

MARKDOWN_TAG *makeNormalTag(UTFString &markdownText, const UTFStringIndex startPosition, const UTFStringIndex endPosition, const UTFStringIndex startText, const UTFStringIndex endText)
{
    MARKDOWN_TAG *normalTag = newTag();
    if (!normalTag)
    {
        return nullptr;
    }
    normalTag->type = MARKDOWN_TAG_NORMAL;
    normalTag->startPosition = startPosition;
    normalTag->startText = startText;
    normalTag->endPosition = endPosition;
    normalTag->endText = endText;
    if (markdownText[endPosition - 1] == '\n')
    {
        --normalTag->endText;
    }
    if (normalTag->startText < normalTag->endText)
    {
        return normalTag;
    }
    freeTag(normalTag);
    return nullptr;
}

MARKDOWN_TAG *makeNormalTag(UTFString &markdownText, const UTFStringIndex startText, const UTFStringIndex endText)
{
    return makeNormalTag(markdownText, startText, endText, startText, endText);
}

MARKDOWN_TAG *makeHeaderTag(UTFString &markdownText, const UTFStringIndex maxLength, const UTFStringIndex position)
{
    MARKDOWN_TAG *tag = newTag();
    if (!tag)
    {
        return nullptr;
    }
    int headerSize = 0;
    tag->startPosition = position;
    for (UTFStringIndex i = position; i < maxLength; ++i)
    {
        int chr = markdownText[i];
        if (chr == '\\' && markdownText[i + 1] != '\n')
        {
            tag->flags |= MARKDOWN_FLAG_ESCAPED;
            ++i;
            continue;
        }
        if (!tag->startText.valid())
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
                tag->endPosition = i + 1;
                tag->endText = i;
                break;
            }
        }
    }
    if (!tag->endPosition.valid())
    {
        tag->endPosition = maxLength;
        tag->endText = maxLength;
    }
    if (tag->startText.valid())
    {
        return tag;
    }
    freeTag(tag);
    return nullptr;
}

MARKDOWN_TAG *makeTextStyleTag(UTFString &markdownText, const UTFStringIndex maxLength, const UTFStringIndex position)
{
    MARKDOWN_TAG *tag = newTag();
    if (!tag)
    {
        return nullptr;
    }
    int styleStrength = 0;
    int needStyleStrength = 0;
    int tagChr = markdownText[position];
    tag->startPosition = position;
    for (UTFStringIndex i = position; i < maxLength; ++i)
    {
        int chr = markdownText[i];
        if (chr == '\\' && markdownText[i + 1] != '\n')
        {
            tag->flags |= MARKDOWN_FLAG_ESCAPED;
            ++i;
            continue;
        }
        if (!tag->startText.valid())
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
                    tag->endPosition = i + 1;
                    tag->endText = i + 1 - styleStrength;
                    break;
                }
            }
            else if (needStyleStrength != styleStrength)
            {
                if (tagChr != '~' && styleStrength - needStyleStrength > 0)
                {
                    tag->flags |= flagsForTextStrength(styleStrength - needStyleStrength);
                    tag->startText -= needStyleStrength;
                    tag->endPosition = i;
                    tag->endText = i - (styleStrength - needStyleStrength);
                    break;
                }
                else
                {
                    needStyleStrength = styleStrength;
                }
            }
        }
    }
    if (tag->startText.valid() && tag->endText.valid() && (tag->flags & MARKDOWN_FLAG_TEXTSTYLE) > 0)
    {
        return tag;
    }
    freeTag(tag);
    return nullptr;
}


/**
 * Search for types of tag
 */
MARKDOWN_TAG *searchStylingTag(UTFString &markdownText, const UTFStringIndex maxLength, const UTFStringIndex position)
{
    int prevChr = 0;
    for (UTFStringIndex i = position; i < maxLength; ++i)
    {
        const int chr = markdownText[i];
        if (chr == '\\')
        {
            ++i;
            continue;
        }
        if (chr == '#' && (prevChr == '\n' || prevChr == 0 || i == markdownText.startIndex()))
        {
            return makeHeaderTag(markdownText, maxLength, i);
        }
        if (chr == '*' || chr == '_' || chr == '~')
        {
            return makeTextStyleTag(markdownText, maxLength, i);
        }
        prevChr = chr;
    }
    return nullptr;
}

void addNestedStylingTags(MARKDOWN_TAG_MEMORY_BLOCK *foundTags, MARKDOWN_TAG *stylingTag, UTFString &markdownText, const UTFStringIndex maxLength)
{
    MARKDOWN_TAG *nestedTag = searchStylingTag(markdownText, stylingTag->endText, stylingTag->startText);
    if (nestedTag && nestedTag->type != MARKDOWN_TAG_HEADER && nestedTag->endPosition < stylingTag->endPosition)
    {
        //Combine found styling tag with nested tag
        nestedTag->flags |= stylingTag->flags & MARKDOWN_FLAG_TEXTSTYLE;

        //Add part of found styling tag before nested style
        MARKDOWN_TAG *beforeNestedTag = newTag();
        if (beforeNestedTag)
        {
            beforeNestedTag->type = stylingTag->type;
            beforeNestedTag->startPosition = stylingTag->startPosition;
            beforeNestedTag->startText = stylingTag->startText;
            beforeNestedTag->endPosition = nestedTag->startPosition;
            beforeNestedTag->endText = nestedTag->startPosition;
            beforeNestedTag->flags = stylingTag->flags;
            if (beforeNestedTag->startText < beforeNestedTag->endText)
            {
                addTagToBlock(foundTags, beforeNestedTag);
            }
            freeTag(beforeNestedTag);
        }

        //Add found nested style tag recursively
        addNestedStylingTags(foundTags, nestedTag, markdownText, maxLength);

        //Add part of found styling tag after nested style
        MARKDOWN_TAG *afterNestedTag = newTag();
        if (afterNestedTag)
        {
            afterNestedTag->type = stylingTag->type;
            afterNestedTag->startPosition = nestedTag->endPosition;
            afterNestedTag->startText = nestedTag->endPosition;
            afterNestedTag->endPosition = stylingTag->endPosition;
            afterNestedTag->endText = stylingTag->endText;
            afterNestedTag->flags = stylingTag->flags;
            if (afterNestedTag->startText < afterNestedTag->endText)
            {
                addTagToBlock(foundTags, afterNestedTag);
            }
            freeTag(afterNestedTag);
        }
    }
    else
    {
        addTagToBlock(foundTags, stylingTag);
    }
    freeTag(nestedTag);
}

void addParagraphedNormalTag(MARKDOWN_TAG_MEMORY_BLOCK *foundTags, MARKDOWN_TAG *stylingTag, UTFString &markdownText, const UTFStringIndex maxLength)
{
    bool foundEscapedChar = false, foundParagraphBreak = false;
    UTFStringIndex lineStart = stylingTag->startText, endParagraph(nullptr), foundPrintableCharAt(nullptr);
    int newlineCount = 0;
    if (stylingTag->startText > markdownText.startIndex() && markdownText[stylingTag->startText - 1] != '\n')
    {
        foundPrintableCharAt = stylingTag->startText;
    }
    for (UTFStringIndex i = stylingTag->startText; i < stylingTag->endText; ++i)
    {
        int chr = markdownText[i];
        if (chr == '\\' && markdownText[i + 1] != '\n')
        {
            foundEscapedChar = true;
            continue;
        }
        if (!foundPrintableCharAt.valid() && !isWhitespace(chr))
        {
            foundPrintableCharAt = lineStart;
        }
        if (chr == '\n')
        {
            lineStart = i + 1;
        }
        if (chr == '\n' && foundPrintableCharAt.valid())
        {
            newlineCount++;
            if (newlineCount == 1)
            {
                endParagraph = i;
            }
            if (newlineCount > 1)
            {
                if (foundParagraphBreak)
                {
                    addTagToBlockAndFree(foundTags, makeParagraphTag(stylingTag->startText));
                }
                MARKDOWN_TAG *normalTag = makeNormalTag(markdownText, stylingTag->startPosition, endParagraph, foundPrintableCharAt, endParagraph);
                if (normalTag)
                {
                    if (foundEscapedChar)
                    {
                        normalTag->flags |= MARKDOWN_FLAG_ESCAPED;
                    }
                    addTagToBlock(foundTags, normalTag);
                    stylingTag->startPosition = i + 1;
                    stylingTag->startText = i + 1;
                    freeTag(normalTag);
                }
                foundEscapedChar = false;
                foundParagraphBreak = true;
                foundPrintableCharAt = UTFStringIndex(nullptr);
                newlineCount = 0;
            }
        }
        else if (chr != ' ')
        {
            newlineCount = 0;
        }
    }
    if (foundPrintableCharAt.valid())
    {
        if (foundParagraphBreak)
        {
            addTagToBlockAndFree(foundTags, makeParagraphTag(stylingTag->startText));
        }
        stylingTag->startText = foundPrintableCharAt;
        if (foundEscapedChar)
        {
            stylingTag->flags |= MARKDOWN_FLAG_ESCAPED;
        }
        if (stylingTag->startText < stylingTag->endText)
        {
            addTagToBlock(foundTags, stylingTag);
        }
    }
}


/**
 * JNI function to find all supported markdown tags
 */
extern "C"
{
JNIEXPORT jintArray JNICALL
Java_com_crescentflare_markdownparsercore_MarkdownNativeParser_findNativeTags(JNIEnv *env, jobject instance, jstring markdownText_)
{
    //Java string conversion
    const char *markdownTextPointer = env->GetStringUTFChars(markdownText_, 0);
    UTFString markdownText(markdownTextPointer);

    //Loop over string and find tags
    MARKDOWN_TAG_MEMORY_BLOCK foundTags = newMarkdownTagMemoryBlock();
    UTFStringIndex maxLength = markdownText.endIndex();
    UTFStringIndex position = markdownText.startIndex();
    bool processing;
    do
    {
        MARKDOWN_TAG *stylingTag = searchStylingTag(markdownText, maxLength, position);
        if (stylingTag)
        {
            if (stylingTag->startPosition > position)
            {
                MARKDOWN_TAG *normalTag = makeNormalTag(markdownText, position, stylingTag->startPosition);
                if (normalTag)
                {
                    addParagraphedNormalTag(&foundTags, normalTag, markdownText, maxLength);
                }
            }
            if (stylingTag->type == MARKDOWN_TAG_TEXTSTYLE)
            {
                addNestedStylingTags(&foundTags, stylingTag, markdownText, maxLength);
            }
            else
            {
                addTagToBlock(&foundTags, stylingTag);
            }
            position = stylingTag->endPosition;
            processing = true;
            freeTag(stylingTag);
        }
        else
        {
            processing = false;
        }
    }
    while (processing);

    //Add final tag if there is a bit of string left to handle
    if (position < maxLength)
    {
        MARKDOWN_TAG *normalTag = makeNormalTag(markdownText, position, maxLength);
        if (normalTag)
        {
            addParagraphedNormalTag(&foundTags, normalTag, markdownText, maxLength);
        }
    }

    //Convert tags into a java array, clean up and return
    jintArray returnArray = nullptr;
    jint *convertedValues = (jint *) malloc(foundTags.tagCount * tagFieldCount() * sizeof(jint));
    if (convertedValues)
    {
        returnArray = env->NewIntArray(foundTags.tagCount * tagFieldCount());
        int i;
        for (i = 0; i < foundTags.tagCount; i++)
        {
            fillTagToArray(tagFromBlock(&foundTags, i), &convertedValues[i * tagFieldCount()]);
        }
        env->SetIntArrayRegion(returnArray, 0, foundTags.tagCount * tagFieldCount(), convertedValues);
        free(convertedValues);
    }
    env->ReleaseStringUTFChars(markdownText_, markdownTextPointer);
    deleteMarkdownTagMemoryBlock(&foundTags);
    return returnArray;
}
}

/**
 * JNI function to extract an escaped string
 */
extern "C"
{
JNIEXPORT jstring JNICALL
Java_com_crescentflare_markdownparsercore_MarkdownNativeParser_escapedSubstring(JNIEnv *env, jobject instance, jstring text_, jint bytePosition, jint length)
{
    jstring returnValue = nullptr;
    const char *text = env->GetStringUTFChars(text_, 0);
    char *extractedText = (char *) malloc((size_t) length * 4);
    if (extractedText)
    {
        int srcPos = bytePosition;
        int destPos = 0;
        int i;
        size_t bytesTraversed = 0;
        for (i = 0; i < length; i++)
        {
            char chr = text[srcPos];
            int charSize = UTFStringIndex::charSize(chr);
            if (charSize != 1)
            {
                if (charSize == 0)
                {
                    charSize = 1;
                }
                chr = 0;
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
        returnValue = env->NewStringUTF(extractedText);
        free(extractedText);
    }
    env->ReleaseStringUTFChars(text_, text);
    return returnValue;
}
}