package com.sleepycookie.stillstanding.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.sleepycookie.stillstanding.R;
import com.sleepycookie.stillstanding.data.Preferences;
import com.sleepycookie.stillstanding.utils.PermissionManager;

/**
 * Created by geotsam on 15/02/2018.
 * This activity handles the first experience of the user with the app. It shows information
 * about how the app works and about how to use it.
 */

public class IntroActivity extends AppIntro2 {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Hide the ActionBar
        ab.hide();

        // Note here that we DO NOT use setContentView();

        // Set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_welcome),
                getString(R.string.intro_desc_welcome),
                R.drawable.ic_splash_image,
                getResources().getColor(R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_how_to),
                getString(R.string.intro_desc_pocket),
                R.drawable.ic_stillstandingonboardxp_pocket,
                getResources().getColor(R.color.colorPrimaryDark)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_how_to),
                getString(R.string.intro_desc_fall),
                R.drawable.ic_stillstandingonboardxp_fall,
                getResources().getColor(R.color.attentionColor)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_how_to),
                getString(R.string.intro_desc_action),
                R.drawable.ic_stillstandingonboardxp_trigger,
                getResources().getColor(R.color.colorPrimaryDark)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_setup),
                getString(R.string.intro_desc_contact),
                R.drawable.ic_stillstandingonboardxp_contact,
                getResources().getColor(R.color.deep_blue)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_setup),
                getString(R.string.intro_desc_settings),
                R.drawable.ic_stillstandingonboardxp_settings,
                getResources().getColor(R.color.deep_blue)));

        // Hide Skip/Done button.
        showSkipButton(false);
        setProgressButtonEnabled(true);
    }

    @Override
    public void onSkipPressed(androidx.fragment.app.Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.

        Preferences.setIntroPref(IntroActivity.this, true);
        PermissionManager.checkForPermissions(this, this);

        Intent goToMain = new Intent(IntroActivity.this, MainActivity.class);
        startActivity(goToMain);
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable androidx.fragment.app.Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }
}