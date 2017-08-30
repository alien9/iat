package org.bigrs.iat.util;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

<<<<<<< HEAD:app/src/main/java/org/bigrs/croqui/util/Lever.java
import org.bigrs.croqui.R;
=======
import org.bigrs.iat.R;
>>>>>>> cd5ab9e936d335ad50a1fdf9741cb1d74bb96ce7:app/src/main/java/org/bigrs/iat/util/Lever.java

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
