package iodevelopers.sssemil.com.wand;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by poocoder on 12/7/17.
 */

public class SesitivityDialog extends DialogFragment {


    interface SensListener{
        void sensListener(float num);
    }


    private SensListener listener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle("Set Sensitivity");
        View v = inflater.inflate(R.layout.sensitivity, null);


        final Button search = v.findViewById(R.id.set);
        final EditText imeitext = v.findViewById(R.id.sensitivity_num);

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               listener.sensListener(Float.parseFloat(imeitext.getText().toString()));
            }
        });


        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try{
            this.listener = (SensListener) context;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnCompleteListener");
        }
    }
}
