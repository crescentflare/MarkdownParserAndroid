package com.crescentflare.markdownparserexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
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
        final TextView markdownView = (TextView)findViewById(R.id.activity_main_text);
        if (TEST_HTML)
        {
            String htmlString = MarkdownConverter.toHtmlString(markdownText);
            markdownView.setText(Html.fromHtml(htmlString), TextView.BufferType.SPANNABLE);
        }
        else
        {
            markdownView.setText(MarkdownConverter.toSpannable(markdownText), TextView.BufferType.SPANNABLE);
        }
        markdownView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
