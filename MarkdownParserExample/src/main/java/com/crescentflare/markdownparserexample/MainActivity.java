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
    static final boolean TEST_HTML = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String markdownText = TextUtils.join("\n", getResources().getStringArray(R.array.markdown_test));
        if (TEST_HTML) //The Android HTML to spannable converter doesn't support strike-through text out of the box
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
