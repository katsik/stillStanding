package com.sleepycookie.stillstanding.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sleepycookie.stillstanding.AnalyzeDataFromAccelerometer;
import com.sleepycookie.stillstanding.Emergency;
import com.sleepycookie.stillstanding.PickContactFragment;
import com.sleepycookie.stillstanding.R;
import com.sleepycookie.stillstanding.data.AppDatabase;
import com.sleepycookie.stillstanding.data.Incident;
import com.sleepycookie.stillstanding.data.Preferences;
import com.sleepycookie.stillstanding.utils.MainHelper;
import com.sleepycookie.stillstanding.utils.PermissionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


//TODO clean & organize this activity
public class MainActivity extends AppCompatActivity
                          implements PickContactFragment.PickContactListener, SensorEventListener {

    FloatingActionButton startDetection;
    ImageButton phoneContactsButton;
    TextView emergencyNumber;
    TextView emergencyContact;
    ImageView emergencyPhoto;
    android.support.v7.widget.CardView contactCard;
    android.support.v7.widget.CardView incidentCard;
    String tempName;
    ProgressBar pvAnalysis;

    // ReadData variables:
    private SensorManager mSensorManager;

    AnalyzeDataFromAccelerometer mService;
    Boolean mBound;

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

    private double[] location = new double[2];
    final static double GRAVITY_ACCELERATION = 9.81;

    static public String currentState = "";

    public Button quitButton;
    public Button triggerButton;
    public Toast mToast;

    private Boolean accelerationBalanced;
    private Boolean stoodUp;
    private Boolean userFell;
    private static Boolean onCall = false;
    private static Boolean active;

    private Context mContext;

    private Long timeOfFall;

    public android.support.v7.widget.CardView warningCard;
    public Button warningOkButton;
    public Button warningEmergencyButton;

    private AppDatabase db;

    private TextView warningText;
    private TextView warningTitle;
    private TextView warningTimer;
    private ImageView warningImage;

    private CountDownTimer timer;
//    private TextView timerTextView;

    public static Boolean fabOn = false;

    Intent serviceIntent;


    //used for receiving messages from fall detection service.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("OnReceive","Hey I got something!");
            Boolean fell = intent.getExtras().getBoolean(getString(R.string.fall_detected_key));
            setUserFell(fell);
            Log.d("onReceive","fell = "+ userFell);
            if(fell){
                long timeOfFall = intent.getExtras()
                    .getLong(getString(R.string.fall_deteciton_time_key));
                setTimeOfFall(timeOfFall);
            }
        }
    };

    /**--------------------------------- Activity Lifecycle Methods -----------------------------**/

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sets preferences to the default values the first time, just to be sure.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Check if we need to display our Intro Activity
        if (!Preferences.getIntroPref(this)) {
            // The user hasn't seen the Intro yet, so show it
            startActivity(new Intent(this, IntroActivity.class));
            mBound = false;
            finish();
        }

        db = AppDatabase.getInstance(this);

        mContext = this;

        emergencyContact = findViewById(R.id.contact_name);
        emergencyNumber = findViewById(R.id.contact_phone);
        emergencyPhoto = findViewById(R.id.contact_image);
        contactCard = findViewById(R.id.card_view);
        phoneContactsButton = findViewById(R.id.set_contact);
        startDetection = findViewById(R.id.start_detection);
        triggerButton = findViewById(R.id.btn_trigger);
        quitButton = findViewById(R.id.btn_quit);

//        timerTextView = findViewById(R.id.tv_emergency_timer);

        pvAnalysis = findViewById(R.id.pb_collecting_data);

        warningCard = findViewById(R.id.warning_card);
        warningEmergencyButton = findViewById(R.id.warning_action_fell);
        warningOkButton = findViewById(R.id.warning_action_ok);
        warningText = findViewById(R.id.warning_text);
        warningTitle = findViewById(R.id.warning_title);
        warningTimer = findViewById(R.id.warning_countdown);
        warningImage = findViewById(R.id.warning_image);

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        initContactButton();
        initFAB();
        initTriggerFunctionality();
        initQuitFunctionality();
        initWarningButtonFunctionality();

        initValues();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setContactCard();
        setIncidentCard();

        // If we just showed the intro, we have asked for the initial permissions
        if (Preferences.getIntroPref(this)) {
            PermissionManager.checkForPermissions(this, this);
        }

        Boolean fell;
        Bundle extras = getIntent().getExtras();
        setActiveStatus(true);

        if(fabOn) mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_UI);

        if(extras!=null && !onCall){
            Log.d("onStart","Hey there!!");
            fell = extras.getBoolean(getString(R.string.fall_detected_key));
            setUserFell(fell);

            setFabStatus(true);

            //mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_UI);

            if(fell){
                long time = extras.getLong(getString(R.string.fall_deteciton_time_key));
                setTimeOfFall(time);
            }
        }
        setOnCall(false);

        serviceIntent = new Intent(this, AnalyzeDataFromAccelerometer.class);

        if(!AnalyzeDataFromAccelerometer.serviceRunning || AnalyzeDataFromAccelerometer.serviceRunning == null){
            setFabStatus(false);

        } else {
            bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
            setFabStatus(true);
        }
    }

    @Override
    protected void onResume(){
        if(!MainActivity.active){
            setActiveStatus(true);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        setActiveStatus(false);
        if(fabOn == true) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onDestroy() {

        if(fabOn){
            unbindService(mConnection);
        }
        mBound = false;
        super.onDestroy();
    }

    /**----------------------------------- UI setup ---------------------------------------------**/

    /**
     * This method is responsible for showing the last incident card in the UI. It puts the last fall's
     * data every time this screen is brought back. It hides the card if the database is empty.
     */
    public void setIncidentCard(){

        final Incident lastIncident = db.incidentDao().loadLastIncident();

        incidentCard = findViewById(R.id.incident_card);

        if (lastIncident != null){
            incidentCard.setVisibility(View.VISIBLE);

            TextView incidentDate = findViewById(R.id.incident_card_date);
            incidentDate.setText(lastIncident.getDateText());

            TextView incidentInfo = findViewById(R.id.incident_card_info);
            incidentInfo.setText(lastIncident.getInfo());

            ImageView incidentImage = findViewById(R.id.incident_image);
            incidentImage.setImageResource(lastIncident.getIcon());

            ImageButton incidentLocationButton = findViewById(R.id.incident_card_location);

            if(lastIncident.hasLocation() == false){
                incidentLocationButton.setVisibility(View.GONE);
            }
            else {
                incidentLocationButton.setVisibility(View.VISIBLE);
                incidentLocationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // The two lines below are needed to open location
                        StringBuffer url = new StringBuffer();
                        url.append( "http://maps.google.com?q=");
                        url.append(String.format ("%.7f", lastIncident.getLatitude()).replaceAll(",", "."));
                        url.append(",");
                        url.append(String.format ("%.7f", lastIncident.getLongitude()).replaceAll(",", "."));
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url.toString()));
                        startActivity(i);
                    }
                });
            }

        }
    }

    /**
     * Shows the saved preferences or the placeholder text (if there is nothing saved) in the contact card.
     * In case there is no saved number the color of the card changes to orange to grab attention.
     */
    private void setContactCard(){

        String mName = Preferences.getContact(this);
        String mNumber = Preferences.getNumber(this);
        String mPhoto = Preferences.getPhoto(this);

        if (mName != null) emergencyContact.setText(mName);
        if (mNumber != null) {
            emergencyNumber.setText(mNumber);
            contactCard.setCardBackgroundColor(getResources().getColor(R.color.white));
            phoneContactsButton.setImageResource(R.drawable.ic_edit_black_24dp);
        }
        else{
            contactCard.setCardBackgroundColor(getResources().getColor(R.color.atterntionColor));
            phoneContactsButton.setImageResource(R.drawable.ic_person_add_black_24dp);
        }
        if (mPhoto != null) {
            emergencyPhoto.setVisibility(View.VISIBLE);
            emergencyPhoto.setImageURI(Uri.parse(mPhoto));
        }
        else {
            emergencyPhoto.setVisibility(View.GONE);
        }
    }

    private void fallWarning(){
        warningTitle.setText(getString(R.string.warning_card_title_fall));
        warningImage.setVisibility(View.INVISIBLE);
        warningTimer.setVisibility(View.VISIBLE);
        warningText.setText(getString(R.string.warning_card_text_seconds_left));
        warningCard.setCardBackgroundColor(getResources().getColor(R.color.warningColor));
        warningCard.setVisibility(View.VISIBLE);
    }

    private void getUpWarning(){
        warningTitle.setText(getString(R.string.warning_card_title));
        warningImage.setVisibility(View.VISIBLE);
        warningTimer.setVisibility(View.GONE);
        warningText.setText(getString(R.string.warning_card_text));
        warningCard.setCardBackgroundColor(getResources().getColor(R.color.atterntionColor));
        warningCard.setVisibility(View.VISIBLE);
    }

    /**------------------------------------- Buttons --------------------------------------------**/

    private void initContactButton(){
        phoneContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // This opens the contact list
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
                    Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    startActivityForResult(contactPickerIntent, 1);
                } else {
                    new Toast(getApplicationContext()).makeText(MainActivity.this, getString(R.string.main_toast_set_permission), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initFAB(){
        if(fabOn){
            startDetection.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_white_24dp));
        }
        else{
            startDetection.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        }

        startDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Preferences.getNumber(MainActivity.this) == null){
                    triggerDialogBox();
                }else{
//                    Intent readData = new Intent(MainActivity.this, ReadDataFromAccelerometer.class);
//                    startActivity(readData);
                    if(fabOn){
                        fabStop();
                    }
                    else{
                        fabStart();
                    }
                }
            }
        });
    }

    private void fabStart(){
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_UI);

        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mMessageReceiver,
                        new IntentFilter("broadcastIntent"));

        setFabStatus(true);
    }

    private void fabStop(){
        mSensorManager.unregisterListener(this);


        if(mBound) { unbindService(mConnection); }
        mBound = false;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        setFabStatus(false);

        initValues();
    }

    private void setFabIcon(Boolean fabOn){
        if(fabOn){
            startDetection.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_white_24dp));
        } else {
            startDetection.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        }
    }

    private void setFabStatus(Boolean input){
        if(input){
            fabOn = true;
            setFabIcon(input);
            pvAnalysis.setVisibility(View.VISIBLE);
        } else {
            fabOn = false;
            setFabIcon(input);
            pvAnalysis.setVisibility(View.GONE);
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
                initValues();
                new Emergency(MainActivity.this, db, mToast).triggerEmergency();
            }
        });
    }

    /**
     *Method used to initialize functionality of Warning card buttons
     **/
    public void initWarningButtonFunctionality(){
        warningEmergencyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initValues();
                new Emergency(MainActivity.this, db, mToast).triggerEmergency();
            }
        });

        warningOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initValues();
            }
        });
    }

    /**------------------------------------- Pick Contact ---------------------------------------**/

    @Override
    public void onPickContactPositive() {
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(contactPickerIntent, 1);
        } else {
            new Toast(getApplicationContext()).makeText(MainActivity.this, getString(R.string.main_toast_set_permission), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPickContactNegative() {
        //TODO not sure if any useful but added it anyway :)
    }

    public void triggerDialogBox(){
        PickContactFragment pickContact = new PickContactFragment();
        pickContact.show(getSupportFragmentManager(),"PickContactFragment");
    }

    /**
     * Method triggered after clicking "Pick a contact". It shows the user the contact list UI,
     * depending on their phone, and they can pick a desired contact. After that there is a dialog
     * that lets them pick one of the phone numbers from that contact, which is then saved for
     * future use. The dialog is shown even if there is only one number stored.
     * This piece of code was found in parts on stackoverflow.
     *
     * @param reqCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (1):
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c = getContentResolver().query(contactData, null, null, null, null);
                    if (c.moveToFirst()) {
                        String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                        String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        String number;

                        Cursor phoneCur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

                        // Gets contact photo URI
                        final Uri photoUri = MainHelper.getPhotoUri(Long.parseLong(id), this);

                        String phoneNumber = "";

                        List<String> allNumbers = new ArrayList<String>();

                        while (phoneCur.moveToNext()) {
                            number = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            int type = phoneCur.getInt(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                            switch (type) {
                                case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                                    Log.v("Home", name + ": " + number);
                                    number = MainHelper.removeClutter(number);
                                    allNumbers.add(number);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                                    Log.v("Mobile", name + ": " + number);
                                    number = MainHelper.removeClutter(number);
                                    allNumbers.add(number);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                                    Log.v("Work", name + ": " + number);
                                    number = MainHelper.removeClutter(number);
                                    allNumbers.add(number);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                                    Log.v("Other", name + ": " + number);
                                    number = MainHelper.removeClutter(number);
                                    allNumbers.add(number);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
                                    Log.v("Custom", name + ": " + number);
                                    number = MainHelper.removeClutter(number);
                                    allNumbers.add(number);
                                    break;
                            }
                        }

                        tempName = name;

                        if (allNumbers.isEmpty()){
                            Toast.makeText(this, getString(R.string.main_toast_no_number), Toast.LENGTH_SHORT).show();
                            phoneCur.close();
                            break;
                        }

                        // Removes duplicate numbers
                        // TODO check if it does that consistently
                        allNumbers = new ArrayList<>(new HashSet<>(allNumbers));

                        final CharSequence[] items = allNumbers.toArray(new String[allNumbers.size()]);
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.choose_number));
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                String selectedNumber = items[item].toString();
                                selectedNumber = selectedNumber.replace("-", "");
                                Log.v("Selected Number:", selectedNumber);

                                Preferences.setNumber(MainActivity.this, selectedNumber);
                                Preferences.setContact(MainActivity.this, tempName);
                                Preferences.setPhoto(MainActivity.this, photoUri);

                                setContactCard();
                            }
                        });
                        AlertDialog alert = builder.create();
                        if (allNumbers.size() > 0) {
                            alert.show();
                        } else {
                            String selectedNumber = phoneNumber;
                            selectedNumber = selectedNumber.replace("-", "");
                            Log.v("Sel:", selectedNumber);
                        }

                        phoneCur.close();
                    }
                    c.close();
                }
                break;
        }
    }

    /**------------------------------------ Accelerometer ---------------------------------------**/

    /**
     * This method initializes the arrays used for keeping track of the states and the acceleration values.
     */
    public void initValues(){
        for(int i=0;i<samples.length;i++){
            samples[i] = 0;
        }
        currentState = "none";
        setUserFell(false);
        setAccelerationBalanced(false);
        setStoodUp(false);
        setTimeOfFall(null);
        for (int j = 0; j< states.length; j++){
            states[j] = currentState;
        }
        warningCard.setVisibility(View.GONE);
        if(timer!=null){
            timer.cancel();
//            timerTextView.setVisibility(View.INVISIBLE);
        }
//        mLabelTextView.setText("Analyzing Data...");
        if(timer != null) {
            timer.cancel();
            timer = null;
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
                    + Math.pow(ay,2)
                    + Math.pow(az,2));

            for (int i =0 ; i<SAMPLES_BUFFER_SIZE-1 ; i++ ){
                //last place of buffer cleared
                samples[i] = samples[i+1];
            }
            samples[SAMPLES_BUFFER_SIZE-1] = svTotalAcceleration;

            if(userFell && getTimeOfFall()!=null){
                fallWarning();
                if(timer==null){
                    Log.d("userFell","timer initialized");
//                    timerTextView.setVisibility(View.VISIBLE);
                    timer = new CountDownTimer(
                            Preferences.getTimeForTriggering(this)*MILLISECONDS_PER_SECOND,
                            MILLISECONDS_PER_SECOND) {
                        @Override
                        public void onTick(long l) {
                            //timerTextView.setText((l/1000)+" seconds remaining until emergency triggered");
                            if(getStoodUp() == false)
                            warningTimer.setText(Long.toString(l/1000));
                        }

                        @Override
                        public void onFinish() {
//                            timerTextView.setVisibility(View.GONE);
                        }
                    };
                    timer.start();
                }
                removeExtrasFromIntent();
                fabOn = true;
                checkPosture(timeOfFall);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //TODO ?
    }

    /**
     * This method checks whether the user stood up or is still down, assuming he/she has already fell.
     *
     * If a fall of user is detected the next thing to monitor will be whether the user will stand up
     * or will remain on the ground. If the user stands up we check the acceleration the device has and we
     * determine weather the user is up, and therefore there is no need for automatic emergency triggering
     * or if he/she cannot stand up and therefore there is an emergency triggering.
     */

    public void checkPosture(long timeSinceFall){
        //wait for 15 seconds (setting the time randomly) to see if user stands up during this time
        Log.d("checkPosture:", "time since fall in ms: "+timeSinceFall);
        long currentTime = System.currentTimeMillis();

        Log.d("checkPosture: ","time passed from fall: "+(currentTime-timeSinceFall));

        if(!getAccelerationBalanced()){
            Log.d("checkPosture","in acceleration balanced if statement");
            // we check for the last measurement to see if it's between the following limits
            accelerationBalanced = (samples[SAMPLES_BUFFER_SIZE-1] >= 9.6 && samples[SAMPLES_BUFFER_SIZE-1] <= 10.0);
            Log.d("checkPosture","accelerationBalanced: " + accelerationBalanced);
        }else{
            //acceleration has balanced between (9.5,10) so now we assume user is lying down
            Log.d("checkPosture","stoodup: "+stoodUp);
            if(!getStoodUp() && (currentTime - timeSinceFall < 5* MILLISECONDS_PER_SECOND)){
                //check to see if he stood up
                stoodUp = (samples[SAMPLES_BUFFER_SIZE-1] <= 0.65 * GRAVITY_ACCELERATION);
                Log.d("CheckPosture", "Not stood up yet.");
            }else if(!getStoodUp() && (currentTime - timeSinceFall >= 15* MILLISECONDS_PER_SECOND)){
                //user didn't stand up since fall and there's been 15 seconds => trigger action
                Log.d("checkPosture","not stood up and gonna trigger emergency");
                initValues();
                new Emergency(this, db, mToast).triggerEmergency();
            }else if(getStoodUp() && (currentTime - timeSinceFall <= 15 * MILLISECONDS_PER_SECOND)){
                getUpWarning();
            }
        }
    }

    /**---------------------------------------- Service -----------------------------------------**/

    /**
     * Method to get extras when activity is in the foreground.
     */
    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            AnalyzeDataFromAccelerometer.AnalyzeDataBinder binder = (AnalyzeDataFromAccelerometer.AnalyzeDataBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.startAccelerometer();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            if(mService!=null){
                mService.stopAccelerometer();
            }
            mBound = false;
        }
    };


    /**---------------------SETTERS/GETTERS---------------------------*/

    private void setUserFell(Boolean fell){this.userFell = fell;}

    private Long getTimeOfFall(){
        return timeOfFall;
    }

    private void setTimeOfFall(Long time){
        timeOfFall = time;
    }

    public void setAccelerationBalanced(Boolean accelerationBalanced) {this.accelerationBalanced = accelerationBalanced;}
    public Boolean getAccelerationBalanced(){return accelerationBalanced;}

    public void setStoodUp(Boolean stoodUp) {this.stoodUp = stoodUp;}
    public Boolean getStoodUp(){return stoodUp;}


    public static void setActiveStatus(Boolean status){active = status;}
    public static Boolean getActiveStatus(){return active;}

    public static void setOnCall(Boolean onCall){MainActivity.onCall = onCall;}
    public static Boolean getOnCall(){return MainActivity.onCall;}

    public static void setUsersState(String setState){
        MainActivity.currentState = setState;
    }

    /**---------------------------------------- Misc --------------------------------------------**/

    /**
     * Used to check for permissions to access contacts or call phone or send SMS
     * @param context
     *
     * @return
     * true if either one of the three permissions is NOT granted
     * false in any other case
     */
    private boolean forbiddenToCallOrReadContacts(Context context){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
            return true;
        }else if(ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            return true;
        }else if(ContextCompat.checkSelfPermission(context,Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
            return true;
        }
        return false;
    }

    private void removeExtrasFromIntent(){
        if(getIntent().getExtras()!=null){
            getIntent().removeExtra(getString(R.string.fall_detected_key));
            getIntent().removeExtra(getString(R.string.fall_deteciton_time_key));
        }
    }

    /**--------------------------------- App Bar Options ----------------------------------------**/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                // User chose the "About" item, show the app "about" UI...
                Intent seeInfo = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(seeInfo);
                return true;

            case R.id.action_history:
                // User chose the "History" item, show the app "history" UI...
                Intent seeHistory = new Intent(MainActivity.this, IncidentHistory.class);
                startActivity(seeHistory);
                return true;

            case R.id.action_settings:
                // User chose the "Settings" item, show the "settings" UI...
                Intent seeSettings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(seeSettings);
                return true;

            case R.id.action_feedback:
                // User chose the "Feedback" item, go to email app...
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:sleepy.cookie.studios@gmail.com")); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_SUBJECT, "[Still Standing] App Feedback");

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;

            case R.id.action_graph:
                // User chose the "Graph" item, show the "graph" UI...
                Intent seeGraph = new Intent(MainActivity.this, GraphActivity.class);
                startActivity(seeGraph);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}