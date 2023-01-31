package de.androidcrypto.googledriveplayground;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

//import androidx.security.crypto.EncryptedSharedPreferences;
//import androidx.security.crypto.MasterKeys;


public class StorageUtils {

    /**
     *
     * NOTE: This class will store credentials UNENCRYPTED - all references to Encrypted Shared Preferences are commented out !
     *
     * This is a utility class to securely store credentials like developer key, user name and user password using
     * SharedEncryptedPreferences
     *
     * Security warning: as we are working with EncryptedSharedPreferences the Masterkey is stored in Android's keystore
     * that is not backuped. If your smartphone is unusable for any reason you do not have anymore access to the stored
     * data
     *
     */

    private static final String TAG = "StorageUtils nocrypt";

    private final Context mContext;
    //private String masterKeyAlias;
    private SharedPreferences sharedPreferences; // for credentials
    private boolean libraryIsReady = false;

    // used for credentials
    private final String UNENCRYPTED_PREFERENCES_FILENAME = "shared_prefs";
    private final String LOCAL_STORAGE_PATH = "local_path";
    private final String LOCAL_STORAGE_NAME = "local_name";
    private final String GOOGLE_DRIVE_STORAGE_ID = "google_drive_id";
    private final String GOOGLE_DRIVE_STORAGE_NAME = "google_drive_name";


    /*
    private final String ENCRYPTED_PREFERENCES_FILENAME = "secret_shared_prefs";
    private final String DEVELOPER_KEY_NAME = "developer_key";
    private final String USER_NAME = "user_name";
    private final String PASSWORD_NAME = "user_password";
    private final String USER_KEY = "user_key";
     */

    public StorageUtils(Context context) {
        Log.d(TAG, "StorageUtils construction");
        this.mContext = context;
        //try {
            //masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            //sharedPreferences = setupSharedPreferences(mContext, masterKeyAlias, ENCRYPTED_PREFERENCES_FILENAME);
            sharedPreferences = mContext.getSharedPreferences(UNENCRYPTED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
            libraryIsReady = true;
        /*
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error on initialization of StorageUtils: " + e.getMessage());
            libraryIsReady = false;
            e.printStackTrace();
        }
         */
    }

    /*
    private SharedPreferences setupSharedPreferences (Context context, String keyAlias, String preferencesFilename) throws GeneralSecurityException, IOException {
        return EncryptedSharedPreferences.create(
                preferencesFilename,
                keyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
*/
    public boolean isStorageLibraryReady() {
        return libraryIsReady;
    }

    /**
     * local storage path & name
     */

    public boolean isLocalStoragePathAvailable() {
        if (TextUtils.isEmpty(getLocalStoragePath())) {
            Log.d(TAG, "local storage path is not available");
            return false;
        } else {
            Log.d(TAG, "local storage path is available");
            return true;
        }
    }

    public boolean setLocalStoragePath(String path) {
        if (TextUtils.isEmpty(path)) {
            Log.e(TAG, "local storage path is empty, storage aborted");
            return false;
        }
        try {
            sharedPreferences.edit().putString(LOCAL_STORAGE_PATH, path).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error on path storage: " + e.getMessage());
            return false;
        }
        Log.d(TAG, "local storage path successful stored");
        return true;
    }

    public String getLocalStoragePath() {
        return sharedPreferences.getString(LOCAL_STORAGE_PATH, "");
    }

    public boolean isLocalStorageNameAvailable() {
        if (TextUtils.isEmpty(getLocalStorageName())) {
            Log.d(TAG, "local storage name is not available");
            return false;
        } else {
            Log.d(TAG, "local storage name is available");
            return true;
        }
    }

    public boolean setLocalStorageName(String name) {
        if (TextUtils.isEmpty(name)) {
            Log.e(TAG, "local storage name is empty, storage aborted");
            return false;
        }
        try {
            sharedPreferences.edit().putString(LOCAL_STORAGE_NAME, name).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error on name storage: " + e.getMessage());
            return false;
        }
        Log.d(TAG, "local storage name successful stored");
        return true;
    }

    public String getLocalStorageName() {
        return sharedPreferences.getString(LOCAL_STORAGE_NAME, "");
    }

    /**
     * Google Drive storage path & name
     */

    public boolean isGoogleDriveStorageIdAvailable() {
        if (TextUtils.isEmpty(getGoogleDriveStorageId())) {
            Log.d(TAG, "Google Drive storage id is not available");
            return false;
        } else {
            Log.d(TAG, "Google Drive storage id is available");
            return true;
        }
    }

    public boolean setGoogleDriveStorageId(String id) {
        if (TextUtils.isEmpty(id)) {
            Log.e(TAG, "Google Drive storage id is empty, storage aborted");
            return false;
        }
        try {
            sharedPreferences.edit().putString(GOOGLE_DRIVE_STORAGE_ID, id).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error on id storage: " + e.getMessage());
            return false;
        }
        Log.d(TAG, "Google Drive storage id successful stored");
        return true;
    }

    public String getGoogleDriveStorageId() {
        return sharedPreferences.getString(GOOGLE_DRIVE_STORAGE_ID, "");
    }

    public boolean isGoogleDriveStorageNameAvailable() {
        if (TextUtils.isEmpty(getGoogleDriveStorageName())) {
            Log.d(TAG, "Google Drive storage name is not available");
            return false;
        } else {
            Log.d(TAG, "Google Drive storage name is available");
            return true;
        }
    }

    public boolean setGoogleDriveStorageName(String name) {
        if (TextUtils.isEmpty(name)) {
            Log.e(TAG, "Google Drive storage name is empty, storage aborted");
            return false;
        }
        try {
            sharedPreferences.edit().putString(GOOGLE_DRIVE_STORAGE_NAME, name).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error on name storage: " + e.getMessage());
            return false;
        }
        Log.d(TAG, "Google Drive storage name successful stored");
        return true;
    }

    public String getGoogleDriveStorageName() {
        return sharedPreferences.getString(GOOGLE_DRIVE_STORAGE_NAME, "");
    }

    /**
     * developer key utils
     */
/*
    public boolean isDeveloperKeyAvailable() {
        if (TextUtils.isEmpty(getDeveloperKey())) {
            Log.d(TAG, "developer key is not available");
            return false;
        } else {
            Log.d(TAG, "developer key is available");
            return true;
        }
    }

    public boolean setDeveloperKey(String devKey) {
        if (TextUtils.isEmpty(devKey)) {
            Log.e(TAG, "developerKey is empty, storage aborted");
            return false;
        }
        try {
            sharedPreferences.edit().putString(DEVELOPER_KEY_NAME, devKey).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error on key storage: " + e.getMessage());
            return false;
        }
        Log.d(TAG, "developer key sucessful stored");
        return true;
    }

    public String getDeveloperKey() {
        return sharedPreferences.getString(DEVELOPER_KEY_NAME, "");
    }

 */

    /**
     * user name utils
     */
/*
    public boolean isUserNameAvailable() {
        if (TextUtils.isEmpty(getUserName())) {
            Log.d(TAG, "user name is not available");
            return false;
        } else {
            Log.d(TAG, "user name is available");
            return true;
        }
    }

    public boolean setUserName(String userName) {
        if (TextUtils.isEmpty(userName)) {
            Log.e(TAG, "user name is empty, storage aborted");
            return false;
        }
        try {
            sharedPreferences.edit().putString(USER_NAME, userName).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error on user name storage: " + e.getMessage());
            return false;
        }
        Log.d(TAG, "user name sucessful stored");
        return true;
    }

    public String getUserName() {
        return sharedPreferences.getString(USER_NAME, "");
    }
*/
    /**
     * user password utils
     */
/*
    public boolean isUserPasswordAvailable() {
        if (TextUtils.isEmpty(getUserPassword())) {
            Log.d(TAG, "user password is not available");
            return false;
        } else {
            Log.d(TAG, "user password is available");
            return true;
        }
    }

    public boolean setUserPassword(String userPassword) {
        if (TextUtils.isEmpty(userPassword)) {
            Log.e(TAG, "user password is empty, storage aborted");
            return false;
        }
        try {
            sharedPreferences.edit().putString(PASSWORD_NAME, userPassword).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error on user password storage: " + e.getMessage());
            return false;
        }
        Log.d(TAG, "user password sucessful stored");
        return true;
    }

    public String getUserPassword() {
        return sharedPreferences.getString(PASSWORD_NAME, "");
    }
*/
    /**
     * This method checks that a developer key, a user name and user password were stored
     * returns TRUE if all are set or FALSE when one or more are not set
     */
/*
    public boolean checkForCredentials() {
        if (!isDeveloperKeyAvailable()) {
            Log.d(TAG, "the developer key is not available");
            return false;
        }
        if (!isUserNameAvailable()) {
            Log.d(TAG, "the user name is not available");
            return false;
        }
        if (!isUserPasswordAvailable()) {
            Log.d(TAG, "the user password is not available");
            return false;
        }
        return true;
    }
*/
    /**
     * user key utils
     */
/*
    public boolean isUserKeyAvailable() {
        if (TextUtils.isEmpty(getUserKey())) {
            Log.d(TAG, "user key is not available");
            return false;
        } else {
            Log.d(TAG, "user key is available");
            return true;
        }
    }

    public boolean deleteUserKey() {
        try {
            sharedPreferences.edit().putString(USER_KEY, "").apply();
        } catch (Exception e) {
            Log.e(TAG, "Error on user key deletion: " + e.getMessage());
            return false;
        }
        Log.d(TAG, "user key sucessful deleted");
        return true;
    }

    public boolean setUserKey(String userKey) {
        if (TextUtils.isEmpty(userKey)) {
            Log.e(TAG, "user key is empty, storage aborted");
            return false;
        }
        try {
            sharedPreferences.edit().putString(USER_KEY, userKey).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error on user key storage: " + e.getMessage());
            return false;
        }
        Log.d(TAG, "user key sucessful stored");
        return true;
    }

    public String getUserKey() {
        return sharedPreferences.getString(USER_KEY, "");
    }
*/
    /**
     * section for utils
     */

    public static String base64Encoding(byte[] input) {
        return Base64.encodeToString(input, Base64.NO_WRAP);
    }

    public static byte[] base64Decoding(String input) {
        return Base64.decode(input, Base64.NO_WRAP);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    public static byte[] hexToBytes(String str) {
        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2),
                    16);
        }
        return bytes;
    }

    public static String hexToBase64(String hexString) {
        return base64Encoding(hexToBytes(hexString));
    }

    public static String base64ToHex(String base64String) {
        return bytesToHex(base64Decoding(base64String));
    }
}
