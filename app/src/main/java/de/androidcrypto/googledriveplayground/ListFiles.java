package de.androidcrypto.googledriveplayground;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class ListFiles extends AppCompatActivity implements Serializable {

    Button listFiles;
    ListView listViewFiles;

    private String[] fileList;

    Intent startMainActivityIntent;

    String folderFromListFolder = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_files);

        listFiles = findViewById(R.id.btnListFilesA);
        listViewFiles = findViewById(R.id.lvFiles);

        startMainActivityIntent = new Intent(ListFiles.this, MainActivity.class);

        listFiles.setVisibility(View.VISIBLE);
        Bundle extras = getIntent().getExtras();
        System.out.println("get bundles");
        if (extras != null) {
            System.out.println("extras not null");
            String folder = "";
            folder = (String) getIntent().getSerializableExtra("browsedFolder"); //Obtaining data
            //if (!folder.equals("")) {
            if (folder != null) {
                System.out.println("folder not null");
                folderFromListFolder = folder;
                System.out.println("ListFile folder: " + folder);
                // todo do what has todo when folder is selected
                listFiles.setVisibility(View.GONE);
                listFiles(getBaseContext(), folder);
            }
        }

        listFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listFiles(v.getContext(), ""); // means root directory
            }
        });
    }

    private void listFiles(Context context, String startDirectory) {
        File internalStorageDir = new File(getFilesDir(), startDirectory);
        File[] files = internalStorageDir.listFiles();
        ArrayList<String> fileNames = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                fileNames.add(files[i].getName());
            }
        }
        fileList = fileNames.toArray(new String[0]);
        System.out.println("fileList size: " + fileList.length);
        ArrayAdapter adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, fileList);
        listViewFiles.setAdapter(adapter);
        listViewFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
    }
}