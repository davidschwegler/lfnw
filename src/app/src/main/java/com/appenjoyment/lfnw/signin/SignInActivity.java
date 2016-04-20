package com.appenjoyment.lfnw.signin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.appenjoyment.lfnw.R;
import com.appenjoyment.lfnw.accounts.AccountManager;
import com.appenjoyment.lfnw.accounts.SignInResponseData;
import com.appenjoyment.utility.HttpUtility;

import java.net.CookieManager;
import java.net.HttpURLConnection;

public class SignInActivity extends AppCompatActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signin);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		m_emailText = (EditText) findViewById(R.id.input_email);
		m_passwordText = (EditText) findViewById(R.id.input_password);
		m_signInButton = (Button) findViewById(R.id.btn_signin);
		m_forgotPasswordLink = (TextView) findViewById(R.id.link_forgot_password);
		m_signUpLink = (TextView) findViewById(R.id.link_signup);

		m_passwordText.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_DONE)
				{
					signIn();
					return true;
				}

				return false;
			}
		});

		m_signInButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (!validate())
					return;

				signIn();
			}
		});

		m_signUpLink.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linuxfestnorthwest.org/user/register"));
				startActivity(intent);
			}
		});

		m_forgotPasswordLink.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linuxfestnorthwest.org/user/password"));
				startActivity(intent);
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void signIn()
	{
		Log.d(TAG, "signIn");
		m_signInButton.setEnabled(false);

		final ProgressDialog progressDialog = new ProgressDialog(SignInActivity.this);
		progressDialog.setIndeterminate(true);
		progressDialog.setMessage("Authenticating...");
		progressDialog.show();

		final String email = m_emailText.getText().toString();
		final String password = m_passwordText.getText().toString();
		new AsyncTask<Void, Void, Boolean>()
		{
			@Override
			protected Boolean doInBackground(Void... params)
			{
				String url = "https://www.linuxfestnorthwest.org/api/login/user/logintoboggan";
				String postContent = "{\"username\":\"" + email + "\",\"password\":\"" + password + "\"}";

				HttpUtility.StringResponse response = HttpUtility.sendPost(url, postContent);

				if (response != null && response.result != null && response.responseCode == HttpURLConnection.HTTP_OK)
				{
					SignInResponseData data = SignInResponseData.parseFromJson(response.result);
					if (data == null)
						return false;

					AccountManager.getInstance().setLogin(data.account, data.user);

					return true;
				}

				return false;
			}

			@Override
			protected void onPostExecute(Boolean result)
			{
				progressDialog.dismiss();

				if (result != null && result.booleanValue())
					onSignInSuccess();
				else
					onSignInFailed();
			}
		}.execute();
	}

	public void onSignInSuccess()
	{
		Log.d(TAG, "onSignInSuccess");
		m_signInButton.setEnabled(true);

		setResult(RESULT_OK);
		finish();
	}

	public void onSignInFailed()
	{
		Log.d(TAG, "onSignInFailed");
		m_signInButton.setEnabled(true);

		Toast.makeText(SignInActivity.this, "Couldn't sign in", Toast.LENGTH_SHORT).show();
	}

	public boolean validate()
	{
		boolean valid = true;

		String email = m_emailText.getText().toString();
		String password = m_passwordText.getText().toString();

		if (email.isEmpty())
		{
			m_emailText.setError("enter a username or email");
			valid = false;
		}
		else
		{
			m_emailText.setError(null);
		}

		if (password.isEmpty())
		{
			m_passwordText.setError("enter a password");
			valid = false;
		}
		else
		{
			m_passwordText.setError(null);
		}

		return valid;
	}

	private static final String TAG = "SignInActivity";

	private EditText m_emailText;
	private EditText m_passwordText;
	private Button m_signInButton;
	private TextView m_signUpLink;
	private TextView m_forgotPasswordLink;
}