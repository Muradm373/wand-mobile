package io_developers.sssemil.com.wand.Account;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.Bind;
import io_developers.sssemil.com.wand.R;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";

    @Bind(R.id.input_name) EditText mNameText;
    @Bind(R.id.input_email) EditText mEmailText;
    @Bind(R.id.input_mobile) EditText mMobileText;
    @Bind(R.id.input_password) EditText mPasswordText;
    @Bind(R.id.input_reEnterPassword) EditText mReEnterPasswordText;
    @Bind(R.id.btn_signup) Button mSignupButton;
    @Bind(R.id.link_login) TextView mLoginLink;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

        mSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        mLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
    }

    private void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed();
            return;
        }

        mSignupButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(SignupActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        String name = mNameText.getText().toString();
        String email = mEmailText.getText().toString();
        String mobile = mMobileText.getText().toString();
        String password = mPasswordText.getText().toString();
        String reEnterPassword = mReEnterPasswordText.getText().toString();

        // TODO: Implement your own signup logic here.

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onSignupSuccess or onSignupFailed
                        // depending on success
                        onSignupSuccess();
                        // onSignupFailed();
                        progressDialog.dismiss();
                    }
                }, 3000);
    }


    private void onSignupSuccess() {
        mSignupButton.setEnabled(true);
        setResult(RESULT_OK, null);
        finish();
    }

    private void onSignupFailed() {
        Toast.makeText(getBaseContext(), R.string.login_failed, Toast.LENGTH_LONG).show();

        mSignupButton.setEnabled(true);
    }

    private boolean validate() {
        boolean valid = true;

        String name = mNameText.getText().toString();
        String email = mEmailText.getText().toString();
        String mobile = mMobileText.getText().toString();
        String password = mPasswordText.getText().toString();
        String reEnterPassword = mReEnterPasswordText.getText().toString();

        if (name.isEmpty() || name.length() < 3) {
            mNameText.setError(getString(R.string.name_length_error));
            valid = false;
        } else {
            mNameText.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmailText.setError(getString(R.string.invalid_email));
            valid = false;
        } else {
            mEmailText.setError(null);
        }

        if (mobile.isEmpty() || mobile.length()!=10) {
            mMobileText.setError(getString(R.string.invalid_number));
            valid = false;
        } else {
            mMobileText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            mPasswordText.setError(getString(R.string.invalid_pass_length));
            valid = false;
        } else {
            mPasswordText.setError(null);
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length() < 4 || reEnterPassword.length() > 10 || !(reEnterPassword.equals(password))) {
            mReEnterPasswordText.setError(getString(R.string.different_passwords_error));
            valid = false;
        } else {
            mReEnterPasswordText.setError(null);
        }

        return valid;
    }
}