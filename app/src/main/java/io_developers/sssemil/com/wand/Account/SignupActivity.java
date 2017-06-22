package io_developers.sssemil.com.wand.Account;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io_developers.sssemil.com.wand.R;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";

    private EditText mNameText;
    private EditText mEmailText;
    private EditText mMobileText;
    private EditText mPasswordText;
    private EditText mReEnterPasswordText;
    private Button mSignupButton;

    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mNameText = (EditText) findViewById(R.id.input_name);
        mEmailText = (EditText) findViewById(R.id.input_email);
        mMobileText = (EditText) findViewById(R.id.input_mobile);
        mPasswordText = (EditText) findViewById(R.id.input_password);
        mReEnterPasswordText = (EditText) findViewById(R.id.input_reEnterPassword);

        mSignupButton = (Button) findViewById(R.id.btn_signup);
        TextView loginLink = (TextView) findViewById(R.id.link_login);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
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

        final String name = mNameText.getText().toString();
        final String email = mEmailText.getText().toString();
        final String mobile = mMobileText.getText().toString();
        final String password = mPasswordText.getText().toString();

        //TODO: Implement your own signup logic here.

        ApiHelper apiHelper = new ApiHelper();
        apiHelper.getApi().signup(email, password, name, mobile)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        Log.i("Signup", "onCompleted");
                        onSignupSuccess();
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("Signup", "onError", e);
                        onSignupFailed();
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onNext(String token) {
                        Log.i("Signup", "onNext " + token);
                        mSharedPreferences.edit()
                                .putString(ApiHelper.Companion.getPREF_TOKEN(), token)
                                .putString(ApiHelper.Companion.getPREF_EMAIL(), email)
                                .putString(ApiHelper.Companion.getPREF_NAME(), name)
                                .apply();
                    }
                });
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

        if (mobile.isEmpty() || mobile.length() != 10) {
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