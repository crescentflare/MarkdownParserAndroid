# MarkdownParserAndroid
A multi-functional and easy way to integrate markdown formatting within Android apps.

### Features

* Easy to use, convert markdown to spannable strings and set it to a text view
* Highly customizable, use the core library to search for markdown tags for customized styling
* Also customizable without using the core library through a simple interface
* Parses the following markdown tags: headers (\#), text styles (italics and bold), strike through text, lists and links
* Supports escaping of markdown tag characters (using \\)
* Uses fast native code (optionally) for the core parsing work

### Integration guide
When using gradle, the library can easily be imported into the build.gradle file of your project. Add the following dependency:

    compile ('com.crescentflare.markdownparser:MarkdownParserLib:0.4.0') {
        transitive = false
    }

Make sure that jcenter is added as a repository.

### Example
The provided example shows how to parse markdown, convert it to spannable string or html and show it on a text view. Also it contains an example on how to apply custom styling easily.

### Status
The library is new and doesn't contain all markdown features, but the commonly used features should be supported. It can (optionally) convert it to spannables/html using java and native code. The goal is to turn this into a flexible markdown utility library.