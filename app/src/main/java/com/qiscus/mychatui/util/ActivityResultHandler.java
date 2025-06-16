package com.qiscus.mychatui.util;

import android.content.Intent;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class ActivityResultHandler {

    private final ActivityResultCaller caller;
    private final IActivityOnResult onResult;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private int requestCode = 0;

    public ActivityResultHandler(ActivityResultCaller caller, IActivityOnResult onResult) {
        this.caller = caller;
        this.onResult = onResult;
    }

    public ActivityResultHandler setRequestCode(int requestCode) {
        this.requestCode = requestCode;
        return this;
    }

    public void registerLauncher() {
        activityResultLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                        if (onResult != null) onResult.handleOnActivityResult(
                                result.getResultCode(), requestCode, result.getData()
                        );
                });
    }

    public void openActivityForResult(Intent intent) {
        if (activityResultLauncher == null) registerLauncher();
        activityResultLauncher.launch(intent);
    }

    public interface IActivityOnResult {
        void handleOnActivityResult(int resultCode, int requestCode, Intent data);
    }
}
