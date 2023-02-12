package de.androidcrypto.googledriveplayground;

import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class ViewUtils {


    public static void showSnackbarGreen(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(ContextCompat.getColor(view.getContext(), R.color.green));
        snackbar.show();
    }

    public static void showSnackbarOrange(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(ContextCompat.getColor(view.getContext(), R.color.orange));
        snackbar.show();
    }

    public static void showSnackbarRed(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(view.getContext(), R.color.red));
        snackbar.show();
    }


}
