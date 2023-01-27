package de.androidcrypto.googledriveplayground;

import androidx.appcompat.app.AppCompatActivity;

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

public class ListFolder extends AppCompatActivity implements Serializable {
    Button listFolder;
    ListView listViewFolder;

    private String[] folderList;

    Intent startMainActivityIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_folder);

        listFolder = findViewById(R.id.btnListFolderA);
        listViewFolder = findViewById(R.id.lvFolder);

        startMainActivityIntent = new Intent(ListFolder.this, MainActivity.class);

        listFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File internalStorageDir = new File(getFilesDir(), "");
                File[] files = internalStorageDir.listFiles();
                ArrayList<String> folderNames = new ArrayList<>();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        folderNames.add(files[i].getName());
                    }
                }
                folderList = folderNames.toArray(new String[0]);
                System.out.println("fileList size: " + folderList.length);
                ArrayAdapter adapter = new ArrayAdapter<String>(v.getContext(), android.R.layout.simple_list_item_1, folderList);
                listViewFolder.setAdapter(adapter);
                listViewFolder.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String selectedItem = (String) parent.getItemAtPosition(position);
                        System.out.println("The selected folder is : " + selectedItem);
                        Bundle bundle = new Bundle();
                        bundle.putString("selectedFolder", selectedItem);
                        startMainActivityIntent.putExtras(bundle);
                        startActivity(startMainActivityIntent);
                    }
                });
            }
        });
    }
}
