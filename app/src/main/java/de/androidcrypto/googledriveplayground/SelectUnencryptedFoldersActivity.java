package de.androidcrypto.googledriveplayground;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SelectUnencryptedFoldersActivity extends AppCompatActivity {

    private final String TAG = "SelectUnencryptedFolders";

    com.google.android.material.textfield.TextInputEditText folderNameLocal;
    com.google.android.material.textfield.TextInputEditText folderNameGoogleDrive;

    Button selectLocalFolder, selectGoogleDriveFolder, returnToMainActivity;

    private String storedFolderNameLocal, storedFolderPathLocal;
    private String storedGdFolderName, storedGdFolderId;

    private String selectedFolderLocalFromIntent, parentFolderLocalFromIntent;
    private String googleDriveFolderIdFromIntent, googleDriveFolderNameFromIntent;

    private StorageUtils storageUtils;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_unencrypted_folders);

        folderNameLocal = findViewById(R.id.etSelectUnencryptedFoldersLocalName);
        folderNameGoogleDrive = findViewById(R.id.etSelectUnencryptedFoldersGoogleDriveName);
        selectLocalFolder = findViewById(R.id.btnSelectUnencryptedFoldersLocal);
        selectGoogleDriveFolder = findViewById(R.id.btnSelectUnencryptedFoldersGoogleDrive);
        returnToMainActivity = findViewById(R.id.btnSelectUnencryptedFoldersReturnToMain);

        // init the StorageUtils
        context = getApplicationContext();
        storageUtils = new StorageUtils(context);

        if (storageUtils.isLocalStorageNameAvailable()) {
            storedFolderNameLocal = storageUtils.getLocalStorageName();
            folderNameLocal.setText(storedFolderNameLocal);
        }
        if (storageUtils.isLocalStoragePathAvailable()) {
            storedFolderPathLocal = storageUtils.getLocalStoragePath();
        }
        if (storageUtils.isGoogleDriveStorageNameAvailable()) {
            storedGdFolderName = storageUtils.getGoogleDriveStorageName();
            folderNameGoogleDrive.setText(storedGdFolderName);
        }
        if (storageUtils.isGoogleDriveStorageIdAvailable()) {
            storedGdFolderId = storageUtils.getGoogleDriveStorageId();
        }


        /**
         * section for incoming intent handling
         */
        Bundle extras = getIntent().getExtras();
        System.out.println("get bundles");
        if (extras != null) {
            System.out.println("extras not null");

            // check first for IntentType
            String intentType = (String) getIntent().getSerializableExtra("IntentType");
            if (intentType.equals("selectUnencryptedSharedFolder")) {
                Log.i(TAG, "receive intent data for selectUnencryptedSharedFolder");
                selectedFolderLocalFromIntent = (String) getIntent().getSerializableExtra("browsedFolder");
                parentFolderLocalFromIntent = (String) getIntent().getSerializableExtra("parentFolder");
                if (parentFolderLocalFromIntent == null) {
                    Log.i(TAG, "from Intent: parent folder is empty");
                }
                if (selectedFolderLocalFromIntent == null) {
                    Log.i(TAG, "from Intent: selected folder is empty");
                }
                String folderSelectionString = "selected folder: " +
                        selectedFolderLocalFromIntent;
                folderNameLocal.setText(folderSelectionString);
                String resultString = "selectedFolder: " + selectedFolderLocalFromIntent + "\n"
                        + "parentFolder: " + parentFolderLocalFromIntent;
                Log.i(TAG, "resultString: " + resultString);
                storeTheLocalSelectedFolder();
            }
            if (intentType.equals("selectUnencryptedGoogleDriveFolder")) {
                Log.i(TAG, "receive intent data for selectUnencryptedGoogleDriveFolder");
                googleDriveFolderIdFromIntent = (String) getIntent().getSerializableExtra("googleDriveFolderId");
                googleDriveFolderNameFromIntent = (String) getIntent().getSerializableExtra("googleDriveFolderName");
                String folderSelectionString = "selectedFolder: " +
                        googleDriveFolderNameFromIntent;
                folderNameGoogleDrive.setText(folderSelectionString);
                String resultString = "selectedFolder: " + googleDriveFolderNameFromIntent + "\n"
                        + "folderId: " + googleDriveFolderIdFromIntent;
                Log.i(TAG, "resultString: " + resultString);
                storeTheGoogleDriveSelectedFolder();
            }

        }

        selectLocalFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "selectLocalFolder unencrypted");
                Intent intent = new Intent(SelectUnencryptedFoldersActivity.this, BrowseSharedFolder.class);
                Bundle bundle = new Bundle();
                bundle.putString("returnToActivity", "SelectUnencryptedFoldersActivity");
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        selectGoogleDriveFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "selectGoogleDriveLocalFolder unencrypted");
                Intent intent = new Intent(SelectUnencryptedFoldersActivity.this, BrowseGoogleDriveFolder.class);
                Bundle bundle = new Bundle();
                bundle.putString("returnToActivity", "SelectUnencryptedFoldersActivity");
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        returnToMainActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "return to main menu");
                Intent intent = new Intent(SelectUnencryptedFoldersActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
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
            //showSnackbarRed(view, "Google Drive folder name/ID is not stored yet, aborted");
            return false;
        }
        boolean localFolderStored = checkForStoredFolderLocal();
        if (!localFolderStored) {
            //showSnackbarRed(view, "Local folder name/path is not stored yet, aborted");
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
                        storageUtils.setLocalStorageName(selectedFolderLocalFromIntent);
                        String completePath = parentFolderLocalFromIntent + "/" + selectedFolderLocalFromIntent;
                        storageUtils.setLocalStoragePath(completePath);
                        Toast.makeText(context, "selected folder stored", Toast.LENGTH_SHORT).show();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        Log.i(TAG, "the storage of selectedFolder and parentFolder was denied");
                        Toast.makeText(context, "selected folder NOT stored", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        final String selectedFolderString = "You have selected the folder " +
                selectedFolderLocalFromIntent + " in " +
                parentFolderLocalFromIntent + " as local folder.\n" +
                "Do you want to store the folder ?";
        AlertDialog.Builder builder = new AlertDialog.Builder(SelectUnencryptedFoldersActivity.this);
        builder.setTitle("STORE LOCAL FOLDER FOR ENCRYPTED CONTENT");
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
                        Toast.makeText(context, "selected folder stored", Toast.LENGTH_SHORT).show();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        Log.i(TAG, "the storage of googleDriveFolderName and id was denied");
                        Toast.makeText(context, "selected folder NOT stored", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        final String selectedFolderString = "You have selected the folderName " +
                googleDriveFolderNameFromIntent + " with ID " +
                googleDriveFolderIdFromIntent + " as Google Drive folder.\n" +
                "Do you want to store the folder ?";
        AlertDialog.Builder builder = new AlertDialog.Builder(SelectUnencryptedFoldersActivity.this);
        builder.setTitle("STORE GOOGLE DRIVE FOLDER FOR ENCRYPTED CONTENT");
        builder.setMessage(selectedFolderString).setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, dialogClickListener).show();
        /*
        If you want to use the "yes" "no" literals of the user's language you can use this
        .setPositiveButton(android.R.string.yes, dialogClickListener)
        .setNegativeButton(android.R.string.no, dialogClickListener)
         */
    }
}