package nevo_mashiach.pitkiot.NotActivities;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Created by Nevo2 on 10/20/2016.
 */

public class MyButton extends Button{


    public MyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        // TODO Auto-generated constructor stub
    }
    public MyButton(Context context) {
        super(context);
        init();
        // TODO Auto-generated constructor stub
    }
    public MyButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
        // TODO Auto-generated constructor stub
    }
    private void init(){
        Typeface font_type=Typeface.createFromAsset(getContext().getAssets(), "gan.ttf");
        setTypeface(font_type);
    }

}