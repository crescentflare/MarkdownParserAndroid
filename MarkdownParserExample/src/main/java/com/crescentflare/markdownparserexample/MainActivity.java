package com.crescentflare.markdownparserexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.widget.TextView;

import com.crescentflare.markdownparser.MarkdownConverter;


/**
 * The example activity displays an example of parsed markdown
 */
public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String htmlString = MarkdownConverter.toHtmlString("# First chapter\nThis text can be either __bold__ or *italics*.\nA combination is ***also possible***.\n### Small heading\nWith a single line of text.");
        ((TextView)findViewById(R.id.activity_main_text)).setText(Html.fromHtml(htmlString), TextView.BufferType.SPANNABLE);
    }
}
