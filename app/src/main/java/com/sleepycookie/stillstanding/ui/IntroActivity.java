package com.sleepycookie.stillstanding.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.sleepycookie.stillstanding.R;
import com.sleepycookie.stillstanding.data.Preferences;
import com.sleepycookie.stillstanding.utils.PermssionsManager;

/**
 * Created by geotsam on 15/02/2018.
 */

public class IntroActivity extends AppIntro2 {

    public static final String COMPLETED_ONBOARDING_PREF = "onboard_key";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Note here that we DO NOT use setContentView();

        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance("Welcome to Still Standing", "This app might save your life :)", R.drawable.ic_splash_image, getResources().getColor(R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance("How it works", "Have your phone on you. For example, you can put it in your pocket.", R.drawable.ic_stillstandingonboardxp_pocket, getResources().getColor(R.color.colorPrimaryDark)));
        addSlide(AppIntroFragment.newInstance("How it works", "In case you fall the app detects it and it takes action in order to help you.", R.drawable.ic_stillstandingonboardxp_fall, getResources().getColor(R.color.atterntionColor)));
        addSlide(AppIntroFragment.newInstance("How it works", "After it detects a fall, the app calls your emergency contact or notifies them with an SMS, depending on your settings.", R.drawable.ic_stillstandingonboardxp_trigger, getResources().getColor(R.color.colorPrimaryDark)));
        addSlide(AppIntroFragment.newInstance("Set it up", "In the main screen you can pick your emergency contact.", R.drawable.ic_stillstandingonboardxp_contact, getResources().getColor(R.color.deep_blue)));
        addSlide(AppIntroFragment.newInstance("Set it up", "Tap the Settings icon in the main screen to go to your preferences. Select if your contact will be called, or if they will get an SMS. You can also add your location to message.", R.drawable.ic_stillstandingonboardxp_settings, getResources().getColor(R.color.deep_blue)));

        // Hide Skip/Done button.
        showSkipButton(false);
        setProgressButtonEnabled(true);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.

        Preferences.setIntroPref(IntroActivity.this, true);
        PermssionsManager.checkForPermissions(this, this);

        Intent goToMain = new Intent(IntroActivity.this, MainActivity.class);
        startActivity(goToMain);
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }
}