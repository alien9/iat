package br.com.cetsp.util;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import br.com.cetsp.R;


/**
 * Created by tiago on 16/05/17.
 */

public class Lever extends View {
    private void init() {
        setBackground(getResources().getDrawable(R.drawable.circle,null));
    }
    public Lever(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public Lever(Context context, @Nullable AttributeSet attrs){
        super(context, attrs);
        init();
    }
    public Lever(Context context) {
        super(context);
        init();
    }
}
