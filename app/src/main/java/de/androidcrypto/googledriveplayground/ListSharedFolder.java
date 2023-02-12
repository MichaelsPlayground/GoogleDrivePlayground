package de.androidcrypto.googledriveplayground;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class ListSharedFolder extends AppCompatActivity implements Serializable {

    private final String TAG = "ListSharedFolder";

    Button selectFolder;
    // style="?attr/materialButtonOutlinedStyle"
    // https://github.com/material-components/material-components-android/blob/master/docs/components/Button.md
    //Button listFolder;
    ListView listViewFolder;

    private String[] folderList;
    String selectedFolderForIntent, parentFolderForIntent;

    Intent startMainActivityIntent, startListFolderActivityIntent;
    private String returnToActivityFromIntent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_shared_folder);

        //listFolder = findViewById(R.id.btnListFolderA);
        listViewFolder = findViewById(R.id.lvFolder);
        selectFolder = findViewById(R.id.btnListFolderSelect);

        Bundle extras = getIntent().getExtras();
        System.out.println("get bundles");
        if (extras != null) {
            System.out.println("extras not null");
            String folder = "";
            String parentFolder = "";
            folder = (String) getIntent().getSerializableExtra("selectedFolder");
            parentFolder = (String) getIntent().getSerializableExtra("parentFolder");
            returnToActivityFromIntent = (String) getIntent().getSerializableExtra("returnToActivity");
            if (returnToActivityFromIntent != null) {
                Log.i(TAG, "returnToActivity: " + returnToActivityFromIntent);
            } else {
                Log.i(TAG, "returnToActivity: is NULL");
            }
            if (parentFolder != null) {
                Log.i(TAG, "parent folder: " + parentFolder);
                parentFolderForIntent = parentFolder;
            }

            //if (!folder.equals("")) {
            if (folder != null) {
                Log.i(TAG, "received folder: " + folder);
                selectedFolderForIntent = folder;
                System.out.println("folder not null");
                //folderFromListFolder = folder;
                System.out.println("ListFile folder: " + folder);
                // todo do what has todo when folder is selected
                //listFiles.setVisibility(View.GONE);
                listFolder(getBaseContext(), folder);
                String selectFolderButton = "select folder "
                        + parentFolderForIntent + "/"
                        + selectedFolderForIntent;
                selectFolder.setText(selectFolderButton);
            }
        }

        selectFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "selectFolder");
                Bundle bundle = new Bundle();
                bundle.putString("IntentType", "selectSharedFolder");
                bundle.putString("browsedFolder", selectedFolderForIntent);
                bundle.putString("parentFolder", parentFolderForIntent);
                if (returnToActivityFromIntent.equals("SelectEncryptedFoldersActivity")) {
                    bundle.putString("returnToActivity", returnToActivityFromIntent);
                }
                else {
                    bundle.putString("returnToActivity", "");
                }
                if (returnToActivityFromIntent.equals("SelectEncryptedFoldersActivity")) {
                    startMainActivityIntent = new Intent(ListSharedFolder.this, SelectEncryptedFoldersActivity.class);
                } else {
                    startMainActivityIntent = new Intent(ListSharedFolder.this, MainActivity.class);
                }
                startMainActivityIntent.putExtras(bundle);
                // jumps back
                startActivity(startMainActivityIntent);
                finish();
            }
        });

    }

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
                if (returnToActivityFromIntent.equals("returnToActivity")) {
                    bundle.putString("returnToActivity", returnToActivityFromIntent);
                }
                else {
                    bundle.putString("returnToActivity", "");
                }
                // we are starting a new ListFolder activity
                startListFolderActivityIntent = new Intent(ListSharedFolder.this, ListSharedFolder.class);
                startListFolderActivityIntent.putExtras(bundle);
                startActivity(startListFolderActivityIntent);
                finish();
            }
        });
    }
}
