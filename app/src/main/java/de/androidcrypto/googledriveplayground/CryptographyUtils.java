package de.androidcrypto.googledriveplayground;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

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

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptographyUtils {

    private static final String TAG = "CryptographyUtils";
    private static final int MINIMUM_PASSPHRASE_LENGTH = 4;
    private static final int ITERATIONS = 10000;
    private static final int BUFFER_SIZE = 8196;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String AES_ALGORITHM = "AES/GCM/NOPadding";

    /**
     * this method will encrypt a file from external storage to internal storage / cache folder
     * the method is using AES GCM mode and PBKDF2 algorithm PBKDF2WithHmacSHA256 for key derivation
     * IMPORTANT: this method needs to run NOT on the main thread as it will block the UI otherwise
     * NOTE: if you don't enter the correct passphrase there is NO WAY to recover the file !!!
     *
     * @param context        the context of the calling activity
     * @param filePath       the path to the unencrypted file on external storage
     * @param passphraseChar the file will be encrypted with this passphrase
     * @return the file path to the encrypted file
     * if the passphraseChar is shorter than MINIMUM_PASSPHRASE_LENGTH or an error occurs it is returning null
     */
    public static File encryptExternalStorageFileToInternalStorage(@NonNull Context context, @NonNull File filePath, @NonNull char[] passphraseChar) {
        Log.i(TAG, "encryptExternalStorageFileToInternalStorage");
        Log.i(TAG, "filePath: " + filePath.getAbsolutePath() + " passphraseChar: " + passphraseChar.toString());
        if (passphraseChar.length < MINIMUM_PASSPHRASE_LENGTH) {
            Log.e(TAG, "the passwordChar is too short, aborted");
            return null;
        }

        String tempFilename = "temp.dat";
        Cipher cipher;
        try {
            SecureRandom secureRandom = new SecureRandom();
            byte[] salt = new byte[32];
            secureRandom.nextBytes(salt);
            byte[] nonce = new byte[12];
            secureRandom.nextBytes(nonce);
            cipher = Cipher.getInstance(AES_ALGORITHM);

            try (FileInputStream in = new FileInputStream(filePath);
                 FileOutputStream out = context.openFileOutput(tempFilename, Context.MODE_PRIVATE);
                 CipherOutputStream encryptedOutputStream = new CipherOutputStream(out, cipher);) {
                out.write(nonce);
                out.write(salt);
                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
                KeySpec keySpec = new PBEKeySpec(passphraseChar, salt, ITERATIONS, 32 * 8);
                byte[] key = secretKeyFactory.generateSecret(keySpec).getEncoded();
                SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
                byte[] buffer = new byte[BUFFER_SIZE];
                int nread;
                while ((nread = in.read(buffer)) > 0) {
                    encryptedOutputStream.write(buffer, 0, nread);
                }
                encryptedOutputStream.flush();
            } catch (IOException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                     InvalidKeyException e) {
                Log.e(TAG, "ERROR on encryption: " + e.getMessage());
                return null;
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "ERROR on encryption: " + e.getMessage());
            return null;
        }
        return new File(context.getFilesDir(), tempFilename);
    }

    /**
     * this method will decrypt a file from internal storage to external storage
     * the method is using AES GCM mode and PBKDF2 algorithm PBKDF2WithHmacSHA256 for key derivation
     * IMPORTANT: this method needs to run NOT on the main thread as it will block the UI otherwise
     * NOTE: if you don't enter the correct passphrase there is NO WAY to recover the file !!!
     *
     * @param filePathToSave the path to the unencrypted file on external storage
     * @param encryptedFilePath the path to the encrypted file on internal storage
     * @param passphraseChar the file will be decrypted with this passphrase
     * @return the file path to the decrypted file
     */
    public static File decryptFileFromInternalStorageToExternalStorage(@NonNull File filePathToSave, @NonNull File encryptedFilePath, @NonNull char[] passphraseChar) {
        Log.i(TAG, "decryptInternalStorageFileToExternalStorage");
        Log.i(TAG, "encryptedFilePath: " + encryptedFilePath.getAbsolutePath());
        Log.i(TAG, "filePathToSave: " + filePathToSave.getAbsolutePath() + " passphraseChar: " + passphraseChar.toString());
        if (passphraseChar.length < MINIMUM_PASSPHRASE_LENGTH) {
            Log.e(TAG, "the passwordChar is too short, aborted");
            return null;
        }
        Cipher cipher;
        try {
            byte[] salt = new byte[32];
            byte[] nonce = new byte[12];
            cipher = Cipher.getInstance(AES_ALGORITHM);
            try (
                 FileInputStream in = new FileInputStream(encryptedFilePath);
                 CipherInputStream cipherInputStream = new CipherInputStream(in, cipher);
                 FileOutputStream out = new FileOutputStream(filePathToSave))
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
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            Log.e(TAG, "ERROR on encryption: " + e.getMessage());
            return null;
        }
        return filePathToSave;
    }

    /**
     * this method is deleting the file used for encryption and decryption
     * @return TRUE if file could get deleted and FALSE if not
     */
    public static boolean deleteFileInInternalStorage(@NonNull Context context, @NonNull String fileName) {
        Log.i(TAG, "deleteFileInInternalStorage: " + fileName);
        boolean deletionResult = false;
        File file = new File(context.getFilesDir(), fileName);
        if (file.exists()) {
            deletionResult = file.delete();
        }
        return deletionResult;
    }
}
