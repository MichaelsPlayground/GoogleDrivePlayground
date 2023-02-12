package de.androidcrypto.googledriveplayground;

import static de.androidcrypto.googledriveplayground.ViewUtils.showSnackbarRed;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SelectEncryptedFoldersActivity extends AppCompatActivity {

    private final String TAG = "SelectEncryptedFolders";

    com.google.android.material.textfield.TextInputEditText folderNameLocal;
    com.google.android.material.textfield.TextInputEditText folderNameGoogleDrive;

    Button selectLocalFolder, selectGoogleDriveFolder;

    private String storedFolderNameLocal, storedFolderPathLocal;
    private String storedGdFolderName, storedGdFolderId;

    StorageUtils storageUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_encrypted_folders);

        folderNameLocal = findViewById(R.id.etSelectEncryptedFoldersLocalName);
        folderNameGoogleDrive = findViewById(R.id.etSelectEncryptedFoldersGoogleDriveName);
        selectLocalFolder = findViewById(R.id.btnSelectEncryptedFoldersLocal);
        selectGoogleDriveFolder = findViewById(R.id.btnSelectEncryptedFoldersGoogleDrive);

        // init the StorageUtils
        storageUtils = new StorageUtils(getApplicationContext());

        if (storageUtils.isLocalStorageNameEncryptedAvailable()) {
            storedFolderNameLocal = storageUtils.getLocalStorageNameEncrypted();
            selectLocalFolder.setText(storedFolderNameLocal);
        }
        if (storageUtils.isLocalStoragePathEncryptedAvailable()) {
            storedFolderPathLocal = storageUtils.getLocalStoragePathEncrypted();
        }



        selectLocalFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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
}