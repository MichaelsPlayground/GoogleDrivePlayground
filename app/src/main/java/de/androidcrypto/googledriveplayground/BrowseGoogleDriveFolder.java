package de.androidcrypto.googledriveplayground;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrowseGoogleDriveFolder extends AppCompatActivity implements Serializable {

    private final String TAG ="BrowseGoogleDriveFolder";

    private ListView listViewFolder;

    private Intent startListFolderActivityIntent;


    private static final int REQUEST_CODE_SIGN_IN = 1;

    private Drive googleDriveServiceOwn = null;

    private String returnToActivityFromIntent = "";
    // could be SelectEncryptedFoldersActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_google_drive_folder);

        listViewFolder = findViewById(R.id.lvBrowseGoogleDriveFolder);

        /**
         * section for incoming intent handling
         */
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            //System.out.println("extras not null");
            returnToActivityFromIntent = (String) getIntent().getSerializableExtra("returnToActivity");
        }

        requestSignIn();

     }

    /**
     * section sign-in to Google Drive account
     */

    /**
     * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
     */
    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        // DriveScopes.DRIVE shows ALL files
        // DriveScopes.DRIVE_FILE shows only files uploaded by this app

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        //.requestIdToken(clientId)
                        .requestScopes(new Scope(DriveScopes.DRIVE))
                        .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        // todo handle deprecated startActivityForResult
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    /**
     * Handles the {@code result} of a completed sign-in activity initiated from {@link
     * #requestSignIn()}.
     */
    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.i(TAG, "Signed in as " + googleAccount.getEmail());
                    Toast.makeText(BrowseGoogleDriveFolder.this, "Signed in as " + googleAccount.getEmail(), Toast.LENGTH_SHORT).show();

                    // Use the authenticated account to sign in to the Drive service.
                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    Drive googleDriveService =
                            new Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("GoogleDrivePlayground")
                                    .build();

                    googleDriveServiceOwn = googleDriveService; // todo

                    listFolder();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Unable to sign in.", exception);
                    Toast.makeText(BrowseGoogleDriveFolder.this, "Unable to sign in: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * section onActivityResult
     * todo handle deprecated startActivityForResult
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }


    private void listFolder() {
        Log.i(TAG, "list folder in Google Drive");

        // https://developers.google.com/drive/api/guides/search-files
        Thread DoBasicListFolder = new Thread() {
            public void run() {
                Log.i(TAG, "running Thread DoBasicListFolder");
                List<com.google.api.services.drive.model.File> files = new ArrayList<com.google.api.services.drive.model.File>();
                String pageToken = null;
                do {
                    FileList result = null;
                    try {
                        result = googleDriveServiceOwn.files().list()
                                .setQ("mimeType = 'application/vnd.google-apps.folder'") // list only folders
                                .setSpaces("drive")
                                .setFields("nextPageToken, files(id, name, size)")
                                .setPageToken(pageToken)
                                .execute();
                    } catch (IOException e) {
                        //throw new RuntimeException(e);
                        Log.e(TAG, "ERROR: " + e.getMessage());
                    }

                    if (result != null) {
                        files.addAll(result.getFiles());
                    }

                    pageToken = result != null ? result.getNextPageToken() : null;
                } while (pageToken != null);
                // files is containing all files
                //return files;
                Log.i(TAG, "files is containing files or folders: " + files.size());

                ArrayList<String> folderNames = new ArrayList<>();
                ArrayList<String> folderIds = new ArrayList<>();

                StringBuilder sb = new StringBuilder();
                sb.append("Folders found in GoogleDrive:\n\n");
                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);
                    String folderName = file.getName();
                    String folderId = file.getId();
                    folderNames.add(folderName);
                    folderIds.add(folderId);
                    String content =
                            files.get(i).getName() + " " +
                                    files.get(i).getId() + "\n";
                    sb.append(content);
                    sb.append("--------------------\n");
                }
                System.out.println("** folder:\n" + sb.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, folderNames);
                        listViewFolder.setAdapter(adapter);
                        listViewFolder.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                String selectedItemName = (String) parent.getItemAtPosition(position);
                                String selectedItemId = (String) folderIds.get(position);
                                System.out.println("The selected folder is : " + selectedItemName + " id: " + selectedItemId);
                                Bundle bundle = new Bundle();
                                bundle.putString("googleDriveFolderId", selectedItemId);
                                bundle.putString("googleDriveFolderName", selectedItemName);
                                if (returnToActivityFromIntent.equals("SelectEncryptedFoldersActivity")) {
                                    Log.i(TAG, "set returnToActivity to: " + returnToActivityFromIntent);
                                    bundle.putString("returnToActivity", returnToActivityFromIntent);
                                }
                                else {
                                    Log.i(TAG, "set returnToActivity to: " + "");
                                    bundle.putString("returnToActivity", "");
                                }
                                Log.i(TAG, "selectFolder, returnToActivity IN BUNDLE is " + bundle.getSerializable("returnToActivity"));
                                Log.i(TAG, "selectFolder and returnToActivity: " + returnToActivityFromIntent);
                                startListFolderActivityIntent = new Intent(BrowseGoogleDriveFolder.this, ListGoogleDriveFolder.class);
                                startListFolderActivityIntent.putExtras(bundle);
                                startActivity(startListFolderActivityIntent);
                            }
                        });
                    }
                });

            }
        };
        DoBasicListFolder.start();
    }
}
