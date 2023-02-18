# Google Drive playground

This is an experimental app that shows how to work with Google Drive Rest API 3

Function/Views:

- View: "user is signed in" : red = not signed in, green = signed in
- View: "storage permissions granted": red = permissions not granted, green = permissions granted ( 
status is checked in "check storage permissions")
- grant storage permissions: the app needs the "MANAGE_EXTERNAL_STORAGE" permission that gives full 
access to all files (except the app-data "internal storage" files) on Android 11+. The permission needs 
to be given by the user using switch
- check storage permissions: checks if the permission was granted, if yes it changes the indicator to green
- sign in to Google Drive: to run any operation in a Google Drive account you need to sign in. If an 
account was used before you check in immediately.
- sign out from Google Drive: check out of Google Drive (the account used last)
- check sign status from from Google Drive: if a user is signed in the indicator gets grren, otherwise red
- basic create a folder in Google Drive: creates a hard-coded folder in the Google Drive account
- select local folder: for some operations you need to preselect a folder on your local device. The data 
is stored using shared preferences.
- select Google Drive folder: for some operations you need to preselect a folder on your Google Drive. The data 
is stored using shared preferences.
- select unencrypted content folders: this activity combines the activities "select local folder" and 
"select Google Drive folder". The data is stored using shared preferences.
- select encrypted content folders: this activity stores the selected local and Google Drive folders 
used for an **encrypted** file transfer. The data is stored using shared preferences.
- sync local to Google Drive: the activity compares the content of the preselected local folder with 
the Google Drive folder and uploads the "missing" files to Google Drive. Note: this is not a full 
synchronization functionality - it compares only file names and not timestamps, file lengths or contents of 
files in local and Google Drive folder ! 
- sync Google Drive to local: the activity compares the content of the preselected local folder with 
the Google Drive folder and downloads the "missing" files from Google Drive. Note: this is not a full 
synchronization functionality - it compares only file names and not timestamps, file lengths or contents of 
files in local and Google Drive folder !











build.gradle (app):
````plaintext

    packagingOptions {
        exclude 'META-INF/DEPENDENCIES'
        exclude 'META-INF/LICENSE'
        exclude 'META-INF/LICENSE.txt'
        exclude 'META-INF/license.txt'
        exclude 'META-INF/NOTICE'
        exclude 'META-INF/NOTICE.txt'
        exclude 'META-INF/notice.txt'
        exclude 'META-INF/ASL2.0'
    }


    // google drive libs
    //implementation 'com.google.android.gms:play-services-auth:17.0.0'
    implementation 'com.google.android.gms:play-services-auth:20.4.1'
    //implementation 'com.google.http-client:google-http-client-gson:1.26.0'
    implementation 'com.google.http-client:google-http-client-gson:1.41.8'
    implementation('com.google.api-client:google-api-client-android:1.26.0')
    implementation('com.google.apis:google-api-services-drive:v3-rev136-1.25.0')
    implementation 'com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava'
    
    // for subproject prateekbangre - GoogleDrive_demo
    // for runtime permission
    implementation 'com.karumi:dexter:6.2.1'
    //for top side toast, text with animation
    implementation 'net.steamcrafted:load-toast:1.0.12'    
````

AndroidManifest.xml:
```plaintext
<uses-permission android:name="android.permission.INTERNET" />

    <!-- Permissions required by Google Auth -->
    <uses-permission android:name="android.permission.GET_ACCOUNTS" />
    <uses-permission android:name="android.permission.MANAGE_ACCOUNTS" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>
```

For DriveServiceHelper.java see here:

https://github.com/ammarptn/GDrive-Rest-Android/blob/master/gdriverest/src/main/java/com/ammarptn/gdriverest/DriveServiceHelper.java

https://stackoverflow.com/questions/56949872/can-someone-provide-an-up-to-date-android-guide-for-google-drive-rest-api-v3

https://stackoverflow.com/questions/69933562/android-deprecated-tasks-call-replacement

https://developers.google.com/android/reference/com/google/android/gms/tasks/Tasks

https://developers.google.com/android/reference/com/google/android/gms/tasks/TaskCompletionSource

https://myksb1223.github.io/develop_diary/2019/04/13/Google-Drive-api-in-Android.html

**Subproject ammarptn - GDrive-Rest-Android**

Source https://github.com/ammarptn/GDrive-Rest-Android

**Subproject prateekbangre - GoogleDrive_demo**

Source: https://github.com/prateekbangre/GoogleDrive_demo

**Don't forget to manually given permission to ALL FILES** using SETTINGS/APPS/GoogleDrivePlayground app

A downloaded files is in Emulator: /storage/emulated/0/Download 

The file path is set in prateekbangre/FilesAdapter.java/

private static final String STORAGE_FOLDER_PATH = "/storage/emulated/0/Download";

https://ammar.lanui.online/integrate-google-drive-rest-api-on-android-app-bc4ddbd90820

https://github.com/ammarptn/GDrive-Rest-Android

To access all folders on the device see this permission "MANAGE_EXTERNAL_STORAGE":

https://developer.android.com/training/data-storage/manage-all-files

https://stackoverflow.com/questions/59145619/uploading-all-files-from-local-directory-to-google-drive-google-drive-api-v3

https://stackoverflow.com/questions/42698519/upload-large-files-to-the-google-drive

https://stackoverflow.com/questions/tagged/google-drive-api+android

https://stackoverflow.com/questions/72894657/unable-to-upload-file-in-specific-folder-in-google-drive-using-rest-api

https://github.com/topics/google-drive-api?l=java&o=desc&s=updated

https://github.com/tranbaoanhh27/BAMusic (A music player app for Android, supports uploading and downloading mp3 files from Google Drive.)

https://github.com/rishshah/Pocket-Expense-Manager (Easy and customizable accounting app, old ?)

https://github.com/ProgrammerGnome/Simple-Gaming-Chat-APP (? This is a very simple chat application using google drive api that allows multiple people to chat at the same time without any registration or account creation.)

https://github.com/ErrorxCode/EasyDrive (OLD API ?, EasyDrive is a wrapper for the Google Drive API. It provides convenience methods for accessing the API.)

https://github.com/isaric/drive-uploader (Java pure, 1Y, Uses the example Google Drive api code to create an application that uploads all files from a specified local folder to a specified remote Google Drive folder.)

https://github.com/jug2505/Google-Drive-Saves-Synchronizer (pure Java)

https://github.com/rconfa/android-customers-time-management (Drive + Calendar The goal of the application is to allow the user to keep track of the time spent by a customer to carry out a repair and at the same time, at the end of the intervention, to be able to enter a brief description of the work done and have the customer sign it as a form of acceptance. At the end of the work, once the customer has signed and accepted, the application will insert the image of the customer's signature in Google Drive and an event in Google Calendar)

https://github.com/tejpratap46/Google-Drive-REST-Android

**Subproject tranbaoanhh27 BAMusic**

https://github.com/tranbaoanhh27/BAMusic

A music player app for Android, supports uploading and downloading mp3 files from Google Drive.


**Subproject: https://github.com/prof18/Database-Backup-Restore**

Database Backup & Restore
This is a simple app that shows how to perform backup and restore of a database. You can also upload it to Google Drive.
The database is very simple and you can add students' information and the exams that they have passed. You can also visualize the two tables.
Please be aware tha you need to create to connect the Google Drive API in the Google Cloud Console. More information can be found here
This repo contains the code from the presentation: "How to deal with backup & restore on Android (The code is slighty different from the presentation)

https://stackoverflow.com/questions/68437176/connecting-to-google-drive-api-v3-with-a-service-account-in-android-java-app

https://stackoverflow.com/questions/49709342/java-download-and-delete-file-on-google-drive

Test the access: 
https://developers.google.com/drive/api/v3/reference/files/list#parameters

Code samples: 
https://github.com/googleworkspace/java-samples/tree/main/drive/snippets/drive_v3/src/main/java


https://o7planning.org/11889/manipulating-files-and-folders-on-google-drive-using-java

https://github.com/googlearchive/drive-appdatapreferences-android



# Registering for Google Drive

As we discuss we need to Google Drive SDK, So in order to use it, we need to enable that API. Okay don't worry we will move forward step by step

- Go to [Google Console](https://console.developers.google.com/projectselector/apis/dashboard).
- Sign up for a developer account if We don't have or then sign in.
- `Create` a project Or `Select` and click continue from below,

![Alt Text](https://thepracticaldev.s3.amazonaws.com/i/9tkmgg6z7s5fup0vxfxs.png)

- After creating a project we have `Dashboard` like below,

![Alt Text](https://thepracticaldev.s3.amazonaws.com/i/yd30f54tv84u4zbbqwup.png)


- Now, Select `Library` on the left-hand side to go to the Search screen
- Type “Google Drive” and select Google Drive API.
- Select Enable. Then It looks like below,

![Alt Text](https://thepracticaldev.s3.amazonaws.com/i/3jyyhw1qc16zdii50mye.png)

- Back to Dashboard, from Dashboard left-hand side go to Credentials. Create `Credential` by selecting the `OAuth client ID` like below

![Alt Text](https://thepracticaldev.s3.amazonaws.com/i/985j2fol9qo84nd04gln.png)

Tt navigates to the `Create OAuth client ID` page. Select our Application Type `Android`.

![Alt Text](https://thepracticaldev.s3.amazonaws.com/i/uc5b0l55srkulmptlo6x.png)

- Now we need your `SHA-1` `Signing-certificate fingerprint` key. Do so follow below steps
- Copy the keytool text (press the Copy icon) and paste it into a terminal.
- Change the path-to-debug-or-production-keystore to your default debug keystore location:
- On Mac or Linux, `~/.android/debug.keystore` .
- On Windows, `%USERPROFILE%/.android/debug.keystore` .
- After you execute the command, you will be prompted to enter the keystore password. The password for the debug keystore is blank by default, so you can just press Return or Enter.


> Notes: If throwing error to generate singing key then it might be `keystore` not found.

I used an alternative command for my machine `MAC OS`.

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```


If everything works correctly, you should see something like this:
- Copy the `SHA1` value from the terminal into the text field and press Create. The Client ID dialog will appear. Press OK.

![Alt Text](https://thepracticaldev.s3.amazonaws.com/i/yeb93rnpvwwfspb7u6wc.png)

- Finally, enter a name and the `package name` that we used to `create our app`. Although the hint refers to the package name in `AndroidManifest.xml`, it has to match the `applicationId` in `build.gradle` instead — otherwise, the login flow will fail.
- We don't need to complete the form that appears, it's optional so press `Save` and move on.

> Optional, Now, In the Credentials page. Authorization on Android uses the `SHA1` fingerprint and `package name` to `identify your app`, so you `don’t have to download` any `JSON file or copy any API key` or secret to our project.

It about lots man, Don't worry! Now, are in the fun part 😎.

