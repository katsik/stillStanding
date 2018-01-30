package com.sleepycookie.stillstanding;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class ReadDataFromAccelerometer extends AppCompatActivity implements SensorEventListener,
                                                            GoogleApiClient.ConnectionCallbacks,
                                                            GoogleApiClient.OnConnectionFailedListener{

    private SensorManager mSensorManager;

    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 20;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    public static final int DETECTION_INTERVAL_ASAP = 0;
    static public Context context;
    public TextView mLabelTextView;
    public double ax,ay,az;
    public double svTotalAcceleration;
    static int BUFFER_SIZE = 50;
    static int SAMPLES_BUFFER_SIZE = 10;
    static public double[] samples = new double[SAMPLES_BUFFER_SIZE];
    public String[] states = new String[BUFFER_SIZE];

    final static double GRAVITY_ACCELERATION = 9.81;

    static public String currentState = "";
    double sigma = 0.5,th =10, th1 = 5, th2 = 2;

    public GoogleApiClient mApiClient;
    public ActivityRecognitionClient activityRecognitionClient;
    private PendingIntent pendingIntent;

    public Button quitButton;
    public Button triggerButton;
    public Toast mToast;

    private Context mContext;

    private Long timeOfFall;


    /** Handles playback of all the sound files */
    private MediaPlayer mMediaPlayer;

    /** Handles audio focus when playing a sound file */
    private AudioManager mAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_data_from_accelerometer);
        ReadDataFromAccelerometer.context = getApplicationContext();

        mContext = this;

/*        sensor provides data according to relationship :
                linear acceleration = acceleration - acceleration due to gravity
 */

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        triggerButton = (Button) findViewById(R.id.btn_trigger);
        initTriggerFunctionality();
        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        quitButton = findViewById(R.id.btn_quit);
        initQuitFunctionality();
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_UI);

        mLabelTextView = findViewById(R.id.tv_collecting);
        mLabelTextView.setText("Are You Still Standing?");

        initValues();

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();
    }

    /**
     * This method initializes the arrays used for keeping track of the states and the acceleration values.
     */
    public void initValues(){
        for(int i=0;i<samples.length;i++){
            samples[i] = 0;
        }
        currentState = "none";
        setTimeOfFall(null);
        for (int j = 0; j<states.length; j++){
            states[j] = currentState;
        }
    }

    /**
     * Method triggered every time the accelerometer sensor detects a change and calculates the total acceleration
     * the device has.
     *
     * Maybe we could pass a high-pass filter to make the acceleration values smoother as indicated
     * in https://developer.android.com/reference/android/hardware/SensorEvent.html but definitely not
     * a low-pass filter cause this will filter out the sudden quick movements which literally define a fall.
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float [] events = sensorEvent.values;

            ax = events[0];
            ay = events[1];
            az = events[2];

        // calculate the sum of vector of acceleration
            svTotalAcceleration = Math.sqrt(Math.pow(ax,2)
                    +Math.pow(ay,2)
                    +Math.pow(az,2));

            for (int i =0 ; i<SAMPLES_BUFFER_SIZE-1 ; i++ ){
                //last place of buffer cleared
                samples[i] = samples[i+1];
            }
            samples[SAMPLES_BUFFER_SIZE-1] = svTotalAcceleration;

            if(fallDetected()){
                //update user state table
                currentState = "fell";
                setTimeOfFall(System.currentTimeMillis());
            }
            //the states probably won't be needed since we changed our approach
            //TODO remove the states from code.
            renewStates();
            if(getTimeOfFall()!=null){
                checkPosture(timeOfFall);
            }
//            checkPosture();
        }
    }

    public void renewStates(){
        for(int i = 0; i <= BUFFER_SIZE - 2; i++){
            states[i] = states[i+1];
        }
        states[BUFFER_SIZE-1] = currentState;
//        if (states[BUFFER_SIZE-1]!="fell"){
//            Log.d("Renew States","last element: "+states[BUFFER_SIZE-1]);
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //TODO ?
    }

    /**
     * This method is used to detect a fall of the user.
     *
     * What we do here is the following. We already have a collection of the 10 latest acceleration
     * values. We, then, make a comparison between the newest and the oldest value of the accelerations.
     * If the difference is greater or equal than 2.5 * GRAVITY_ACCELERATION (9.81 [m/s^2]) then we believe
     * this indicates a fall and a true value is returned. In any other case a false value is returned.
     *
     * TL;DR
     * @return true in case a fall was detected false otherwise.
     */
    public boolean fallDetected(){
        //1. compare acceleration amplitude with lower threshold
        //2. if acceleration is less than low_threshold compare if next_acceleration_amplitude > high_threshold
        //3. if true fall detected!

        if(samples[SAMPLES_BUFFER_SIZE-1] - samples[0] >= 2.5 * GRAVITY_ACCELERATION){
//            Log.d("Fall Detection","Fall Detected!");
            showAToast("It seems like you fell");

            return true;
        }

        return false;

    }



    public void checkPosture(long timeSinceFall){
        //wait for 15 seconds (setting the time randomly) to see if user stands up during this time

        long currentTime = System.currentTimeMillis();

        if(currentTime - timeSinceFall >= 15 * MILLISECONDS_PER_SECOND){
            for (double sample : samples){
                if(sample > 1.15 * GRAVITY_ACCELERATION){
                    //user stood up no need to trigger anything
                    initValues();
                    break;
                }else{
                    initValues();
                    triggerEmergency();
                    break;
                }
            }
        }else{
            Log.d("CheckPosture","Not yet");
        }
    }

    /**
     * Use this to prevent multiple Toasts spamming the UI
     *
     * @param message
     */
    public void showAToast(String message){
        if(mToast != null){
            mToast.cancel();
        }
        mToast = Toast.makeText(context,message,Toast.LENGTH_LONG);
        mToast.show();
    }

    /**
     * This method checks weather the user is still, walking, running etc.
     *
     * If a fall of user is detected the next thing to monitor will be whether the user will stand up
     * or will remain on the ground. If the user stands up and starts walking/running this means that
     * either we had a FP fall detection or the user fell and stood up again so no need to trigger an
     * emergency event.
     */

    //TODO cleanup code
    //TODO optimize code
    public void checkPosture(){
        //check from the state array to see if the "fell" state became "still"
        //if so the user fell and is laid down
        //else the user fell and stood up so no need to worry.
        boolean flag = false;
        for (int i=0 ; i< states.length; i++){
            if(!flag){
                if(states[i] == "fell"){
                    flag = true;
                    Log.d("Check Posture","State is " + states[i] + " and flag is " + Boolean.toString(flag));
                }
            }else{
                Log.d("Check Posture","Hey I'm in!");
                Log.d("Check Posture", "State is " + states[i]);
                flag = false;

                if(states[i] == "still"){
                    //turn off the flag
                    //TODO triggerEmergency function which will handle the calling emergContact or the SMS sending
                    triggerEmergency();
                }
            }
        }

    }

    /**
     * This method will be used to trigger the actions needed to be done in case user falls.
     * Probably a phone calling functionality to the emergency contact will be added or even a message
     * to his cell phone or Messenger, WhatsApp, Viber etc.
     *
     * Currently this method places a call to the saved phone.
     */

    //TODO Handle the case where the user has set no emergency contact. Maybe play an alarm, instead of calling null.
    //TODO This is triggered only when I lock the phone (Tsamis).

    public void triggerEmergency(){
//        mLabelTextView.setText("User fell and didn't stand up");
        mLabelTextView.setVisibility(View.INVISIBLE);
        showAToast("User fell and didn't stand up");
        Intent callingIntent = new Intent(Intent.ACTION_CALL);

        SharedPreferences sharedPref = getSharedPreferences("PREF_PHONE",Context.MODE_PRIVATE);
        String mNumber = sharedPref.getString(getString(R.string.emergency_number), null);

        callingIntent.setData(Uri.parse("tel:" + mNumber));
        //initialize state values to prevent multiple calling events.
        initValues();

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE);

        if(permissionCheck==PERMISSION_GRANTED){
            try{
                mContext.startActivity(callingIntent);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else {
            Log.d("Place emergency call", "User didn't give permission");
        }
    }

    public static void setUsersState(String setState){
        ReadDataFromAccelerometer.currentState = setState;
//        Log.d("setUsersState","currentState = " + currentState);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Intent intent = new Intent(context, ActivityRecognizedService.class);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        pendingIntent = PendingIntent.getService(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        activityRecognitionClient = ActivityRecognition.getClient(this);
        activityRecognitionClient.requestActivityUpdates(DETECTION_INTERVAL_ASAP,pendingIntent);

    }

    /**
     * We won't be using this.
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {}

    /**
     * Method called in case the connection to Google Play cannot be achieved.
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("ActivityDetector","Connection to Google Play Services failed.");
    }

    @Override
    protected void onResume(){
        super.onResume();

        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from sensor.
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            //unbind client and stop receiving updates according his/her activity
            activityRecognitionClient.removeActivityUpdates(pendingIntent);
            Log.d("Activity Recognition","ActivityRecognitionClient Removed.");
        }catch (IllegalStateException e){
            //the client probably was not initialized or something, just ignore and exit
            Log.e("Activity Recognition","IllegalStateException caused: "+ e.getMessage());
        }

        if (mApiClient.isConnected()) {
            Intent intent2 = new Intent(this.getApplicationContext(), ActivityRecognizedService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this.getApplicationContext(), 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
            activityRecognitionClient.removeActivityUpdates(pendingIntent);
            mApiClient.disconnect();
        }
    }

    /**
    *Method used to initialize functionality of Quit button
    **/
    public void initQuitFunctionality(){
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAffinity();

            }
        });
    }

    /**
     *Method used to initialize functionality of Trigger button
     **/
    public void initTriggerFunctionality(){
        triggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                triggerEmergency();
            }
        });
    }

    private Long getTimeOfFall(){
        return timeOfFall;
    }

    private void setTimeOfFall(Long time){
        timeOfFall = time;
    }

    //for debugging purposes
    public static void toastingBoom(String msg){
        Log.d("Activity Detected",msg);
    }

    public void siren(){
        // Release the media player if it currently exists because we are about to
        // play a different sound file
        releaseMediaPlayer();

        // Request audio focus so in order to play the audio file. The app needs to play a
        // short audio file, so we will request audio focus with a short amount of time
        // with AUDIOFOCUS_GAIN_TRANSIENT.
        int result = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // We have audio focus now.

            // Create and setup the {@link MediaPlayer} for the audio resource associated
            // with the current word
            mMediaPlayer = MediaPlayer.create(this, R.raw.alarm);

            // Start the audio file
            mMediaPlayer.start();

            // Setup a listener on the media player, so that we can stop and release the
            // media player once the sound has finished playing.
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
        }
    }

    /**
     * Clean up the media player by releasing its resources.
     */
    private void releaseMediaPlayer() {
        // If the media player is not null, then it may be currently playing a sound.
        if (mMediaPlayer != null) {
            // Regardless of the current state of the media player, release its resources
            // because we no longer need it.
            mMediaPlayer.release();

            // Set the media player back to null. For our code, we've decided that
            // setting the media player to null is an easy way to tell that the media player
            // is not configured to play an audio file at the moment.
            mMediaPlayer = null;

            // Regardless of whether or not we were granted audio focus, abandon it. This also
            // unregisters the AudioFocusChangeListener so we don't get anymore callbacks.
            mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        }
    }

    /**
     * This listener gets triggered whenever the audio focus changes
     * (i.e., we gain or lose audio focus because of another app or device).
     */
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // The AUDIOFOCUS_LOSS_TRANSIENT case means that we've lost audio focus for a
                // short amount of time. The AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK case means that
                // our app is allowed to continue playing sound but at a lower volume. We'll treat
                // both cases the same way because our app is playing short sound files.

                // Pause playback and reset player to the start of the file. That way, we can
                // play the word from the beginning when we resume playback.
                mMediaPlayer.pause();
                mMediaPlayer.seekTo(0);
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // The AUDIOFOCUS_GAIN case means we have regained focus and can resume playback.
                mMediaPlayer.start();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // The AUDIOFOCUS_LOSS case means we've lost audio focus and
                // Stop playback and clean up resources
                releaseMediaPlayer();
            }
        }
    };

    /**
     * This listener gets triggered when the {@link MediaPlayer} has completed
     * playing the audio file.
     */
    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            // Now that the sound file has finished playing, release the media player resources.
            releaseMediaPlayer();
        }
    };
}
