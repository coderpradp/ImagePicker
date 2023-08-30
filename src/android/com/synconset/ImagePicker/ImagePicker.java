/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 */
package com.synconset;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;

public class ImagePicker extends CordovaPlugin {

    private static final String ACTION_GET_PICTURES = "getPictures";
    private static final String ACTION_HAS_READ_PERMISSION = "hasReadPermission";
    private static final String ACTION_REQUEST_READ_PERMISSION = "requestReadPermission";

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int ACTION_PERMISSION_REQUEST_CODE = 101;
    private static final int PERMISSION_DENIED_ERROR = 20;

    private int maxImagesCount;
    private int desiredWidth;
    private int desiredHeight;
    private int quality;
    private int outputType;

    private CallbackContext callbackContext;

    public static String[] storge_permissions = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static String[] storge_permissions_33 = {
        Manifest.permission.READ_MEDIA_IMAGES,
    };

    public static String[] permissions() {
        String[] p;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            p = storge_permissions_33;
        } else {
            p = storge_permissions;
        }
        return p;
    }

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (ACTION_HAS_READ_PERMISSION.equals(action)) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, hasReadPermission()));
            return true;

        } else if (ACTION_REQUEST_READ_PERMISSION.equals(action)) {
            requestReadPermission(PERMISSION_REQUEST_CODE);
            return true;

        } else if (ACTION_GET_PICTURES.equals(action)) {
            final JSONObject params = args.getJSONObject(0);

            this.maxImagesCount = 15;
            this.desiredWidth = 0;
            this.desiredHeight = 0;
            this.quality = 100;
            this.outputType = 0;

            if (params.has("maximumImagesCount")) {
                this.maxImagesCount = params.getInt("maximumImagesCount");
            }
            if (params.has("width")) {
                this.desiredWidth = params.getInt("width");
            }
            if (params.has("height")) {
                this.desiredHeight = params.getInt("height");
            }
            if (params.has("quality")) {
                this.quality = params.getInt("quality");
            }
            if (params.has("outputType")) {
                this.outputType = params.getInt("outputType");
            }

            if (cordova != null) {
                if (hasReadPermission()) {
                    openPickerActivity();
                } else {
                    requestReadPermission(ACTION_PERMISSION_REQUEST_CODE);
                }
            }
            return true;
        }
        return false;
    }

    private void openPickerActivity() {
        Intent imagePickerIntent = new Intent(cordova.getActivity(), MultiImageChooserActivity.class);
        imagePickerIntent.putExtra("MAX_IMAGES", this.maxImagesCount);
        imagePickerIntent.putExtra("WIDTH", this.desiredWidth);
        imagePickerIntent.putExtra("HEIGHT", this.desiredHeight);
        imagePickerIntent.putExtra("QUALITY", this.quality);
        imagePickerIntent.putExtra("OUTPUT_TYPE", this.outputType);

        cordova.startActivityForResult(this, imagePickerIntent, 0);
    }

    private boolean hasReadPermission() {
        return cordova.hasPermission(permissions()[0]);
    }

    private void requestReadPermission(int requestCode) {
        cordova.requestPermissions(this, requestCode, permissions());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            int sync = data.getIntExtra("bigdata:synccode", -1);
            final Bundle bigData = ResultIPC.get().getLargeData(sync);

            ArrayList<String> fileNames = bigData.getStringArrayList("MULTIPLEFILENAMES");

            JSONArray res = new JSONArray(fileNames);
            callbackContext.success(res);

        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            String error = data.getStringExtra("ERRORMESSAGE");
            callbackContext.error(error);

        } else if (resultCode == Activity.RESULT_CANCELED) {
            JSONArray res = new JSONArray();
            callbackContext.success(res);

        } else {
            callbackContext.error("No images selected");
        }
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        for (int r: grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                callbackContext.error(PERMISSION_DENIED_ERROR);
                return;
            }
        }

        switch (requestCode) {
            case ACTION_PERMISSION_REQUEST_CODE:
                openPickerActivity();
                break;
            case PERMISSION_REQUEST_CODE:
                callbackContext.success();
                break;
        }
    }

    /**
     * Choosing a picture launches another Activity, so we need to implement the
     * save/restore APIs to handle the case where the CordovaActivity is killed by the OS
     * before we get the launched Activity's result.
     *
     * @see http://cordova.apache.org/docs/en/dev/guide/platforms/android/plugin.html#launching-other-activities
     */
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }
}
