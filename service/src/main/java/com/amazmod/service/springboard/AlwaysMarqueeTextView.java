package com.amazmod.service.springboard;

import android.content.Context;
import android.graphics.Rect;
import android.os.CountDownTimer;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;

/*

    Imported from:
    https://github.com/Noobknight/MusicPlayer/blob/master/app/src/main/java/com/tadev/musicplayer/supports/design/AlwaysMarqueeTextView.java
    Modified to add a countdown to automatically starting the marquee after 1s

 */

public class AlwaysMarqueeTextView extends AppCompatTextView {
    protected boolean a;

    public AlwaysMarqueeTextView(Context context) {
        super(context);
        a = false;
        setAlwaysMarquee(true);
        setAlwaysMarquee(false);
        countDownTimer.start();
    }

    public AlwaysMarqueeTextView(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        a = false;
        countDownTimer.start();
    }

    public AlwaysMarqueeTextView(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
        a = false;
        countDownTimer.start();
    }

    CountDownTimer countDownTimer = new CountDownTimer(1000, 1000) {
        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            setAlwaysMarquee(true);
        }
    };

    public boolean isFocused() {
        return a || super.isFocused();
    }

    public void setAlwaysMarquee(boolean flag) {
        setSelected(flag);
        setSingleLine(flag);
        if (flag)
            setEllipsize(TextUtils.TruncateAt.MARQUEE);
        a = flag;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (focused)super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        if (focused)super.onWindowFocusChanged(focused);
    }
}