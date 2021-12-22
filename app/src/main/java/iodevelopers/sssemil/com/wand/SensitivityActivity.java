package iodevelopers.sssemil.com.wand;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by poocoder on 12/8/17.
 */

public class SensitivityActivity extends AppCompatPreferenceActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sensitivity);


        final Button search = findViewById(R.id.set);
        final EditText imeitext = findViewById(R.id.sensitivity_num);

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // MainActivity.sens_t
            }
        });
    }

}
