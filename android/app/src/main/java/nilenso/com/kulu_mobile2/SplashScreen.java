package nilenso.com.kulu_mobile2;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;

public class SplashScreen extends Activity {
    public static final String TEAM_NAME = "TeamName";
    public static final String ACCOUNT_NAME = "AccountName";
    public static final String TOKEN = "token";
    static final String LOGGED_IN = "LoggedIn";
    static final String SIGN_OUT = "SignOut";
    static final String ERROR = "error";

    private static final String TAG = "SplashScreen";

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isSigningOut()) signOut();
        if (isUserLoggedIn()) startMainActivity();

        getActionBar().hide();
        setContentView(R.layout.splash_screen);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pd = ProgressDialog.show(SplashScreen.this, "", "Please Wait", false);
                new LoginTask().execute();
            }
        });

        final Animation animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.fadein);
        findViewById(R.id.kuluLogo).startAnimation(animationFadeIn);
    }

    private boolean isSigningOut() {
        if (getIntent().getExtras() == null) return false;
        if (getIntent().getExtras().getString(SIGN_OUT) == null) return false;
        return getIntent().getExtras().getString(SIGN_OUT).equals("true");
    }

    public void signOut() {
        clearUserInfo();
    }

    private void saveUserInfo(String teamName, String email, String token) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(TEAM_NAME, teamName);
        editor.putString(ACCOUNT_NAME, email);
        editor.putString(TOKEN, token);
        editor.putString(LOGGED_IN, "true");
        editor.commit();
        Log.e(TAG, String.valueOf(sharedPref.contains(LOGGED_IN)));
    }

    private void clearUserInfo() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();
    }

    private boolean isUserLoggedIn() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String logged_in = sharedPref.getString(LOGGED_IN, "false");
        return logged_in.equals("true");
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void onBackPressed() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    private class LoginTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String orgName = ((EditText) findViewById(R.id.loginOrgName)).getText().toString();
            String email = ((EditText) findViewById(R.id.loginEmail)).getText().toString();
            String password = ((EditText) findViewById(R.id.loginPassword)).getText().toString();
            try {
                return new LoginClient().login(getString(R.string.kulu_backend_login_url), orgName, email, password);
            } catch (IOException e) {
                return ERROR;
            } catch (JSONException e) {
                return ERROR;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.equals(ERROR)) {
                pd.dismiss();
                Toast.makeText(getBaseContext(), "Issue with logging in", Toast.LENGTH_SHORT).show();
            } else {
                String teamName = ((EditText) findViewById(R.id.loginOrgName)).getText().toString();
                String email = ((EditText) findViewById(R.id.loginEmail)).getText().toString();
                saveUserInfo(teamName, email, result);
                pd.dismiss();
                startMainActivity();
            }
        }
    }
}
