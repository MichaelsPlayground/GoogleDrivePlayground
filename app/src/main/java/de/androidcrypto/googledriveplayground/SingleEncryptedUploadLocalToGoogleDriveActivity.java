package de.androidcrypto.googledriveplayground;

import static de.androidcrypto.googledriveplayground.CryptographyUtils.deleteFileInInternalStorage;
import static de.androidcrypto.googledriveplayground.CryptographyUtils.encryptExternalStorageFileToInternalStorage;
import static de.androidcrypto.googledriveplayground.ViewUtils.showSnackbarGreen;
import static de.androidcrypto.googledriveplayground.ViewUtils.showSnackbarRed;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class SingleEncryptedUploadLocalToGoogleDriveActivity extends AppCompatActivity {

    private final String TAG = "SingleEncryptedUploadLocalToGD";

    RadioButton showUpload, showLocal, showGoogle;
    //Button startUpload;
    ProgressBar progressBar;
    com.google.android.material.textfield.TextInputEditText passphraseInput;
    TextView tvProgress, tvProgressAbsolute;
    private Handler handler = new Handler();
    ListView listFiles;
    // default values
    boolean isSyncChecked = false;
    boolean isLocalChecked = true;
    boolean isGoogleChecked = false;

    ArrayList<String> uploadFileNames = new ArrayList<>();
    ArrayList<String> localFileNames = new ArrayList<>();
    ArrayList<String> googleFileNames = new ArrayList<>();
    ArrayList<String> googleFileIds = new ArrayList<>();

    String localFolderName, localFolderPath;
    String googleDriveFolderName, googleDriveFolderId;
    StorageUtils storageUtils;

    private Drive googleDriveServiceOwn = null;

    private final int MINIMUM_PASSPHRASE_LENGTH = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_encrypted_upload_local_to_google_drive);

        showUpload = findViewById(R.id.rbSingleUploadToGoogleUpload);
        showLocal = findViewById(R.id.rbSingleUploadToGoogleSyncLocal);
        showGoogle = findViewById(R.id.rbSingleUploadToGoogleSyncGoogle);
        //startUpload = findViewById(R.id.btnSingleUploadToGoogleUpload);
        listFiles = findViewById(R.id.lvSingleUploadToGoogle);
        progressBar = findViewById(R.id.pbSingleUploadToGoogleSyncGoogle);
        tvProgress = findViewById(R.id.tvSingleUploadToGoogleSyncGoogleProgress);
        tvProgressAbsolute = findViewById(R.id.tvSingleUploadToGoogleSyncGoogleProgressAbsolute);
        passphraseInput = findViewById(R.id.etSingleUploadToGooglePassphrase);

        // init storageUtils
        storageUtils = new StorageUtils(getApplicationContext());
        // here we are loading the credentials for ENCRYPTED files
        localFolderName = storageUtils.getLocalStorageNameEncrypted();
        localFolderPath = storageUtils.getLocalStoragePathEncrypted();
        googleDriveFolderName = storageUtils.getGoogleDriveStorageNameEncrypted();
        googleDriveFolderId = storageUtils.getGoogleDriveStorageIdEncrypted();

        // sign in to GoogleDrive
        requestSignIn();

        /*
        showUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSyncChecked = true;
                isLocalChecked = false;
                isGoogleChecked = false;
                startUpload.setEnabled(true);
                listAllFolder();
            }
        });
         */

        showLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSyncChecked = false;
                isLocalChecked = true;
                isGoogleChecked = false;
                //startUpload.setEnabled(false);
                listAllFolder();
            }
        });

        showGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSyncChecked = false;
                isLocalChecked = false;
                isGoogleChecked = true;
                //startUpload.setEnabled(false);
                listAllFolder();
            }
        });

        /*
        startUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "start simple upload");

                // todo run the upload process, check that uploadFileNames list is not empty :-)
                if (uploadFileNames.size() < 1) {
                    Log.i(TAG, "no files to sync, aborted");
                    Snackbar snackbar = Snackbar.make(view, "No files to sync", Snackbar.LENGTH_LONG);
                    snackbar.setBackgroundTint(ContextCompat.getColor(SimpleUploadLocalToGoogleDriveActivity.this, R.color.red));
                    snackbar.show();
                    return;
                }
                //
                // uploadSingleFileToGoogleDriveSubfolderNew(view);
            }
        });
*/
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


    private void showFiles(ArrayList<String> fileNames, boolean isLocalFolder) {
        Log.i(TAG, "showFiles");
        String[] fileList;
        fileList = fileNames.toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, fileList);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listFiles.setAdapter(adapter);

                // todo check that setItemOnClickListener is running ONLY when isLocalFolder is TRUE !

                listFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        // first check that there is a passphrase set
                        // get the passphrase from EditText as char array
                        int passphraseLength = passphraseInput.length();
                        if (passphraseLength < MINIMUM_PASSPHRASE_LENGTH) {
                            showSnackbarRed(view, "The entered passphrase is too short, aborted");
                            return;
                        }
                        char[] passphraseChar = new char[passphraseLength];
                        passphraseInput.getText().getChars(0, passphraseLength, passphraseChar, 0);

                        String selectedFileName = (String) parent.getItemAtPosition(position);
                        System.out.println("The selected fileName is : " + selectedFileName);

                        if (googleFileNames.size() == 0) {
                            Log.i(TAG, "there are no files on Google Drive, uploading");
                            //uploadSingleFileToGoogleDriveSubfolderNew(view, selectedFileName);
                            uploadEncryptedSingleFileToGoogleDriveSubfolderNew(view, selectedFileName, passphraseChar);
                        } else {
                            boolean fileIsExisiting = false;
                            String googleFileIdToDelete = "";
                            for (int i = 0; i < googleFileNames.size(); i++) {
                                int index = googleFileNames.indexOf(selectedFileName);
                                if (index > -1) {
                                    fileIsExisiting = true;
                                    googleFileIdToDelete = googleFileIds.get(i);
                                }
                            }
                            if (fileIsExisiting) {
                                Log.i(TAG, "the selectedFileName does exist on Google Drive, deleting the file first");
                                //deleteGoogleDriveFile(view, googleFileIdToDelete, selectedFileName);
                                deleteEncryptedGoogleDriveFile(view, googleFileIdToDelete, selectedFileName, passphraseChar);
                            } else {
                                Log.i(TAG, "the selectedFileName does not exist on Google Drive, start uploding");
                                //uploadSingleFileToGoogleDriveSubfolderNew(view, selectedFileName);
                                uploadEncryptedSingleFileToGoogleDriveSubfolderNew(view, selectedFileName, passphraseChar);
                            }
                        }
                    }
                });
            }
        });

    }

    private void uploadEncryptedSingleFileToGoogleDriveSubfolderNew(View view, String fileNameForUpload, char[] passphraseChar) {
        Log.i(TAG, "upload an encrypted single file from internal storage to subfolder");

        final int MAX = 1;
        progressBar.setMax(MAX);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvProgress.setText("(Info synchronization status)");
                tvProgressAbsolute.setText("(Info synchronization status)");
            }
        });
        Log.i(TAG, "start upload the file " + fileNameForUpload);

        /*
        if (!checkLoginStatus()) {
            Log.e(TAG, "please sign in before upload a file");
            return;
        }

         */
        // https://developers.google.com/drive/api/guides/manage-uploads
        Thread DoEncryptedUploadSubfolder = new Thread() {
            public void run() {
                Log.i(TAG, "running Thread DoEncryptedUploadSubfolder");

                //for (int i = 0; i < numberOfFilesToSync; i++) {
                final int progress = 1;

                String folderId = googleDriveFolderId;
                if (folderId.equals("")) {
                    Log.e(TAG, "The destination folder does not exist, abort: " + fileNameForUpload);
                    return;
                } else {
                    Log.i(TAG, "The destination folder is existing, start uploading to folderId: " + folderId);
                }

                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                //fileMetadata.setName("photo.jpg");
                fileMetadata.setName(fileNameForUpload);
                fileMetadata.setParents(Collections.singletonList(folderId));
                // File's content.
                String recursiveFolder = localFolderPath.replaceFirst("root", "");
                File externalStorageDir = new File(Environment.getExternalStoragePublicDirectory("")
                        , recursiveFolder);
                File filePath = new File(externalStorageDir, fileNameForUpload);
                if (filePath.exists()) {
                    Log.i(TAG, "filePath " + fileNameForUpload + " is existing");
                } else {
                    Log.e(TAG, "filePath " + fileNameForUpload + " is NOT existing");
                    return;
                }

                // here we are encrypting the file from external storage to internal storage / cache and
                // run the upload from there
                File encryptedFilePath = encryptExternalStorageFileToInternalStorage(getApplicationContext(), filePath, passphraseChar);
                if (encryptedFilePath == null) {
                    Log.i(TAG, "Error in encrypting the file, aborted");
                    return;
                }
                Log.i(TAG, "The file was encrypted to " + encryptedFilePath.getAbsolutePath());

                // get media type
                Uri uri = Uri.fromFile(filePath);
                String mimeType = getMimeType(uri);

                //FileContent mediaContent = new FileContent(mimeType, filePath);
                FileContent mediaContent = new FileContent(mimeType, encryptedFilePath);
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
                deleteFileInInternalStorage(getApplicationContext(), "temp.dat");
                handler.post(new Runnable() {
                    public void run() {
                        progressBar.setProgress(progress);
                        int percent = (progress * 100) / MAX;

                        tvProgress.setText("Percent: " + percent + " %");
                        tvProgressAbsolute.setText("files uploaded: " + progress + " of total " + MAX + " files");
                        if (progress == MAX) {
                            tvProgress.setText("Completed!");
                            tvProgressAbsolute.setText("Completed upload (" + MAX + ") files!");
                            //startSync.setEnabled(true);
                        }
                    }
                });
                //}
                showSnackbarGreen(view, "The file was uploaded");
                listAllFolder();
            }
        };
        DoEncryptedUploadSubfolder.start();
    }

    private void deleteEncryptedGoogleDriveFile(View view, String fileToDeleteId, String fileNameToUpload, char[] passphraseChar) {
        Log.i(TAG, "deleteGoogleDriveFile id: " + fileToDeleteId);

        Thread DoDeleteEncryptedGoogleDriveFile = new Thread() {
            public void run() {
                try {
                    googleDriveServiceOwn.files().delete(fileToDeleteId).execute();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SingleEncryptedUploadLocalToGoogleDriveActivity.this, "selected file deleted", Toast.LENGTH_SHORT).show();
                        }
                    });
                    // todo upload file now
                    uploadEncryptedSingleFileToGoogleDriveSubfolderNew(view, fileNameToUpload, passphraseChar);
                    //listFiles();
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SingleEncryptedUploadLocalToGoogleDriveActivity.this, "ERROR: could not delete the selected file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                    //throw new RuntimeException(e);
                }
            }
        };
        DoDeleteEncryptedGoogleDriveFile.start();
    }

    private void listGoogleDriveFiles() {
        Log.i(TAG, "listGoogleDriveFiles");

        Thread DoBasicListFilesInFolder = new Thread() {
            public void run() {
                Log.i(TAG, "running Thread DoBasicListFilesInFolder");
                listFilesInGoogleFolder(googleDriveFolderId);
                uploadFileNames = new ArrayList<>();
                System.out.println("* uploadFileNames old size: " + uploadFileNames.size());
                System.out.println("* localFileNames size: " + localFileNames.size());
                System.out.println("* GoogleFileNames size: " + googleFileNames.size());
                // find files from local in GoogleDrive list
                for (int i = 0; i < localFileNames.size(); i++) {
                    int index = googleFileNames.indexOf(localFileNames.get(i));
                    // if index = -1 the localFileName is NOT in the googleDriveFileNames list
                    if (index < 0) {
                        // add the entry to the syncs list
                        uploadFileNames.add(localFileNames.get(i));
                    }
                }
                //System.out.println("* syncFileNames new size: " + syncFileNames.size());
                // show data depending on radioGroup
                if (isSyncChecked) {
                    showFiles(uploadFileNames, false);
                }
                if (isLocalChecked) {
                    showFiles(localFileNames, true);
                }
                if (isGoogleChecked) {
                    showFiles(googleFileNames, false);
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
        googleFileIds = new ArrayList<>();
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
            googleFileIds.add(files.get(i).getId());
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
     * Starts a sign-in activity
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
        googleSignInStartActivityIntent.launch(client.getSignInIntent());
    }

    /**
     * Handles the {@code result} of a completed sign-in activity initiated from {@link
     * #requestSignIn()}.
     */
    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.i(TAG, "Signed in as " + googleAccount.getEmail());
                    //Toast.makeText(SimpleUploadLocalToGoogleDriveActivity.this, "Signed in as " + googleAccount.getEmail(), Toast.LENGTH_SHORT).show();

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
                    Toast.makeText(SingleEncryptedUploadLocalToGoogleDriveActivity.this, "Unable to sign in: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    ActivityResultLauncher<Intent> googleSignInStartActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    handleSignInResult(result.getData());
                }
            });
}