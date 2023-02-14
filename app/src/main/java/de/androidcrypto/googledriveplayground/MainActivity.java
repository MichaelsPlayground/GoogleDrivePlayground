package de.androidcrypto.googledriveplayground;

import static android.os.Build.VERSION.SDK_INT;
import static de.androidcrypto.googledriveplayground.ViewUtils.showSnackbarGreen;
import static de.androidcrypto.googledriveplayground.ViewUtils.showSnackbarOrange;
import static de.androidcrypto.googledriveplayground.ViewUtils.showSnackbarRed;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "GD Playground Main";

    Button signIn, signOut, checkSignStatus;
    Button grantStoragePermissions, checkStoragePermissions;
    com.google.android.material.button.MaterialButton storagePermissionsGranted, userIsSignedIn;
    //Button queryFiles;
    //Button uploadFileFromInternalStorage;
    //Button basicUploadFromInternalStorage;
    //Button basicDownloadToInternalStorage;
    //Button basicListFiles, basicListFolder, basicListFilesInFolder;
    Button basicCreateFolder;
    //Button basicUploadFromInternalStorageToSubfolder;
    Button selectLocalFolder, selectGoogleDriveFolder, selectUnencryptedFolders, selectEncryptedFolders;
    Button syncLocalToGoogleDrive, syncGoogleDriveToLocal;
    Button uploadLocalToGoogleDrive, downloadGoogleDriveToLocal;
    Button syncEncryptedLocalToGoogleDrive, syncEncryptedGoogleDriveToLocal;
    Button uploadEncryptedLocalToGoogleDrive, downloadEncryptedGoogleDriveToLocal;

    Button deleteGoogleDriveFile;

    com.google.android.material.textfield.TextInputEditText fileName;

    String selectedFolderFromIntent, parentFolderFromIntent;
    String googleDriveFolderIdFromIntent, googleDriveFolderNameFromIntent;
    StorageUtils storageUtils;

    private View view;

    //private DriveServiceHelper mDriveServiceHelper;
    //private static final int REQUEST_CODE_SIGN_IN = 1;
    //private static final int REQUEST_CODE_PERMISSION = 101;
    private static final int REQUEST_CODE_PERMISSION_BELOW_SDK30 = 102;

    public Drive googleDriveServiceOwn = null;
    //GoogleSignInClient googleSignInClientForSignOut;
    //String googleIdToken = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        view = findViewById(R.id.viewMainLayout);

        grantStoragePermissions = findViewById(R.id.btnMainGrantStoragePermissions);
        checkStoragePermissions = findViewById(R.id.btnMainCheckStoragePermissions);

        storagePermissionsGranted = findViewById(R.id.btnMainStoragePermissionsGranted);
        userIsSignedIn = findViewById(R.id.btnMainUserIsSignedIn);

        signIn = findViewById(R.id.btnMainSignIn);
        signOut = findViewById(R.id.btnMainSignOut);
        checkSignStatus = findViewById(R.id.btnMainCheckSignStatus);
        //queryFiles = findViewById(R.id.btnMainQueryFiles);
        fileName = findViewById(R.id.etMainFilename);

        //uploadFileFromInternalStorage = findViewById(R.id.btnMainUploadFile);
        //basicUploadFromInternalStorage = findViewById(R.id.btnMainBasicUploadFile);
        //basicDownloadToInternalStorage = findViewById(R.id.btnMainBasicDownloadFile);
        //basicListFiles = findViewById(R.id.btnMainBasicListFiles);
        //basicListFolder = findViewById(R.id.btnMainBasicListFolder);
        //basicListFilesInFolder = findViewById(R.id.btnMainBasicListFilesInFolder);
        basicCreateFolder = findViewById(R.id.btnMainBasicCreateFolder);
        //basicUploadFromInternalStorageToSubfolder = findViewById(R.id.btnMainBasicUploadFileSubfolder);
        selectLocalFolder = findViewById(R.id.btnMainSelectLocalFolder);
        selectGoogleDriveFolder = findViewById(R.id.btnMainSelectGoogleDriveFolder);
        selectUnencryptedFolders = findViewById(R.id.btnMainSelectUnencryptedFolders);
        selectEncryptedFolders = findViewById(R.id.btnMainSelectEncryptedFolders);
        syncLocalToGoogleDrive = findViewById(R.id.btnMainStartSyncLocalToGoogleDrive);
        syncGoogleDriveToLocal = findViewById(R.id.btnMainStartSyncGoogleDriveToLocal);
        uploadLocalToGoogleDrive = findViewById(R.id.btnMainStartSingleUploadLocalToGoogleDrive);
        downloadGoogleDriveToLocal = findViewById(R.id.btnMainStartSingleDownloadGoogleDriveToLocal);
        deleteGoogleDriveFile = findViewById(R.id.btnMainStartDeleteGoogleDriveFile);
        syncEncryptedLocalToGoogleDrive = findViewById(R.id.btnMainStartEncryptedSyncLocalToGoogleDrive);
        syncEncryptedGoogleDriveToLocal = findViewById(R.id.btnMainStartEncryptedSyncGoogleDriveToLocal);
        uploadEncryptedLocalToGoogleDrive = findViewById(R.id.btnMainStartSingleEncryptedUploadLocalToGoogleDrive);
        downloadEncryptedGoogleDriveToLocal = findViewById(R.id.btnMainStartSingleEncryptedDownloadGoogleDriveToLocal);

        // init the StorageUtils
        storageUtils = new StorageUtils(getApplicationContext());

        /**
         * section for incoming intent handling
         */
        Bundle extras = getIntent().getExtras();
        System.out.println("get bundles");
        if (extras != null) {
            System.out.println("extras not null");

            // check first for IntentType
            String intentType = (String) getIntent().getSerializableExtra("IntentType");
            if (intentType.equals("selectSharedFolder")) {
                Log.i(TAG, "receive intent data for selectSharedFolder");
                selectedFolderFromIntent = (String) getIntent().getSerializableExtra("browsedFolder");
                parentFolderFromIntent = (String) getIntent().getSerializableExtra("parentFolder");
                if (parentFolderFromIntent == null) {
                    Log.i(TAG, "from Intent: parent folder is empty");
                }
                if (selectedFolderFromIntent == null) {
                    Log.i(TAG, "from Intent: selected folder is empty");
                }
                String folderSelectionString = "you selected the folder " +
                        selectedFolderFromIntent +
                        "\nthat is a subfolder of " +
                        parentFolderFromIntent;
                fileName.setText(folderSelectionString);
                String resultString = "selectedFolder: " + selectedFolderFromIntent + "\n"
                        + "parentFolder: " + parentFolderFromIntent;
                Log.i(TAG, "resultString: " + resultString);
                storeTheLocalSelectedFolder();
            }
            if (intentType.equals("selectGoogleDriveFolder")) {
                Log.i(TAG, "receive intent data for selectGoogleDriveFolder");
                googleDriveFolderIdFromIntent = (String) getIntent().getSerializableExtra("googleDriveFolderId");
                googleDriveFolderNameFromIntent = (String) getIntent().getSerializableExtra("googleDriveFolderName");
                String folderSelectionString = "you selected the folder " +
                        googleDriveFolderNameFromIntent +
                        "\nwith the ID " +
                        googleDriveFolderIdFromIntent;
                fileName.setText(folderSelectionString);
                String resultString = "selectedFolder: " + googleDriveFolderNameFromIntent + "\n"
                        + "folderId: " + googleDriveFolderIdFromIntent;
                Log.i(TAG, "resultString: " + resultString);
                storeTheGoogleDriveSelectedFolder();
            }

        }

        syncLocalToGoogleDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "syncLocalToGoogleDriveFolder");

                if (!checkForStoredFolders()) {
                    Log.i(TAG, "local and/or Google Drive folder not stored yet, aborted");
                    return;
                }

                // todo check internet connection state

                Intent intent = new Intent(MainActivity.this, SyncLocalToGoogleDriveActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        syncGoogleDriveToLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "syncGoogleDriveToLocalFolder");

                if (!checkForStoredFolders()) {
                    Log.i(TAG, "local and/or Google Drive folder not stored yet, aborted");
                    return;
                }

                // todo check internet connection state

                Intent intent = new Intent(MainActivity.this, SyncGoogleDriveToLocalActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        uploadLocalToGoogleDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "uploadLocalToGoogleDriveFolder");

                if (!checkForStoredFolders()) {
                    Log.i(TAG, "local and/or Google Drive folder not stored yet, aborted");
                    return;
                }

                // todo check internet connection state

                Intent intent = new Intent(MainActivity.this, SingleUploadLocalToGoogleDriveActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        downloadGoogleDriveToLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "downloadGoogleDriveToLocalFolder");

                if (!checkForStoredFolders()) {
                    Log.i(TAG, "local and/or Google Drive folder not stored yet, aborted");
                    return;
                }

                // todo check internet connection state

                Intent intent = new Intent(MainActivity.this, SingleDownloadGoogleDriveToLocalActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        syncEncryptedLocalToGoogleDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "syncEncryptedLocalToGoogleDriveFolder");

                if (!checkForStoredEncryptedFolders()) {
                    Log.i(TAG, "local and/or Google Drive Encrypted folder not stored yet, aborted");
                    return;
                }

                // todo check internet connection state

                Intent intent = new Intent(MainActivity.this, SyncLocalToGoogleDriveActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("SyncType", "encryptedSync");
                intent.putExtras(bundle);
                startActivity(intent);
                //finish();
            }
        });

        syncEncryptedGoogleDriveToLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "syncEncryptedGoogleDriveToLocalFolder");

                if (!checkForStoredEncryptedFolders()) {
                    Log.i(TAG, "local and/or Google Drive Encrypted folder not stored yet, aborted");
                    return;
                }
                // todo check internet connection state

                Intent intent = new Intent(MainActivity.this, SyncGoogleDriveToLocalActivity.class);
                startActivity(intent);
                Bundle bundle = new Bundle();
                bundle.putString("SyncType", "encryptedSync");
                intent.putExtras(bundle);
                startActivity(intent);
                //finish();
            }
        });

        uploadEncryptedLocalToGoogleDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "uploadEncryptedLocalToGoogleDriveFolder");

                if (!checkForStoredFolders()) {
                    Log.i(TAG, "local and/or Google Drive folder not stored yet, aborted");
                    return;
                }

                // check for a passphrase available

                // todo check internet connection state

                Intent intent = new Intent(MainActivity.this, SingleEncryptedUploadLocalToGoogleDriveActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        downloadEncryptedGoogleDriveToLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "downloadEncryptedGoogleDriveToLocalFolder");

                if (!checkForStoredFolders()) {
                    Log.i(TAG, "local and/or Google Drive folder not stored yet, aborted");
                    return;
                }

                // todo check internet connection state

                Intent intent = new Intent(MainActivity.this, SingleEncryptedDownloadGoogleDriveToLocalActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        /**
         * The BrowseGoogleDriveFolder class will show all folders in the root of Google Drive.
         * When selecting a folder the name is passed to ListGoogleDriveFolder where the user
         * can select this folder or browse to a subfolder (if available).
         * The selected folder is returned to MainActivity using an Intent Bundle
         */
        selectGoogleDriveFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "selectGoogleDriveFolder");
                if (!checkLoginStatus()) {
                    Log.e(TAG, "please sign in before list folder");
                    return;
                }
                //Bundle bundle = new Bundle();
                //bundle.putString("googleIdToken", googleIdToken);
                Intent intent = new Intent(MainActivity.this, BrowseGoogleDriveFolder.class);
                //intent.putExtras(bundle);
                startActivity(intent);
                //finish();
            }
        });

        /**
         * The BrowseGoogleDriveFolder class will show all folders in the root of Google Drive.
         * When selecting a folder the name is passed to ListGoogleDriveFolder where the user
         * can select this folder or browse to a subfolder (if available).
         * The selected folder is returned to MainActivity using an Intent Bundle
         */

        selectUnencryptedFolders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "selectUnencryptedFolders");
                if (!checkLoginStatus()) {
                    Log.e(TAG, "please sign in before list folder");
                    return;
                }

                Intent intent = new Intent(MainActivity.this, SelectUnencryptedFoldersActivity.class);
                startActivity(intent);
                /*
                Bundle bundle = new Bundle();
                bundle.putString("googleIdToken", googleIdToken);
                Intent intent = new Intent(MainActivity.this, BrowseGoogleDriveFolder.class);
                intent.putExtras(bundle);
                startActivity(intent);
                 */
                //finish();
            }
        });

        selectEncryptedFolders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "selectEncryptedFolders");
                if (!checkLoginStatus()) {
                    Log.e(TAG, "please sign in before list folder");
                    return;
                }

                Intent intent = new Intent(MainActivity.this, SelectEncryptedFoldersActivity.class);
                startActivity(intent);
                /*
                Bundle bundle = new Bundle();
                bundle.putString("googleIdToken", googleIdToken);
                Intent intent = new Intent(MainActivity.this, BrowseGoogleDriveFolder.class);
                intent.putExtras(bundle);
                startActivity(intent);
                 */
                //finish();
            }
        });


        /**
         * The BrowseSharedFolder class will show all folders in the root of the device.
         * When selecting a folder the name is passed to ListSharedFolder where the user
         * can select this folder or browse to a subfolder (if available).
         * The selected folder is returned to MainActivity using an Intent Bundle
         */
        selectLocalFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "selectLocalFolder");
                Intent intent = new Intent(MainActivity.this, BrowseSharedFolder.class);
                startActivity(intent);
                //finish();
            }
        });

        basicCreateFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Basic create a folder in Google Drive");
                if (!checkLoginStatus()) {
                    Log.e(TAG, "please sign in before upload a file");
                    return;
                }
                // https://developers.google.com/drive/api/guides/folder
                // before creating a new folder check if the folder is existing !!!
                // otherwise a second folder with the same name will be created
                Thread DoBasicCreateFolder = new Thread() {
                    public void run() {
                        Log.i(TAG, "running Thread DoBasicCreateFolder");
                        // File's metadata.
                        String folderName = "test2";

                        // check for existing folder
                        if (folderExist(folderName)) {
                            Log.i(TAG, "The folder is existing on GoogleDrive, aborted: " + folderName);
                        } else {
                            Log.i(TAG, "The folder is NOT existing on GoogleDrive, will create: " + folderName);


                            File fileMetadata = new File();
                            fileMetadata.setName(folderName);
                            fileMetadata.setMimeType("application/vnd.google-apps.folder");
                            try {
                                File file = googleDriveServiceOwn.files().create(fileMetadata)
                                        .setFields("id")
                                        .execute();
                                Log.i(TAG, "new folder created in GoogleDrive: " + folderName);
                                Log.i(TAG, "folderId is: " + file.getId());
                                //System.out.println("Folder ID: " + file.getId());
                                //return file.getId();
                            } catch (GoogleJsonResponseException e) {
                                // TODO(developer) - handle error appropriately
                                System.err.println("Unable to create folder: " + e.getDetails());
                                Log.e(TAG, "ERROR: " + e.getMessage());
                                return;
                                //throw e;
                            } catch (IOException e) {
                                //throw new RuntimeException(e);
                                Log.e(TAG, "ERROR: " + e.getMessage());
                                return;
                            }
                        }
                    }
                };
                DoBasicCreateFolder.start();
            }
        });

        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "sign out from Google Drive");
                //googleSignInClientForSignOut.signOut();
                Log.i(TAG, "user was signed out from Google Drive");
            }
        });

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "sign in to Google Drive");
                // Authenticate the user. For most apps, this should be done when the user performs an
                // action that requires Drive access rather than in onCreate.

                // first check an internet connection
                int conStat = getNetworkStatus();
                if (conStat == 0) {
                    showSnackbarRed(view, "Activate an internet connection before run any action on Google Drive");
                    return;
                }
                if (conStat == 1) {
                    showSnackbarGreen(view, "WIFI internet connection detected");
                }
                if (conStat > 1) {
                    showSnackbarOrange(view, "Your internet connection could be metered - think about before beginning any data transfer");
                }
                requestSignIn();
            }
        });

        checkSignStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "check sign status");
                    if (checkLoginStatus()) {
                        showSnackbarGreen(view, "User is signed in to Google Drive");
                        userIsSignedIn.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.green));
                    } else {
                        showSnackbarRed(view, "User is NOT signed in to Google Drive");
                        userIsSignedIn.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red));
                    }

            }
        });

        grantStoragePermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "grant storage permissions");
                // R = SDK 30
                if (SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        startActivity(new Intent(view.getContext(), MainActivity.class));
                    } else { //request for the permission
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                } else {
                    //below android 11=======
                    verifyPermissionsBelowSdk30();
                    //startActivity(new Intent(view.getContext(), MainActivity.class));
                    //ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
                }
            }
        });

        checkStoragePermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "check storage permissions");
                // R = SDK 30
                if (SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        showSnackbarGreen(view, "Storage permissions granted");
                        storagePermissionsGranted.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.green));
                    } else {
                        // storage permission not granted
                        showSnackbarRed(view, "Please grant storage permissions");
                        storagePermissionsGranted.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red));
                    }
                }
            }
        });

        deleteGoogleDriveFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "delete file on Google Drive");

                if (!checkForStoredFolderGoogleDrive()) {
                    Log.i(TAG, "Google Drive folder name/ID is not stored yet, aborted");
                    showSnackbarRed(view, "Google Drive folder name/ID is not stored yet, aborted");
                    return;
                }

                String selectedGoogleDriveId = storageUtils.getGoogleDriveStorageId();
                String selectedGoogleDriveFolderName = storageUtils.getGoogleDriveStorageName();
                // todo check internet connection state

                Bundle bundle = new Bundle();
                bundle.putString("googleDriveFolderId", selectedGoogleDriveId);
                bundle.putString("googleDriveFolderName", selectedGoogleDriveFolderName);
                Intent intent = new Intent(MainActivity.this, DeleteGoogleDriveFile.class);
                intent.putExtras(bundle);
                startActivity(intent);
                //finish();
            }
        });
    }

    /**
     * this method checks that local and Google Drive folders are stored
     * @return TRUE if everything is OK and FALSE if not
     */
    private boolean checkForStoredFolders() {
        // check that local and GoogleDrive folders are selected and stored
        boolean googleDriveFolderStored = checkForStoredFolderGoogleDrive();
        if (!googleDriveFolderStored) {
            showSnackbarRed(view, "Google Drive folder name/ID is not stored yet, aborted");
            return false;
        }
        boolean localFolderStored = checkForStoredFolderLocal();
        if (!localFolderStored) {
            showSnackbarRed(view, "Local folder name/path is not stored yet, aborted");
            return false;
        }
        return true;
    }

    private boolean checkForStoredFolderLocal() {
        // check that local folder is selected and stored
        boolean setLocalName = storageUtils.isLocalStorageNameAvailable();
        boolean setLocalPath = storageUtils.isLocalStoragePathAvailable();
        if (!setLocalName) {
            return false;
        }
        if (!setLocalPath) {
            return false;
        }
        return true;
    }

    private boolean checkForStoredFolderGoogleDrive() {
        // check that GoogleDrive folder is selected and stored
        boolean setGdName = storageUtils.isGoogleDriveStorageNameAvailable();
        boolean setGdId = storageUtils.isGoogleDriveStorageIdAvailable();
        if (!setGdName) {
            return false;
        }
        if (!setGdId) {
            return false;
        }
        return true;
    }

    /**
     * this method checks that ENCRYPTED local and Google Drive folders are stored
     * @return TRUE if everything is OK and FALSE if not
     */
    private boolean checkForStoredEncryptedFolders() {
        // check that local and GoogleDrive folders are selected and stored
        boolean googleDriveFolderStored = checkForStoredEncryptedFolderGoogleDrive();
        if (!googleDriveFolderStored) {
            showSnackbarRed(view, "Google Drive Encrypted folder name/ID is not stored yet, aborted");
            return false;
        }
        boolean localFolderStored = checkForStoredEncryptedFolderLocal();
        if (!localFolderStored) {
            showSnackbarRed(view, "Local Encrypted folder name/path is not stored yet, aborted");
            return false;
        }
        return true;
    }

    private boolean checkForStoredEncryptedFolderLocal() {
        // check that local folder is selected and stored
        boolean setLocalName = storageUtils.isLocalStorageNameEncryptedAvailable();
        boolean setLocalPath = storageUtils.isLocalStoragePathEncryptedAvailable();
        if (!setLocalName) {
            return false;
        }
        if (!setLocalPath) {
            return false;
        }
        return true;
    }

    private boolean checkForStoredEncryptedFolderGoogleDrive() {
        // check that GoogleDrive folder is selected and stored
        boolean setGdName = storageUtils.isGoogleDriveStorageNameEncryptedAvailable();
        boolean setGdId = storageUtils.isGoogleDriveStorageIdEncryptedAvailable();
        if (!setGdName) {
            return false;
        }
        if (!setGdId) {
            return false;
        }
        return true;
    }


    /**
     * section permission granting
     */

    private void verifyPermissionsBelowSdk30() {
        String[] permissions = {android.Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[1]) == PackageManager.PERMISSION_GRANTED) {
            showSnackbarGreen(view, "Storage permissions granted");
            storagePermissionsGranted.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.green));
            Log.i(TAG, "permissions were granted on device below SDK30");
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    REQUEST_CODE_PERMISSION_BELOW_SDK30);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION_BELOW_SDK30) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackbarGreen(view, "Storage permissions granted");
                storagePermissionsGranted.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.green));
                Log.i(TAG, "permissions were granted on device below SDK30");
            } else {
                Toast.makeText(this, "Grant Storage Permission is Required to use this function.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * section list files in Google Drive and local folders
     */

    private void listFilesInGoogleFolder(String folderId) {
        // https://developers.google.com/drive/api/v3/reference/files
        Log.i(TAG, "listFilesInFolder: " + folderId);
        List<File> files = new ArrayList<File>();
        String pageToken = null;
        do {
            FileList result = null;
            try {

                // build queryString
                String queryString = String.format("mimeType != 'application/vnd.google-apps.folder'  and '%s' in parents", folderId);
                //                      queryString: "mimeType != 'application/vnd.google-apps.folder'  and '1-c0_0R0tOomtfuHcpi3Y08PHQXRuMG15' in parents"
                //                             .setQ("mimeType != 'application/vnd.google-apps.folder'  and '1-c0_0R0tOomtfuHcpi3Y08PHQXRuMG15' in parents")
                System.out.println("* queryString: " + queryString);
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
            // todo NPE error handling
                            /*
                            for (File file : result.getFiles()) {
                                System.out.printf("Found file: %s (%s)\n",
                                        file.getName(), file.getId());
                            }

                             */
            if (result != null) {
                files.addAll(result.getFiles());
            }

            pageToken = result != null ? result.getNextPageToken() : null;
            System.out.println("*** pageToken: " + pageToken);
        } while (pageToken != null);
        // files is containing all files
        //return files;
        Log.i(TAG, "files is containing files or folders: " + files.size());
        StringBuilder sb = new StringBuilder();
        sb.append("Files found in GoogleDrive:\n\n");
        for (int i = 0; i < files.size(); i++) {

            // get parents
            File fileResult = files.get(i);
            List<String> fileParents = fileResult.getParents();
            String parentList = "";
            if (fileParents != null) {
                int parentListSize = fileParents.size();
                System.out.println("* there are parents: " + parentListSize);
                for (int j = 0; j < parentListSize; j++) {
                    parentList += fileParents.get(j) + " ";
                }
            }

            String content =
                    "name: " + files.get(i).getName() + " " +
                            " parents: " + parentList + " " +

                            " id: " + files.get(i).getId() + " " +
                            " size: " + files.get(i).getSize() + "\n";
            sb.append(content);
            sb.append("--------------------\n");
        }
        System.out.println("fileList:\n" + sb.toString());

    }


    /**
     * This method checks if a folder is existing on GoogleDrive
     *
     * @param folderName
     * @return true if folder is existing
     */
    private boolean folderExist(String folderName) {
        Log.i(TAG, "folderExist");
        //final String FOLDER_MIME_TYPE= "application/vnd.google-apps.folder";
        String pageToken = null;
        do {
            FileList result = null;
            try {
                result = googleDriveServiceOwn.files()
                        .list()
                        .setQ("mimeType = 'application/vnd.google-apps.folder'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
                //System.out.println("result.size: " + result.size());
                for (int i = 0; i < result.getFiles().size(); i++) {
                    String nameResult = result.getFiles().get(i).getName();
                    //System.out.println("* folderName:\n" + nameResult);
                    if (nameResult.equals(folderName)) return true;
                }
                pageToken = result.getNextPageToken();
            } catch (IOException e) {
                //throw new RuntimeException(e);
                Log.e(TAG, "ERROR: " + e.getMessage());
            }
        } while (pageToken != null);
        return false;
    }

    /**
     * This method returns the folderId on GoogleDrive
     *
     * @param folderName
     * @return tthe folderId, if not existing returns ""
     */
    private String getFolderId(String folderName) {
        Log.i(TAG, "getFolderId");
        String pageToken = null;
        do {
            FileList result = null;
            try {
                result = googleDriveServiceOwn.files()
                        .list()
                        .setQ("mimeType = 'application/vnd.google-apps.folder'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
                //System.out.println("result.size: " + result.size());
                for (int i = 0; i < result.getFiles().size(); i++) {
                    File fileResult = result.getFiles().get(i);
                    String nameResult = fileResult.getName();
                    //System.out.println("* folderName:\n" + nameResult);
                    if (nameResult.equals(folderName)) return fileResult.getId();
                }
                pageToken = result.getNextPageToken();
            } catch (IOException e) {
                //throw new RuntimeException(e);
                Log.e(TAG, "ERROR: " + e.getMessage());
            }
        } while (pageToken != null);
        return "";
    }

    private boolean folderExistOld(String folderName) {
        List<File> files = new ArrayList<File>();
        String pageToken = null;
        do {
            FileList result = null;
            try {
                result = googleDriveServiceOwn.files().list()
                        //.setQ("mimeType = 'application/vnd.google-apps.folder' and name = folderName")
                        .setQ("name = '$folderName'")
                        .setSpaces("drive")
                        //.setFields("nextPageToken, items(id, title)")
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
        Log.i(TAG, "files is containing folders: " + files.size());
        if (files.size() == 0) {
            return false;
        } else {
            return true;
        }
    }


    /**
     * section sign-in to Google Drive account
     */

    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        // DriveScopes.DRIVE shows ALL files
        // DriveScopes.DRIVE_FILE shows only files uploaded by this app

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE))
                        .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);
        if (client == null) {
            System.out.println("* client is null");
        } else {
            System.out.println("* client is not null: " + client.toString());
        }
        //googleSignInClientForSignOut = client;
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
                    Toast.makeText(MainActivity.this, "Signed in as " + googleAccount.getEmail(), Toast.LENGTH_SHORT).show();

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

                    googleDriveServiceOwn = googleDriveService;
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Unable to sign in.", exception);
                    Toast.makeText(MainActivity.this, "Unable to sign in: " + exception.getMessage(), Toast.LENGTH_LONG).show();
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

    /**
     * section for storage a selected folder
     */

    private void storeTheLocalSelectedFolder() {
        // https://stackoverflow.com/a/2478662/8166854
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Log.i(TAG, "the selectedFolder and parentFolder were stored in SharedPreferences");
                        //Yes button clicked
                        storageUtils.setLocalStorageName(selectedFolderFromIntent);
                        String completePath = parentFolderFromIntent + "/" + selectedFolderFromIntent;
                        storageUtils.setLocalStoragePath(completePath);
                        Toast.makeText(MainActivity.this, "selected folder stored", Toast.LENGTH_SHORT).show();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        Log.i(TAG, "the storage of selectedFolder and parentFolder was denied");
                        Toast.makeText(MainActivity.this, "selected folder NOT stored", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        final String selectedFolderString = "You have selected the folder " +
                selectedFolderFromIntent + " in " +
                parentFolderFromIntent + " as local folder.\n" +
                "Do you want to store the folder ?";
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(selectedFolderString).setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, dialogClickListener).show();
        /*
        If you want to use the "yes" "no" literals of the user's language you can use this
        .setPositiveButton(android.R.string.yes, dialogClickListener)
        .setNegativeButton(android.R.string.no, dialogClickListener)
         */
    }

    private void storeTheGoogleDriveSelectedFolder() {
        // https://stackoverflow.com/a/2478662/8166854
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Log.i(TAG, "the googleDriveFolderName and googleDriveFolderId were stored in SharedPreferences");
                        //Yes button clicked
                        storageUtils.setGoogleDriveStorageName(googleDriveFolderNameFromIntent);
                        storageUtils.setGoogleDriveStorageId(googleDriveFolderIdFromIntent);
                        Toast.makeText(MainActivity.this, "selected folder stored", Toast.LENGTH_SHORT).show();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        Log.i(TAG, "the storage of googleDriveFolderName and id was denied");
                        Toast.makeText(MainActivity.this, "selected folder NOT stored", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        final String selectedFolderString = "You have selected the folderName " +
                googleDriveFolderNameFromIntent + " with ID " +
                googleDriveFolderIdFromIntent + " as Google Drive folder.\n" +
                "Do you want to store the folder ?";
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(selectedFolderString).setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, dialogClickListener).show();
        /*
        If you want to use the "yes" "no" literals of the user's language you can use this
        .setPositiveButton(android.R.string.yes, dialogClickListener)
        .setNegativeButton(android.R.string.no, dialogClickListener)
         */
    }

    /**
     * section utils
     */

    private boolean checkLoginStatus() {
        if (googleDriveServiceOwn == null) {
            Log.e(TAG, "please sign in before list folder");
            showSnackbarRed(view, "Please sign in before run any action");
            return false;
        } else return true;
    }

    /**
     * getNetworkStatus checks for the connection state
     * @return values
     * 0 = no active connection
     * 1 = Wifi connection
     * 2 = Cellular connection
     * 9 = none of them
     */
    private int getNetworkStatus() {
        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        Network currentNetwork = connectivityManager.getActiveNetwork();
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);
        if (caps == null) {
            Log.i(TAG, "getNetworkStatus: 0 no internet connection");
            return 0;
        }
        boolean hasWifiConnection = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean hasCellularConnection = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        boolean hasActiveConnection = isOnline();
        if (!hasActiveConnection) {
            Log.i(TAG, "getNetworkStatus: 0 no internet connection");
            return 0;
        }
        if (hasCellularConnection) {
            Log.i(TAG, "getNetworkStatus: 2 cellular internet connection");
            return 2;
        }
        if (hasWifiConnection) {
            Log.i(TAG, "getNetworkStatus: 1 wifi internet connection");
            return 1;
        }
        Log.i(TAG, "getNetworkStatus: 9 not identified internet connection");
        return 9;
    }

    /**
     * This method checks if we can ping to google.com - if yes we do have an active internet connection
     * returns true if there is an active internet connection
     * returns false if there is no active internet connection
     * https://stackoverflow.com/a/45777087/8166854 by sami rahimi
     */
    public Boolean isOnline() {
        try {
            Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.com");
            int returnVal = p1.waitFor();
            boolean reachable = (returnVal==0);
            return reachable;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

}