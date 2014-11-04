package com.zeapo.pwdstore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.zeapo.pwdstore.crypto.PgpHandler;
import com.zeapo.pwdstore.utils.PasswordRepository;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.CloneCommand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class UserPreference extends ActionBarActivity implements Preference.OnPreferenceClickListener {

    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preference);
            findPreference("openpgp_key_id").setOnPreferenceClickListener((UserPreference) getActivity());
            findPreference("ssh_key").setOnPreferenceClickListener((UserPreference) getActivity());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            if ((getIntent().getStringExtra("operation") != null) && (getIntent().getStringExtra("operation").equals("get_ssh_key"))) {
                getSshKey();
            }
        }

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PrefsFragment()).commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Opens a file explorer to import the private key
     */
    public void getSshKey() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, 1);
    }


    private void copySshKey(Uri uri) throws IOException {
        InputStream sshKey = this.getContentResolver().openInputStream(uri);
        byte[] privateKey = IOUtils.toByteArray(sshKey);
        FileUtils.writeByteArrayToFile(new File(getFilesDir() + "/.ssh_key"), privateKey);
        sshKey.close();
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref.getKey().equals("openpgp_key_id")) {
            Intent intent = new Intent(this, PgpHandler.class);
            intent.putExtra("Operation", "GET_KEY_ID");
            startActivityForResult(intent, 0);
        } else if (pref.getKey().equals("ssh_key")) {
            getSshKey();
        }
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
//                Uri sshFile = data.getData();
                try {
                    copySshKey(data.getData());
                    Log.i("PREF", "Got key");
                    setResult(RESULT_OK);
                    finish();
                } catch (IOException e)
                {
                    new AlertDialog.Builder(this).
                            setTitle(this.getResources().getString(R.string.ssh_key_error_dialog_title)).
                            setMessage(this.getResources().getString(R.string.ssh_key_error_dialog_text) + e.getMessage()).
                            setPositiveButton(this.getResources().getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //pass
                                }
                            }).show();
                }
            }
        }
    }
}
