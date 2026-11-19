package com.minidroid;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends Activity {

    private LinearLayout chat;
    private ScrollView scroll;
    private EditText input;
    private Brain brain;
    private final Handler handler = new Handler();
    private final Random rnd = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        brain = new Brain(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFEAF2F0);

        scroll = new ScrollView(this);
        chat = new LinearLayout(this);
        chat.setOrientation(LinearLayout.VERTICAL);
        chat.setPadding(dp(8), dp(8), dp(8), dp(8));
        scroll.addView(chat);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(6), dp(4), dp(6), dp(4));

        input = new EditText(this);
        input.setHint("Escribe aquí...");
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent e) {
                if (actionId == EditorInfo.IME_ACTION_SEND
                        || (e != null && e.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && e.getAction() == KeyEvent.ACTION_DOWN)) {
                    send();
                    return true;
                }
                return false;
            }
        });
        bar.addView(input, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button btn = new Button(this);
        btn.setText("Enviar");
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { send(); }
        });
        bar.addView(btn, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        addBubble(brain.greeting(), false);
    }

    private void send() {
        String text = input.getText().toString().trim();
        if (text.length() == 0) { return; }
        input.setText("");
        addBubble(text, true);
        final String answer = brain.reply(text);
        handler.postDelayed(new Runnable() {
            public void run() { addBubble(answer, false); }
        }, 500 + rnd.nextInt(500));
    }

    private void addBubble(String text, boolean mine) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(mine ? 0xFFFFFFFF : 0xFF222222);
        tv.setPadding(dp(12), dp(8), dp(12), dp(8));
        tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78f));
        GradientDrawable g = new GradientDrawable();
        g.setColor(mine ? 0xFF4A9B8E : 0xFFFFFFFF);
        g.setCornerRadius(dp(14));
        tv.setBackgroundDrawable(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = mine ? Gravity.RIGHT : Gravity.LEFT;
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        chat.addView(tv, lp);
        scroll.post(new Runnable() {
            public void run() { scroll.fullScroll(View.FOCUS_DOWN); }
        });
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
