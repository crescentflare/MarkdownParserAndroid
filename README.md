# MarkdownParserAndroid
A multi-functional and easy way to integrate markdown formatting within Android apps.

### Features

* Easy to use, convert markdown to spannable strings and set it to a text view
* Highly customizable, use the core library to search for markdown tags for customized styling
* Parses the following markdown tags: headers (\#), text styles (italics and bold) and strike through text
* Supports escaping of markdown tag characters (using \\)
* Uses fast native code (optionally) for the core parsing work

### Integration guide
When using gradle, the library can easily be imported into the build.gradle file of your project. Add the following dependency:

    compile ('com.crescentflare.markdownparser:MarkdownParserLib:0.1.0') {
        transitive = false
    }

Make sure that jcenter is added as a repository.

### Example
The provided example shows how to parse markdown, convert it to spannable string or html and show it on a text view.

### Status
The library is new and currently supports just a limited set of markdown and (optionally) convert it to spannables/html using java and native code. The goal is to turn this into a flexible markdown utility library.