package de.androidcrypto.googledriveplayground;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeleteGoogleDriveFile extends AppCompatActivity implements Serializable {

    private final String TAG = "DeleteGoogleDriveFiles";

    //private Button selectFolder;
    // style="?attr/materialButtonOutlinedStyle"
    // https://github.com/material-components/material-components-android/blob/master/docs/components/Button.md
    //Button listFolder;
    private ListView listViewFiles;

    private String[] folderList;
    private String selectedFolderForIntent, parentFolderForIntent;

    private Intent startMainActivityIntent, startListFolderActivityIntent;

    private static final int REQUEST_CODE_SIGN_IN = 1;

    private Drive googleDriveServiceOwn = null;
    private String googleDriveFolderId = "";
    private String googleDriveFolderName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_google_drive_file);

        //listFolder = findViewById(R.id.btnListFolderA);
        listViewFiles = findViewById(R.id.lvDeleteGoogleDriveFile);
        //selectFolder = findViewById(R.id.btnListGoogleDriveFolderSelect);

        Bundle extras = getIntent().getExtras();
        System.out.println("get bundles");
        if (extras != null) {
            System.out.println("extras not null");
            googleDriveFolderId = (String) getIntent().getSerializableExtra("googleDriveFolderId");
            googleDriveFolderName = (String) getIntent().getSerializableExtra("googleDriveFolderName");

            if (googleDriveFolderId != null) {
                Log.i(TAG, "googleDriveFolderId: " + googleDriveFolderId);
                //parentFolderForIntent = parentFolder;
            }
            if (googleDriveFolderName != null) {
                Log.i(TAG, "googleDriveFolderName: " + googleDriveFolderName);
                //selectedFolderForIntent = folder;
                System.out.println("folder not null");
                //folderFromListFolder = folder;
                System.out.println("googleDriveFolderName: " + googleDriveFolderName);
                // todo do what has todo when folder is selected
                //listFiles.setVisibility(View.GONE);
                //listSubFolder();
                requestSignIn();
                String selectFolderButton = "select folder "
                        + googleDriveFolderName;
                //selectFolder.setText(selectFolderButton);
            }
        }

        /*
        selectFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "selectFolder");
                Bundle bundle = new Bundle();
                bundle.putString("IntentType", "selectGoogleDriveFolder");
                bundle.putString("googleDriveFolderId", googleDriveFolderId);
                bundle.putString("googleDriveFolderName", googleDriveFolderName);
                startMainActivityIntent = new Intent(DeleteGoogleDriveFile.this, MainActivity.class);
                startMainActivityIntent.putExtras(bundle);
                // jumps back
                startActivity(startMainActivityIntent);
                finish();
            }
        });

         */

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
                    Toast.makeText(DeleteGoogleDriveFile.this, "Signed in as " + googleAccount.getEmail(), Toast.LENGTH_SHORT).show();

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

                    // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                    // Its instantiation is required before handling any onClick actions.
                    //mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

                    googleDriveServiceOwn = googleDriveService; // todo

                    //listSubFolder();
                    listFiles();

                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Unable to sign in.", exception);
                    Toast.makeText(DeleteGoogleDriveFile.this, "Unable to sign in: " + exception.getMessage(), Toast.LENGTH_LONG).show();
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

    private void deleteGoogleDriveFile(String fileToDeleteId) {
        Log.i(TAG, "deleteGoogleDriveFile id: " + fileToDeleteId);

        Thread DoDeleteGoogleDriveFile = new Thread() {
            public void run() {
                try {
                    googleDriveServiceOwn.files().delete(fileToDeleteId).execute();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DeleteGoogleDriveFile.this, "selected file deleted", Toast.LENGTH_SHORT).show();
                        }
                    });
                    listFiles();
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DeleteGoogleDriveFile.this, "ERROR: could not delete the selected file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                    //throw new RuntimeException(e);
                }
            }
        };
        DoDeleteGoogleDriveFile.start();
    }


    private void listFiles() {
        Log.i(TAG, "list files in subfolder in Google Drive");

        // https://developers.google.com/drive/api/guides/search-files
        Thread DoBasicListFiles = new Thread() {
            public void run() {
                Log.i(TAG, "running Thread DoBasicListFiles");
                List<com.google.api.services.drive.model.File> files = new ArrayList<com.google.api.services.drive.model.File>();
                String pageToken = null;
                do {
                    FileList result = null;
                    try {
                        // build queryString
                        String queryString = String.format("mimeType != 'application/vnd.google-apps.folder'  and '%s' in parents", googleDriveFolderId);
                        result = googleDriveServiceOwn.files().list()
                                //.setQ("mimeType = 'application/vnd.google-apps.folder'") // list only folders
                                .setQ(queryString)
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

                ArrayList<String> fileNames = new ArrayList<>();
                ArrayList<String> fileIds = new ArrayList<>();

                StringBuilder sb = new StringBuilder();
                sb.append("Folders found in GoogleDrive:\n\n");
                for (int i = 0; i < files.size(); i++) {
                    com.google.api.services.drive.model.File file = files.get(i);
                    String fileName = file.getName();
                    String fileId = file.getId();
                    fileNames.add(fileName);
                    fileIds.add(fileId);
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
                        ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, fileNames);
                        listViewFiles.setAdapter(adapter);

                        listViewFiles.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                            @Override
                            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                                Log.i(TAG, "long click listener for position " + position);

                                String deleteFileName = fileNames.get(position);
                                String deleteFileId = fileIds.get(position);
                                // check before deletion
                                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                Log.i(TAG, "the selectedFolder and parentFolder were stored in SharedPreferences");
                                                //Yes button clicked
                                                deleteGoogleDriveFile(deleteFileId);
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                //No button clicked
                                                Log.i(TAG, "the storage of selectedFolder and parentFolder was denied");
                                                Toast.makeText(DeleteGoogleDriveFile.this, "selected file NOT deleted", Toast.LENGTH_SHORT).show();
                                                break;
                                        }
                                    }
                                };
                                final String selectedFolderString = "You have selected the file\n" +
                                        deleteFileName + "\nwith ID\n" +
                                        deleteFileId + "\n\nfor DELETION.\n\n" +
                                        "Do you want to proceed ?";
                                AlertDialog.Builder builder = new AlertDialog.Builder(DeleteGoogleDriveFile.this);
                                builder.setMessage(selectedFolderString).setPositiveButton(android.R.string.yes, dialogClickListener)
                                        .setTitle("Confirmation required")
                                        .setNegativeButton(android.R.string.no, dialogClickListener).show();
        /*
        If you want to use the "yes" "no" literals of the user's language you can use this
        .setPositiveButton(android.R.string.yes, dialogClickListener)
        .setNegativeButton(android.R.string.no, dialogClickListener)
         */

                                return false;
                            }
                        });


                        /* we do not need a onClickListener to run for a subFolder
                        listViewFolder.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                String selectedItem = (String) parent.getItemAtPosition(position);
                                System.out.println("The selected folder is : " + selectedItem);
                                Bundle bundle = new Bundle();
                                bundle.putString("googleDriveFolderId", googleDriveFolderId);
                                bundle.putString("googleDriveFolderName", googleDriveFolderName);
                                startListFolderActivityIntent = new Intent(ListGoogleDriveFolder.this, ListGoogleDriveFolder.class);
                                startListFolderActivityIntent.putExtras(bundle);
                                startActivity(startListFolderActivityIntent);
                            }
                        });
                         */
                    }
                });

            }
        };
        DoBasicListFiles.start();
    }

    private void listSubFolder() {
        Log.i(TAG, "list subfolder in Google Drive");

        // https://developers.google.com/drive/api/guides/search-files
        Thread DoBasicListFolder = new Thread() {
            public void run() {
                Log.i(TAG, "running Thread DoBasicListFolder");
                List<com.google.api.services.drive.model.File> files = new ArrayList<com.google.api.services.drive.model.File>();
                String pageToken = null;
                do {
                    FileList result = null;
                    try {
                        // build queryString
                        String queryString = String.format("mimeType = 'application/vnd.google-apps.folder'  and '%s' in parents", googleDriveFolderId);
                        result = googleDriveServiceOwn.files().list()
                                //.setQ("mimeType = 'application/vnd.google-apps.folder'") // list only folders
                                .setQ(queryString)
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
                    com.google.api.services.drive.model.File file = files.get(i);
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
                        listViewFiles.setAdapter(adapter);
                        /* we do not need a onClickListener to run for a subFolder
                        listViewFolder.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                String selectedItem = (String) parent.getItemAtPosition(position);
                                System.out.println("The selected folder is : " + selectedItem);
                                Bundle bundle = new Bundle();
                                bundle.putString("googleDriveFolderId", googleDriveFolderId);
                                bundle.putString("googleDriveFolderName", googleDriveFolderName);
                                startListFolderActivityIntent = new Intent(ListGoogleDriveFolder.this, ListGoogleDriveFolder.class);
                                startListFolderActivityIntent.putExtras(bundle);
                                startActivity(startListFolderActivityIntent);
                            }
                        });
                         */
                    }
                });

            }
        };
        DoBasicListFolder.start();
    }

    /*
    private void listFolder(Context context, String startDirectory) {
        Log.i(TAG, "listFolder startDirectory: " + startDirectory);
        String recursiveFolder = parentFolderForIntent.replaceFirst("root", "");
        File externalStorageDir = new File(Environment.getExternalStoragePublicDirectory("")
                + recursiveFolder, startDirectory);
        File[] files = externalStorageDir.listFiles();
        ArrayList<String> fileNames = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                // show nothing
                //fileNames.add(files[i].getName());
            } else {
                fileNames.add((files[i].getName()));
            }
        }
        folderList = fileNames.toArray(new String[0]);
        System.out.println("fileList size: " + folderList.length);
        ArrayAdapter adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, folderList);
        listViewFolder.setAdapter(adapter);
        listViewFolder.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                System.out.println("The selected folder is : " + selectedItem);
                Bundle bundle = new Bundle();
                bundle.putString("selectedFolder", selectedItem);
                bundle.putString("parentFolder", parentFolderForIntent +  "/" + startDirectory);
                // we are starting a new ListFolder activity
                startListFolderActivityIntent = new Intent(ListGoogleDriveFolder.this, ListGoogleDriveFolder.class);
                startListFolderActivityIntent.putExtras(bundle);
                startActivity(startListFolderActivityIntent);
                finish();
            }
        });
    }
    */
}
