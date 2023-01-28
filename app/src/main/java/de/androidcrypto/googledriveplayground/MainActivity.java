package de.androidcrypto.googledriveplayground;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "GD Playground Main";

    private final String basicFilename = "txtfile";
    private final String fileExtension = ".txt";

    Button generateFiles, generateRandomFiles, signIn, queryFiles;
    Button uploadFileFromInternalStorage;
    Button basicUploadFromInternalStorage;
    Button basicDownloadToInternalStorage;
    Button basicListFiles, basicListFolder, basicListFilesInFolder;
    Button basicCreateFolder, basicUploadFromInternalStorageToSubfolder;
    com.google.android.material.textfield.TextInputEditText fileName;


    private DriveServiceHelper mDriveServiceHelper;
    private static final int REQUEST_CODE_SIGN_IN = 1;

    Drive googleDriveServiceOwn = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        generateFiles = findViewById(R.id.btnMainGenerateFiles);
        generateRandomFiles = findViewById(R.id.btnMainGenerateRandomFiles);

        signIn = findViewById(R.id.btnMainSignIn);
        queryFiles = findViewById(R.id.btnMainQueryFiles);
        fileName = findViewById(R.id.etMainFilename);

        uploadFileFromInternalStorage = findViewById(R.id.btnMainUploadFile);
        basicUploadFromInternalStorage = findViewById(R.id.btnMainBasicUploadFile);
        basicDownloadToInternalStorage = findViewById(R.id.btnMainBasicDownloadFile);
        basicListFiles = findViewById(R.id.btnMainBasicListFiles);
        basicListFolder = findViewById(R.id.btnMainBasicListFolder);
        basicListFilesInFolder = findViewById(R.id.btnMainBasicListFilesInFolder);
        basicCreateFolder = findViewById(R.id.btnMainBasicCreateFolder);
        basicUploadFromInternalStorageToSubfolder = findViewById(R.id.btnMainBasicUploadFileSubfolder);

        basicCreateFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Basic create a folder in Google Drive");
                if (googleDriveServiceOwn == null) {
                    Log.e(TAG, "please sign in before upload a file");
                    return;
                }
                // https://developers.google.com/drive/api/guides/folder
                // before creating a new folder check if the folder is existing !!!
                // otherwise a second folder with the same name will be created
                Thread DoBasicCreateFolder = new Thread(){
                    public void run(){
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



        basicUploadFromInternalStorageToSubfolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Basic upload from internal storage to subfolder");
                if (googleDriveServiceOwn == null) {
                    Log.e(TAG, "please sign in before upload a file");
                    return;
                }
                // https://developers.google.com/drive/api/guides/manage-uploads
                Thread DoBasicUploadSubfolder = new Thread(){
                    public void run(){
                        Log.i(TAG, "running Thread DoBasicUploadSubfolder");
                        //do something that return "Calling this from your main thread can lead to deadlock"
                        // Upload file photo.jpg on drive.

                        // todo THIS IS JUST A COPY !!!

                        String filename = "txtfile1.txt";
                        String folderName = "test";
                        String folderId = getFolderId(folderName);
                        if (folderId.equals("")) {
                            Log.e(TAG, "The destination folder does not exist, abort: " + filename);
                            return;
                        } else {
                            Log.i(TAG, "The destination folder is existing, start uploading to folderId: " + folderId);
                        }

                        File fileMetadata = new File();
                        //fileMetadata.setName("photo.jpg");
                        fileMetadata.setName(filename);
                        fileMetadata.setParents(Collections.singletonList(folderId));
                        // File's content.
                        java.io.File filePath = new java.io.File(view.getContext().getFilesDir(), filename);
                        if (filePath.exists()) {
                            Log.i(TAG, "filePath " + filename + " is existing");
                        } else {
                            Log.e(TAG, "filePath " + filename + " is NOT existing");
                            return;
                        }
                        // Specify media type and file-path for file.
                        //FileContent mediaContent = new FileContent("image/jpeg", filePath);
                        FileContent mediaContent = new FileContent("text/plain", filePath);
                        try {
                            File file = googleDriveServiceOwn.files().create(fileMetadata, mediaContent)
                                    .setFields("id, parents")
                                    .execute();
                            System.out.println("File ID: " + file.getId());
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

                    }
                };
                DoBasicUploadSubfolder.start();
            }
        });

        basicUploadFromInternalStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Basic upload from internal storage");
                if (googleDriveServiceOwn == null) {
                    Log.e(TAG, "please sign in before upload a file");
                    return;
                }
                // https://developers.google.com/drive/api/guides/manage-uploads
                Thread DoBasicUpload = new Thread(){
                    public void run(){
                        Log.i(TAG, "running Thread DoBasicUpload");
                        //do something that return "Calling this from your main thread can lead to deadlock"
                        // Upload file photo.jpg on drive.
                        String filename = "txtfile1.txt";
                        File fileMetadata = new File();
                        //fileMetadata.setName("photo.jpg");
                        fileMetadata.setName(filename);
                        // File's content.
                        java.io.File filePath = new java.io.File(view.getContext().getFilesDir(), filename);
                        if (filePath.exists()) {
                            Log.i(TAG, "filePath " + filename + " is existing");
                        } else {
                            Log.e(TAG, "filePath " + filename + " is NOT existing");
                            return;
                        }
                        // Specify media type and file-path for file.
                        //FileContent mediaContent = new FileContent("image/jpeg", filePath);
                        FileContent mediaContent = new FileContent("text/plain", filePath);
                        try {
                            File file = googleDriveServiceOwn.files().create(fileMetadata, mediaContent)
                                    .setFields("id")
                                    .execute();
                            System.out.println("File ID: " + file.getId());
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

                    }
                };
                DoBasicUpload.start();
            }
        });

        basicDownloadToInternalStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Basic download to internal storage");
                if (googleDriveServiceOwn == null) {
                    Log.e(TAG, "please sign in before downloading a file");
                    return;
                }
                // https://developers.google.com/drive/api/guides/manage-downloads
                // https://gist.github.com/jesusnoseq/4362854
                // https://stackoverflow.com/questions/58945797/google-drive-api-not-downloading-files-java-v3
                // https://stackoverflow.com/questions/41184940/download-folder-with-google-drive-api

                // first we need to list the files in the folder to get the fileId
                // test11.txt fileId:1rnzjf6Qh7CX8DqOniNpt22rrnxhy-Eys
                // Der Sturm auf die Batterie - Influencer als Lehrer.mp3 fileId:1EK8gUleGtKq9zm08x4gV6jycsapmnNhe

                // todo make a file lister with onClick Lister

                final String filename = "test11.txt";
                final String fileId = "1rnzjf6Qh7CX8DqOniNpt22rrnxhy-Eys";
                Thread DoBasicDownload = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "running Thread DoBasicDownloadFile");

                        java.io.File destFile = new java.io.File(view.getContext().getFilesDir(), filename);
                        OutputStream outputstream = null;
                        try {
                            outputstream = new FileOutputStream(destFile);
                            googleDriveServiceOwn.files().get(fileId)
                                    .executeMediaAndDownloadTo(outputstream);
                            outputstream.flush();
                            outputstream.close();
                            Log.i(TAG, "file download: " + filename);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "file downloaded " + filename + " to Internal Storage", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (IOException e) {
                            Log.e(TAG, "ERROR: " + e.getMessage());
                            //throw new RuntimeException(e);
                        }
                    }
                });
                DoBasicDownload.start();
            }
        });

        basicListFilesInFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Basic list files in folder in Google Drive");
                if (googleDriveServiceOwn == null) {
                    Log.e(TAG, "please sign in before list files");
                    return;
                }
                // folder test: 1-c0_0R0tOomtfuHcpi3Y08PHQXRuMG15
                //String folderId = "1-c0_0R0tOomtfuHcpi3Y08PHQXRuMG15";
                String folderId = "root";

                Thread DoBasicListFilesInFolder = new Thread(){
                    public void run() {
                        Log.i(TAG, "running Thread DoBasicListFilesInFolder");

                        listFilesInFolder(folderId);

                    }
                };

                DoBasicListFilesInFolder.start();
            }
        });

        basicListFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Basic list files in Google Drive");
                if (googleDriveServiceOwn == null) {
                    Log.e(TAG, "please sign in before list files");
                    return;
                }



                // https://developers.google.com/drive/api/guides/search-files
                Thread DoBasicListFiles = new Thread(){
                    public void run(){
                        Log.i(TAG, "running Thread DoBasicListFiles");
                        List<File> files = new ArrayList<File>();
                        String pageToken = null;
                        do {
                            FileList result = null;
                            try {
                                result = googleDriveServiceOwn.files().list()
                                        .setQ("mimeType != 'application/vnd.google-apps.folder'") // list only files
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fileName.setText(sb.toString());
                            }
                        });

                    }
                };
                DoBasicListFiles.start();
            }
        });

        basicListFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Basic list folder in Google Drive");
                if (googleDriveServiceOwn == null) {
                    Log.e(TAG, "please sign in before list folder");
                    return;
                }
                // https://developers.google.com/drive/api/guides/search-files
                Thread DoBasicListFolder = new Thread(){
                    public void run(){
                        Log.i(TAG, "running Thread DoBasicListFolder");
                        List<File> files = new ArrayList<File>();
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
                        } while (pageToken != null);
                        // files is containing all files
                        //return files;
                        Log.i(TAG, "files is containing files or folders: " + files.size());
                        StringBuilder sb = new StringBuilder();
                        sb.append("Folders found in GoogleDrive:\n\n");
                        for (int i = 0; i < files.size(); i++) {
                            String content =
                                    files.get(i).getName() + " " +
                                            files.get(i).getId() + " " +
                                            files.get(i).getSize() + "\n";
                            sb.append(content);
                            sb.append("--------------------\n");
                        }
                        System.out.println("** folder:\n" + sb.toString());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fileName.setText(sb.toString());
                            }
                        });

                    }
                };
                DoBasicListFolder.start();
            }
        });

        queryFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "query files on Google Drive");

                if (mDriveServiceHelper == null) {
                    Log.e(TAG, "ERROR on Querying for files");
                    return;
                }

                    mDriveServiceHelper.queryFiles()
                            .addOnSuccessListener(fileList -> {
                                System.out.println("received filelist");
                                System.out.println("list entries: " + fileList.getFiles().size());
                                StringBuilder builder = new StringBuilder();
                                for (File file : fileList.getFiles()) {
                                    //builder.append(file.getName()).append("\n");
                                    builder.append("fileName: " + file.getName() + " fileId:" + file.getId()).append("\n");
                                    System.out.println("fileName: " + file.getName()
                                            + " fileId: " + file.getId()
                                    + " fileSize: " + file.getSize());

                                }
                                String fileNames = builder.toString();
                                System.out.println(fileNames);
                                fileName.setText(fileNames);
                                //mDocContentEditText.setText(fileNames);

                                //etReadOnlyMode();
                            })
                            .addOnFailureListener(exception -> Log.e(TAG, "Unable to query files.", exception));
            }
        });

        generateFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "generate files");
                // this is generating 5 text files in internal storage
                String basicString = "This is a test file for uploading to Google Drive.\nIt is file number ";

                int numberOfFiles = 5;
                for (int i = 1; i < numberOfFiles + 1; i++) {
                    FileWriter writer = null;
                    try {
                        String filename = basicFilename + i + fileExtension;
                        String dataToWrite = basicString + i + "\n" +
                                "generated on " + new Date();
                        java.io.File file = new java.io.File(view.getContext().getFilesDir(), filename);
                        writer = new FileWriter(file);
                        writer.append(dataToWrite);
                        writer.flush();
                        writer.close();
                        Log.i(TAG, "file generated number: " + i);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Error: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                Toast.makeText(MainActivity.this, "generated " + numberOfFiles + " files in internal storage", Toast.LENGTH_SHORT).show();
            }
        });

        generateRandomFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "generate random files");
                // this is generating 3 text files in internal storage
                String basicString = "This is a test file for uploading to Google Drive.\nIt is file number ";

                int numberOfFiles = 3;
                for (int i = 1; i < numberOfFiles + 1; i++) {
                    FileWriter writer = null;
                    try {
                        SimpleDateFormat df = new SimpleDateFormat("yyMMdd_hhmmss-SSS", Locale.getDefault());
                        String date = df.format(new Date());

                        String filename = basicFilename + "_" + date + fileExtension;
                        String dataToWrite = basicString + i + "\n" +
                                "generated on " + new Date();
                        java.io.File file = new java.io.File(view.getContext().getFilesDir(), filename);
                        writer = new FileWriter(file);
                        writer.append(dataToWrite);
                        writer.flush();
                        writer.close();
                        Log.i(TAG, "file generated number: " + i);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Error: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                Toast.makeText(MainActivity.this, "generated " + numberOfFiles + " files in internal storage", Toast.LENGTH_SHORT).show();
            }
        });

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "sign in to Google Drive");
                // Authenticate the user. For most apps, this should be done when the user performs an
                // action that requires Drive access rather than in onCreate.
                requestSignIn();
            }
        });
    }


    private void listFilesInFolder(String folderId) {
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
     * @param folderName
     * @return true if folder is existing
     */
    private boolean folderExist (String folderName) {
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
        } while (pageToken != null) ;
        return false;
    }

    /**
     * This method returns the folderId on GoogleDrive
     * @param folderName
     * @return tthe folderId, if not existing returns ""
     */
    private String getFolderId (String folderName) {
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
        } while (pageToken != null) ;
        return "";
    }

    private boolean folderExistOld (String folderName) {
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
                        .requestScopes(new Scope(DriveScopes.DRIVE))
                        .build();
/*
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
*/
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

                    // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                    // Its instantiation is required before handling any onClick actions.
                    mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

                    googleDriveServiceOwn = googleDriveService; // todo

                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Unable to sign in.", exception);
                    Toast.makeText(MainActivity.this, "Unable to sign in: " + exception.getMessage(), Toast.LENGTH_LONG).show();
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
/*
            case REQUEST_CODE_OPEN_DOCUMENT:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    Uri uri = resultData.getData();
                    if (uri != null) {
                        openFileFromFilePicker(uri);
                    }
                }
                break;
 */
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }

}