package de.androidcrypto.googledriveplayground;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleSyncLocalToGoogleDriveActivity extends AppCompatActivity {

    private final String TAG = "SimpleSyncLocalToGD";

    RadioButton showSync, showLocal, showGoogle;
    Button startSync;
    ProgressBar progressBar;
    TextView tvProgress, tvProgressAbsolute;
    private Handler handler = new Handler();
    ListView listFiles;
    // default values
    boolean isSyncChecked = true;
    boolean isLocalChecked = false;
    boolean isGoogleChecked = false;

    ArrayList<String> syncFileNames = new ArrayList<>();
    ArrayList<String> localFileNames = new ArrayList<>();
    ArrayList<String> googleFileNames = new ArrayList<>();

    String localFolderName, localFolderPath;
    String googleDriveFolderName, googleDriveFolderId;
    StorageUtils storageUtils;

    private static final int REQUEST_CODE_SIGN_IN = 1;

    private Drive googleDriveServiceOwn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_sync_local_to_google_drive);

        showSync = findViewById(R.id.rbSimpleSyncToGoogleSync);
        showLocal = findViewById(R.id.rbSimpleSyncToGoogleSyncLocal);
        showGoogle = findViewById(R.id.rbSimpleSyncToGoogleSyncGoogle);
        startSync = findViewById(R.id.btnSimpleSyncToGoogleSync);
        listFiles = findViewById(R.id.lvSimpleSyncToGoogle);
        progressBar = findViewById(R.id.pbSimpleSyncToGoogleSyncGoogle);
        tvProgress = findViewById(R.id.tvSimpleSyncToGoogleSyncGoogleProgress);
        tvProgressAbsolute = findViewById(R.id.tvSimpleSyncToGoogleSyncGoogleProgressAbsolute);

        // init storageUtils
        storageUtils = new StorageUtils(getApplicationContext());
        localFolderName = storageUtils.getLocalStorageName();
        localFolderPath = storageUtils.getLocalStoragePath();
        googleDriveFolderName = storageUtils.getGoogleDriveStorageName();
        googleDriveFolderId = storageUtils.getGoogleDriveStorageId();

        // sign in to GoogleDrive
        requestSignIn();

        showSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSyncChecked = true;
                isLocalChecked = false;
                isGoogleChecked = false;
                startSync.setEnabled(true);
                listAllFolder();
            }
        });

        showLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSyncChecked = false;
                isLocalChecked = true;
                isGoogleChecked = false;
                startSync.setEnabled(false);
                listAllFolder();
            }
        });

        showGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSyncChecked = false;
                isLocalChecked = false;
                isGoogleChecked = true;
                startSync.setEnabled(false);
                listAllFolder();
            }
        });

        startSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "start simple sync");

                // todo run the upload process, check that syncFileNames list is not empty :-)
                if (syncFileNames.size() < 1) {
                    Log.i(TAG, "no files to sync, aborted");
                    Snackbar snackbar = Snackbar.make(view, "No files to sync", Snackbar.LENGTH_LONG);
                    snackbar.setBackgroundTint(ContextCompat.getColor(SimpleSyncLocalToGoogleDriveActivity.this, R.color.red));
                    snackbar.show();
                    return;
                }
                uploadFileToGoogleDriveSubfolderNew(view);
                /* old
                int numberOfFilesToSync = syncFileNames.size();
                Log.i(TAG, "there are " + numberOfFilesToSync + " files to sync, starting...");
                for (int i = 0; i < numberOfFilesToSync; i++) {
                    String filenameToUpload = syncFileNames.get(i);
                    Log.i(TAG, "fileName to upload: " + filenameToUpload);
                    uploadFileToGoogleDriveSubfolder(filenameToUpload);
                }
                */

            }
        });
    }

    private void uploadFileToGoogleDriveSubfolderNew(View view) {
        Log.i(TAG, "Basic upload from internal storage to subfolder");

        final int numberOfFilesToSync = syncFileNames.size();
        final int MAX = numberOfFilesToSync;
        progressBar.setMax(MAX);
        Log.i(TAG, "there are " + numberOfFilesToSync + " files to sync, starting...");

        /*
        if (!checkLoginStatus()) {
            Log.e(TAG, "please sign in before upload a file");
            return;
        }

         */
        // https://developers.google.com/drive/api/guides/manage-uploads
        Thread DoBasicUploadSubfolder = new Thread() {
            public void run() {
                Log.i(TAG, "running Thread DoBasicUploadSubfolder");
                handler.post(new Runnable() {
                    public void run() {
                        startSync.setEnabled(false);
                    }
                });

                for (int i = 0; i < numberOfFilesToSync; i++) {
                    final int progress = i + 1;
                    String filename = syncFileNames.get(i);
                    Log.i(TAG, "fileName to upload: " + filename);

                    String folderId = googleDriveFolderId;
                    if (folderId.equals("")) {
                        Log.e(TAG, "The destination folder does not exist, abort: " + filename);
                        return;
                    } else {
                        Log.i(TAG, "The destination folder is existing, start uploading to folderId: " + folderId);
                    }

                    com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                    //fileMetadata.setName("photo.jpg");
                    fileMetadata.setName(filename);
                    fileMetadata.setParents(Collections.singletonList(folderId));
                    // File's content.
                    String recursiveFolder = localFolderPath.replaceFirst("root", "");
                    java.io.File externalStorageDir = new File(Environment.getExternalStoragePublicDirectory("")
                            , recursiveFolder);
                    java.io.File filePath = new java.io.File(externalStorageDir, filename);
                    if (filePath.exists()) {
                        Log.i(TAG, "filePath " + filename + " is existing");
                    } else {
                        Log.e(TAG, "filePath " + filename + " is NOT existing");
                        return;
                    }

                    // get media type
                    Uri uri = Uri.fromFile(filePath);
                    String mimeType = getMimeType(uri);
                    //System.out.println("* uri: " + uri);
                    //System.out.println("* mimeType: " + mimeType);

                    // todo Specify media type and file-path for file.
                    //FileContent mediaContent = new FileContent("image/jpeg", filePath);
                    //FileContent mediaContent = new FileContent("text/plain", filePath);
                    FileContent mediaContent = new FileContent(mimeType, filePath);
                    try {
                        com.google.api.services.drive.model.File file = googleDriveServiceOwn.files().create(fileMetadata, mediaContent)
                                .setFields("id, parents")
                                .execute();
                        //System.out.println("File ID: " + file.getId());
                        Log.i(TAG, "The file was saved with fileId: " + file.getId());
                        Log.i(TAG, "The file has a size of: " + file.getSize() + " bytes");
                        //return file.getId();
                    } catch (GoogleJsonResponseException e) {
                        // TODO(developer) - handle error appropriately
                        System.err.println("Unable to upload file: " + e.getDetails());
                        //throw e;
                        Log.e(TAG, "ERROR: " + e.getDetails());
                    } catch (IOException e) {
                        //throw new RuntimeException(e);
                        Log.e(TAG, "IOException: " + e.getMessage());
                    }
                    handler.post(new Runnable() {
                        public void run() {
                            progressBar.setProgress(progress);
                            int percent = (progress * 100) / MAX;

                            tvProgress.setText("Percent: " + percent + " %");
                            tvProgressAbsolute.setText("files uploaded: " + progress + " of total " + MAX + " files");
                            if(progress == MAX)  {
                                tvProgress.setText("Completed!");
                                tvProgressAbsolute.setText("Completed upload (" + MAX + ") files!");
                                startSync.setEnabled(true);
                            }
                        }
                    });
                }
                Snackbar snackbar = Snackbar.make(view, "All files were synced", Snackbar.LENGTH_SHORT);
                snackbar.setBackgroundTint(ContextCompat.getColor(SimpleSyncLocalToGoogleDriveActivity.this, R.color.green));
                snackbar.show();
                listAllFolder();
            }
        };
        DoBasicUploadSubfolder.start();
    }

    private void uploadFileToGoogleDriveSubfolder(String filenameToUpload) {
        Log.i(TAG, "Basic upload from internal storage to subfolder");
        /*
        if (!checkLoginStatus()) {
            Log.e(TAG, "please sign in before upload a file");
            return;
        }

         */
        // https://developers.google.com/drive/api/guides/manage-uploads
        Thread DoBasicUploadSubfolder = new Thread() {
            public void run() {
                Log.i(TAG, "running Thread DoBasicUploadSubfolder");

                //String filename = "txtfile1.txt";
                String filename = filenameToUpload;
                String folderName = "test";
                //String folderId = getFolderId(folderName);
                String folderId = googleDriveFolderId;
                if (folderId.equals("")) {
                    Log.e(TAG, "The destination folder does not exist, abort: " + filename);
                    return;
                } else {
                    Log.i(TAG, "The destination folder is existing, start uploading to folderId: " + folderId);
                }

                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                //fileMetadata.setName("photo.jpg");
                fileMetadata.setName(filename);
                fileMetadata.setParents(Collections.singletonList(folderId));
                // File's content.
                String recursiveFolder = localFolderPath.replaceFirst("root", "");
                java.io.File externalStorageDir = new File(Environment.getExternalStoragePublicDirectory("")
                        , recursiveFolder);
                java.io.File filePath = new java.io.File(externalStorageDir, filename);
                if (filePath.exists()) {
                    Log.i(TAG, "filePath " + filename + " is existing");
                } else {
                    Log.e(TAG, "filePath " + filename + " is NOT existing");
                    return;
                }

                // get media type
                Uri uri = Uri.fromFile(filePath);
                String mimeType = getMimeType(uri);
                System.out.println("* uri: " + uri);
                System.out.println("* mimeType: " + mimeType);

                FileContent mediaContent = new FileContent(mimeType, filePath);
                try {
                    com.google.api.services.drive.model.File file = googleDriveServiceOwn.files().create(fileMetadata, mediaContent)
                            .setFields("id, parents")
                            .execute();
                    //System.out.println("File ID: " + file.getId());
                    Log.i(TAG, "The file was saved with fileId: " + file.getId());
                    Log.i(TAG, "The file has a size of: " + file.getSize() + " bytes");
                    //return file.getId();
                } catch (GoogleJsonResponseException e) {
                    // TODO(developer) - handle error appropriately
                    //System.err.println("Unable to upload file: " + e.getDetails());
                    //throw e;
                    Log.e(TAG, "ERROR: " + e.getDetails());
                } catch (IOException e) {
                    //throw new RuntimeException(e);
                    Log.e(TAG, "IOException: " + e.getMessage());
                }
                // todo give a message and rerun the syncList
            }
        };
        DoBasicUploadSubfolder.start();
    }

    public String getMimeType(Uri uri) {
        String mimeType = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = getApplicationContext().getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

    /**
     * step 1: listAllFolders gets all files in the local and GoogleDrive folder ist an ArrayList
     * step 2: for each file in localFolderList it checks if a file with this name exists in googleDriveFolderList
     * step 3: if a file is missing in googleDriveFolderList it will be listed in the listView
     * step 4: all files in the listView will be uploaded to googleDrive
     * step 5: if radioGroupButton local is checked only all local files get shown, the sync button is disabled
     * step 6: if radioGroupButton GoogleDrive is checked only all GoogleDrive files get shown, the sync button is disabled
     */
    private void listAllFolder() {
        Log.i(TAG, "listAllFolder");

        listLocalFiles(getApplicationContext());
        listGoogleDriveFiles();

        // compare both lists in listGoogleDriveFiles
    }


    private void showFiles(ArrayList<String> fileNames) {
        String[] fileList;
        fileList = fileNames.toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, fileList);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listFiles.setAdapter(adapter);
            }
        });

    }


    private void listGoogleDriveFiles() {
        Log.i(TAG, "listGoogleDriveFiles");

        Thread DoBasicListFilesInFolder = new Thread() {
            public void run() {
                Log.i(TAG, "running Thread DoBasicListFilesInFolder");
                listFilesInGoogleFolder(googleDriveFolderId);
                syncFileNames = new ArrayList<>();
                System.out.println("* syncFileNames old size: " + syncFileNames.size());
                System.out.println("* localFileNames size: " + localFileNames.size());
                System.out.println("* GoogleFileNames size: " + googleFileNames.size());
                // find files from local in GoogleDrive list
                for (int i = 0; i < localFileNames.size(); i++) {
                    int index = googleFileNames.indexOf(localFileNames.get(i));
                    // if index = -1 the localFileName is NOT in the googleDriveFileNames list
                    if (index < 0) {
                        // add the entry to the syncs list
                        syncFileNames.add(localFileNames.get(i));
                    }
                }
                //System.out.println("* syncFileNames new size: " + syncFileNames.size());
                // show data depending on radioGroup
                if (isSyncChecked) {
                    showFiles(syncFileNames);
                }
                if (isLocalChecked) {
                    showFiles(localFileNames);
                }
                if (isGoogleChecked) {
                    showFiles(googleFileNames);
                }
            }
        };
        DoBasicListFilesInFolder.start();
    }

    private void listFilesInGoogleFolder(String folderId) {
        // https://developers.google.com/drive/api/v3/reference/files
        Log.i(TAG, "listFilesInFolder: " + folderId);
        List<com.google.api.services.drive.model.File> files = new ArrayList<com.google.api.services.drive.model.File>();
        String pageToken = null;
        do {
            FileList result = null;
            try {

                // build queryString
                String queryString = String.format("mimeType != 'application/vnd.google-apps.folder'  and '%s' in parents", folderId);
                //                      queryString: "mimeType != 'application/vnd.google-apps.folder'  and '1-c0_0R0tOomtfuHcpi3Y08PHQXRuMG15' in parents"
                //                             .setQ("mimeType != 'application/vnd.google-apps.folder'  and '1-c0_0R0tOomtfuHcpi3Y08PHQXRuMG15' in parents")
                //System.out.println("* queryString: " + queryString);
                result = googleDriveServiceOwn.files().list()
                        //.setQ("mimeType != 'application/vnd.google-apps.folder'  and '1-c0_0R0tOomtfuHcpi3Y08PHQXRuMG15' in parents") // list only files
                        .setQ(queryString)
                        .setSpaces("drive")
                        //.setFields("nextPageToken, files/*") // all fields
                        .setFields("files/name, nextPageToken, files/parents, files/id, files/size")
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
            System.out.println("*** pageToken: " + pageToken);
        } while (pageToken != null);
        // files is containing all files
        //return files;
        googleFileNames = new ArrayList<>();
        Log.i(TAG, "files is containing files or folders: " + files.size());
        StringBuilder sb = new StringBuilder();
        sb.append("Files found in GoogleDrive:\n\n");
        for (int i = 0; i < files.size(); i++) {

            // get parents
            com.google.api.services.drive.model.File fileResult = files.get(i);
            List<String> fileParents = fileResult.getParents();
            String parentList = "";
            if (fileParents != null) {
                int parentListSize = fileParents.size();
                //System.out.println("* there are parents: " + parentListSize);
                for (int j = 0; j < parentListSize; j++) {
                    parentList += fileParents.get(j) + " ";
                }
            }
            googleFileNames.add(files.get(i).getName());
            String content =
                    "name: " + files.get(i).getName() + " " +
                            " parents: " + parentList + " " +

                            " id: " + files.get(i).getId() + " " +
                            " size: " + files.get(i).getSize() + "\n";
            sb.append(content);
            sb.append("--------------------\n");
        }
        //System.out.println("fileList:\n" + sb.toString());
        String[] fileList;
        fileList = googleFileNames.toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, fileList);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //listFiles.setAdapter(adapter);
                //fileName.setText(sb.toString());
            }
        });

    }

    /**
     * list local files will retrieve all filenames in a local folder
     */
    private void listLocalFiles(Context context) {
        Log.i(TAG, "listLocalFiles");
        String recursiveFolder = localFolderPath.replaceFirst("root", "");
        File externalStorageDir = new File(Environment.getExternalStoragePublicDirectory("")
                , recursiveFolder);
        File[] files = externalStorageDir.listFiles();
        localFileNames = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                localFileNames.add(files[i].getName());
            }
        }
        String[] fileList;
        fileList = localFileNames.toArray(new String[0]);
        //System.out.println("fileList size: " + fileList.length);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, fileList);
        //listFiles.setAdapter(adapter);
        /*
        listFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                System.out.println("The selected folder is : " + selectedItem);
                Bundle bundle = new Bundle();
                bundle.putString("selectedFile", selectedItem);
                bundle.putString("selectedFolder", startDirectory);
                startMainActivityIntent.putExtras(bundle);
                startActivity(startMainActivityIntent);
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
                    //Toast.makeText(SimpleSyncLocalToGoogleDriveActivity.this, "Signed in as " + googleAccount.getEmail(), Toast.LENGTH_SHORT).show();

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

                    listAllFolder();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Unable to sign in.", exception);
                    Toast.makeText(SimpleSyncLocalToGoogleDriveActivity.this, "Unable to sign in: " + exception.getMessage(), Toast.LENGTH_LONG).show();
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
}