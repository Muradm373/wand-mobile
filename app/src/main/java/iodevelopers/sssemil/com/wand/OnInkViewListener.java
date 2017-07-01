package iodevelopers.sssemil.com.wand;

import android.os.Handler;

/**
 * Created by emil on 8/05/17.
 */
public interface OnInkViewListener {
    void cleanView(boolean emptyAll);

    Handler getHandler();
}
