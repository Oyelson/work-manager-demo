package com.oyegbite.workmanager;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.oyegbite.workmanager.workers.BlurWorker;
import com.oyegbite.workmanager.workers.CleanupWorker;
import com.oyegbite.workmanager.workers.SaveImageToFileWorker;

import java.util.List;

public class BlurViewModel extends AndroidViewModel {

    private Uri mImageUri;
    private WorkManager mWorkManager;
    // New instance variable for the WorkInfo class
    private LiveData<List<WorkInfo>> mSavedWorkInfo;
    // New instance variable for the WorkInfo
    private Uri mOutputUri;

    public BlurViewModel(@NonNull Application application) {
        super(application);
        mWorkManager = WorkManager.getInstance(application);
        mSavedWorkInfo = mWorkManager.getWorkInfosByTagLiveData(Constants.TAG_OUTPUT);
    }

    LiveData<List<WorkInfo>> getOutputWorkInfo() {
        return mSavedWorkInfo;
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    void applyBlur(int blurLevel) {
        // Sometimes you only want one chain of work to run at a time. For example,
        // perhaps you have a work chain that syncs your local data with the server
        // - you probably want to let the first data sync finish before starting a new one.
        // To do this, you would use beginUniqueWork instead of beginWith; and you provide a unique
        // String name. This names the entire chain of work requests so that you can refer to and
        //  query them together.
        // Ensure that your chain of work to blur your file is unique by using beginUniqueWork.
        // Pass in IMAGE_MANIPULATION_WORK_NAME as the key.
        // You'll also need to pass in a ExistingWorkPolicy.
        // Your options are REPLACE, KEEP or APPEND.

        // You'll use REPLACE because if the user decides to blur another image before the current
        // one is finished, we want to stop the current one and start blurring the new image.


        // Add WorkRequest to Cleanup temporary images
//        WorkContinuation continuation =
//                mWorkManager.beginWith(OneTimeWorkRequest.from(CleanupWorker.class));
        WorkContinuation continuation = mWorkManager.beginUniqueWork(
                Constants.IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker.class)
        );


        // Add WorkRequests to blur the image the number of times requested
        for (int level = 0; level < blurLevel; level++) {
            OneTimeWorkRequest.Builder blurBuilder = new OneTimeWorkRequest.Builder(BlurWorker.class);

            // Input the Uri if this is the first blur operation
            // After the first blur operation the input will be the output of previous
            // blur operations.
            if (level == 0) {
                blurBuilder.setInputData(createInputDataForUri());
            }

            OneTimeWorkRequest currBlurRequest = blurBuilder.build();
            continuation = continuation.then(currBlurRequest);
        }

        // Create charging and storage not low constraint
        Constraints constraints = new Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .setRequiresCharging(true)
                .build();

        // Add WorkRequest to save the image to the filesystem
        OneTimeWorkRequest save =
                new OneTimeWorkRequest.Builder(SaveImageToFileWorker.class)
                        .setConstraints(constraints)
                        .addTag(Constants.TAG_OUTPUT) // Tag your work
                        .build();
        continuation = continuation.then(save);

        // Actually start the work
        continuation.enqueue();
    }

    void cancelWork() {
        mWorkManager.cancelUniqueWork(Constants.IMAGE_MANIPULATION_WORK_NAME);
    }

    private Uri uriOrNull(String uriString) {
        if (!TextUtils.isEmpty(uriString)){
            return Uri.parse(uriString);
        }
        return null;
    }

    public void setImageUri(String uriString) {
        mImageUri = uriOrNull(uriString);
    }

    public Uri getImageUri() {
        return mImageUri;
    }

    void setOutputUri(String outputImageUri) {
        mOutputUri = uriOrNull(outputImageUri);
    }

    Uri getOutputUri() {
        return mOutputUri;
    }

    /**
     * Creates the input data bundle which includes the Uri to operate on
     * @return Data which contains the Image Uri as a String
     */
    private Data createInputDataForUri() {
        Data.Builder builder = new Data.Builder();
        if (mImageUri != null) {
            builder.putString(Constants.KEY_IMAGE_URI, mImageUri.toString());
        }
        return builder.build();
    }
}
