package com.oyegbite.workmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.work.Data;
import androidx.work.WorkInfo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.bumptech.glide.Glide;
import com.oyegbite.workmanager.databinding.ActivityBlurBinding;
import com.oyegbite.workmanager.workers.WorkerUtils;

import java.util.List;

public class BlurActivity extends AppCompatActivity {

    private BlurViewModel mViewModel;
    private ActivityBlurBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlurBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get the ViewModel
        mViewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())
        ).get(BlurViewModel.class);

        mViewModel.getOutputWorkInfo().observe(this, new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfoList) {
                // If there are no matching work info, do nothing
                if (workInfoList == null || workInfoList.isEmpty()) return;

                // We only care about the first output status.
                // Every continuation has only one worker tagged TAG_OUTPUT
                WorkInfo workInfo = workInfoList.get(0);

                boolean isFinished = workInfo.getState().isFinished();
                if (isFinished) {
                    showWorkFinished();
                    Data outputData = workInfo.getOutputData();
                    String outputImageUri = outputData.getString(Constants.KEY_IMAGE_URI);
                    if (!TextUtils.isEmpty(outputImageUri)) {
                        mViewModel.setOutputUri(outputImageUri);
                        binding.seeFileButton.setVisibility(View.VISIBLE);
                        WorkerUtils.makeStatusNotification(
                                "Completed.",
                                BlurActivity.this
                        );
                    }
                } else {
                    showWorkInProgress();
                }
            }
        });

        // Image uri should be stored in the ViewModel; put it there then display
        Intent intent = getIntent();
        String imageUriExtra = intent.getStringExtra(Constants.KEY_IMAGE_URI);
        mViewModel.setImageUri(imageUriExtra);

        if (mViewModel.getImageUri() != null) {
            Glide.with(this).load(mViewModel.getImageUri()).into(binding.imageView);
        }

        // Setup blur image file button
        binding.goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewModel.applyBlur(getBlurLevel());
            }
        });

        binding.seeFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri currentUri = mViewModel.getOutputUri();
                if (currentUri != null) {
                    Intent actionView = new Intent(Intent.ACTION_VIEW, currentUri);
                    if (actionView.resolveActivity(getPackageManager()) != null) {
                        startActivity(actionView);
                    }
                }
            }
        });

        binding.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewModel.cancelWork();
                WorkerUtils.makeStatusNotification(
                        "Cancelled.",
                        BlurActivity.this
                );
            }
        });
    }

    /**
     * Get the blur level from the radio button as an integer
     * @return Integer representing the amount of times to blur the image
     */
    private int getBlurLevel() {
        switch (binding.radioBlurGroup.getCheckedRadioButtonId()) {
            case R.id.radio_blur_lv_1:
                return 1;
            case R.id.radio_blur_lv_2:
                return 2;
            case R.id.radio_blur_lv_3:
                return 3;
        }
        return 1;
    }

    /**
     * Shows and hides views for when the Activity is processing an image
     */
    private void showWorkInProgress() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.cancelButton.setVisibility(View.VISIBLE);
        binding.goButton.setVisibility(View.GONE);
        binding.seeFileButton.setVisibility(View.GONE);
    }

    /**
     * Shows and hides views for when the Activity is done processing an image
     */
    private void showWorkFinished() {
        binding.progressBar.setVisibility(View.GONE);
        binding.cancelButton.setVisibility(View.GONE);
        binding.goButton.setVisibility(View.VISIBLE);
    }
}
