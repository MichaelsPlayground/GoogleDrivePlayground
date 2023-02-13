package de.androidcrypto.googledriveplayground;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class SingleEncryptedDownloadGoogleDriveToLocalActivity extends AppCompatActivity {

    private final String TAG = "SingleEncryptedDownloadGDToLocal";

    RadioButton showUpload, showLocal, showGoogle;
    //Button startUpload;
    ProgressBar progressBar;
    TextView tvProgress, tvProgressAbsolute;
    com.google.android.material.textfield.TextInputEditText passphraseInput;

    private Handler handler = new Handler();
    ListView listFiles;
    // default values
    boolean isSyncChecked = false;
    boolean isLocalChecked = false;
    boolean isGoogleChecked = true;

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
        setContentView(R.layout.activity_single_encrypted_download_google_drive_to_local);

        showUpload = findViewById(R.id.rbSingleDownloadGoogleUpload);
        showLocal = findViewById(R.id.rbSingleDownloadGoogleLocal);
        showGoogle = findViewById(R.id.rbSingleDownloadGoogleGoogle);
        //startUpload = findViewById(R.id.btnSinglleDownloadToGoogleUpload);
        listFiles = findViewById(R.id.lvSingleDownloadGoogle);
        progressBar = findViewById(R.id.pbSingleDownloadGoogle);
        tvProgress = findViewById(R.id.tvSingleDownloadGoogleProgress);
        tvProgressAbsolute = findViewById(R.id.tvSingleDownloadGoogleProgressAbsolute);
        passphraseInput = findViewById(R.id.etSingleDownloadGooglePassphrase);

        // init storageUtils
        storageUtils = new StorageUtils(getApplicationContext());
        // here we are loading the credentials for ENCRYPTED files
        localFolderName = storageUtils.getLocalStorageNameEncrypted();
        localFolderPath = storageUtils.getLocalStoragePathEncrypted();
        googleDriveFolderName = storageUtils.getGoogleDriveStorageNameEncrypted();
        googleDriveFolderId = storageUtils.getGoogleDriveStorageIdEncrypted();

        // sign in to GoogleDrive
        requestSignIn();

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

                // todo check that setItemOnClickListener is running ONLY when isLocalFolder is FALSE !
                if (isLocalFolder) {
                    // do nothing
                    listFiles.setOnItemClickListener(null);
                } else {
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
                            String selectedFileId = "";
                            System.out.println("The selected fileName is : " + selectedFileName);

                            if (googleFileNames.size() == 0) {
                                Log.i(TAG, "there are no files on Google Drivefor download");
                                return;
                            } else {
                                if (!isLocalFolder) {
                                    selectedFileId = googleFileIds.get(position);
                                }
                                downloadSingleEncryptedFileFromGoogleDriveSubfolderNew(view, selectedFileName, selectedFileId, passphraseChar);
                            }
                        }
                    });
                }
            }
        });

    }

    private void downloadSingleEncryptedFileFromGoogleDriveSubfolderNew(View view, String fileNameForDownload, String fileIdForDownload, char[] passphraseChar) {
        Log.i(TAG, "Download a single encrypted file from Google Drive subfolder");

        final int MAX = 1;
        final int progress = 1;
        progressBar.setMax(MAX);
        tvProgress.setText("(Info synchronization status)");
        tvProgressAbsolute.setText("(Info synchronization status)");
        Log.i(TAG, "start downloading the file " + fileNameForDownload + " with ID: " + fileIdForDownload);

        Thread DoEncryptedDownloadSubfolder = new Thread() {
            public void run() {
                Log.i(TAG, "running Thread DoBasicDownloadSubfolder");

                String recursiveFolder = localFolderPath.replaceFirst("root", "");
                File externalStorageDir = new File(Environment.getExternalStoragePublicDirectory("")
                        , recursiveFolder);
                File filePath = new File(externalStorageDir, fileNameForDownload);

                //String tempDownloadFilename = "temp.dat";
                File encryptedFilePath = new File(getFilesDir(), fileNameForDownload);

                OutputStream outputstream = null;
                try {
                    //outputstream = new FileOutputStream(filePath);
                    outputstream = new FileOutputStream(encryptedFilePath);
                    googleDriveServiceOwn.files().get(fileIdForDownload)
                            .executeMediaAndDownloadTo(outputstream);
                    outputstream.flush();
                    outputstream.close();
                    Log.i(TAG, "file download: " + fileNameForDownload);

                    // now decrypt and save in external storage
                    File decryptedFilePath = decryptFileFromInternalStorageToExternalStorage(filePath, encryptedFilePath, passphraseChar);
                    if (decryptedFilePath == null) {
                        Log.e(TAG, "there was an error during decryption");
                        return;
                    } else {
                        Log.i(TAG, "the decryption was successful");
                    }

                    // delete the temp file in internal storage

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(SimpleSyncGoogleDriveToLocalActivity.this, "file downloaded " + fileName + " to Internal Storage", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "ERROR: " + e.getMessage());
                    //throw new RuntimeException(e);
                }
                handler.post(new Runnable() {
                    public void run() {
                        progressBar.setProgress(progress);
                        int percent = (progress * 100) / MAX;

                        tvProgress.setText("Percent: " + percent + " %");
                        tvProgressAbsolute.setText("files downloaded: " + progress + " of total " + MAX + " files");
                        if (progress == MAX) {
                            tvProgress.setText("Completed!");
                            tvProgressAbsolute.setText("Completed download (" + MAX + ") files!");
                            //startSync.setEnabled(true);
                        }
                    }
                });
                showSnackbarGreen(view, "The file was downloaded");
            }

        };
        DoEncryptedDownloadSubfolder.start();
    }

    private File decryptFileFromInternalStorageToExternalStorage(File filePathToSave, File encryptedFilePath, char[] passphraseChar) {
        Log.i(TAG, "decryptInternalStorageFileToExternalStorage");
        Log.i(TAG, "encryptedFilePath: " + encryptedFilePath.getAbsolutePath());
        Log.i(TAG, "filePathToSave: " + filePathToSave.getAbsolutePath() + " passphraseChar: " + passphraseChar.toString());
        String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
        String AES_ALGORITHM = "AES/GCM/NOPadding";
        int ITERATIONS = 10000;
        int BUFFER_SIZE = 8096;
        //String tempFilename = "temp.dat";
        Cipher cipher;
        try {
            byte[] salt = new byte[32];
            byte[] nonce = new byte[12];
            cipher = Cipher.getInstance(AES_ALGORITHM);
            try (//FileInputStream in = getApplicationContext().openFileInput(tempFilename); // i don't care about the path as all is local
                 FileInputStream in = new FileInputStream(encryptedFilePath);
                 CipherInputStream cipherInputStream = new CipherInputStream(in, cipher);
                 FileOutputStream out = new FileOutputStream(filePathToSave)) // i don't care about the path as all is local
            {
                byte[] buffer = new byte[BUFFER_SIZE];
                in.read(nonce);
                in.read(salt);
                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
                KeySpec keySpec = new PBEKeySpec(passphraseChar, salt, ITERATIONS, 32 * 8);
                byte[] key = secretKeyFactory.generateSecret(keySpec).getEncoded();
                SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
                int nread;
                while ((nread = cipherInputStream.read(buffer)) > 0) {
                    out.write(buffer, 0, nread);
                }
                out.flush();
            } catch (IOException | InvalidAlgorithmParameterException | InvalidKeySpecException |
                     InvalidKeyException e) {
                Log.e(TAG, "ERROR on encryption: " + e.getMessage());
                return null;
                //throw new RuntimeException(e);
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            Log.e(TAG, "ERROR on encryption: " + e.getMessage());
            return null;
            //throw new RuntimeException(e);
        }
        return filePathToSave;
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
        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, fileList);

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
                    Toast.makeText(SingleEncryptedDownloadGoogleDriveToLocalActivity.this, "Unable to sign in: " + exception.getMessage(), Toast.LENGTH_LONG).show();
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