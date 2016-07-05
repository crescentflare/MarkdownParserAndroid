package com.crescentflare.markdownparserexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.widget.TextView;

import com.crescentflare.markdownparser.MarkdownConverter;


/**
 * The example activity displays an example of parsed markdown
 */
public class MainActivity extends AppCompatActivity
{
    //Enable/disable conversion through HTML
    //
    //HTML doesn't support these markdown features without a custom tag handler:
    //- Strike through text
    //- Ordered and unordered lists
    static final boolean TEST_HTML = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String markdownText = TextUtils.join("\n", getResources().getStringArray(R.array.markdown_test));
        if (TEST_HTML)
        {
            String htmlString = MarkdownConverter.toHtmlString(markdownText);
            ((TextView)findViewById(R.id.activity_main_text)).setText(Html.fromHtml(htmlString), TextView.BufferType.SPANNABLE);
        }
        else
        {
            ((TextView)findViewById(R.id.activity_main_text)).setText(MarkdownConverter.toSpannable(markdownText), TextView.BufferType.SPANNABLE);
        }
    }
}
