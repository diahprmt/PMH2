package com.example.diah.stepcounterdiah;

import android.graphics.Color;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.diah.stepcounterdiah.logger.Log;
import com.example.diah.stepcounterdiah.logger.LogView;
import com.example.diah.stepcounterdiah.logger.LogWrapper;
import com.example.diah.stepcounterdiah.logger.MessageOnlyLogFilter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResult;
import com.google.android.gms.fitness.result.SessionStopResult;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Diah on 4/28/2018.
 */

public class SpeedActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    //The name used for inserting and reading must match in order to receive results
    private final String SESSION_NAME = "session name";
    private GoogleApiClient mGoogleApiClient;
    private Session mSession;
    public static final String TAG = "StepCounterDiah";

    private Button mStartSession;
    private Button mStopSession;
    private Button mInsertSegment;
    private Button mReadSession;
    private Button mDeleteSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeLogging();
        ButterKnife.bind(this);

        initViews();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.SESSIONS_API)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .build();
        mSession = new Session.Builder()
                .setName(SESSION_NAME)
                .setIdentifier(getString(R.string.app_name) + " " + System.currentTimeMillis())
                .setDescription("Yoga Session Description")
                .setStartTime(Calendar.getInstance().getTimeInMillis(), TimeUnit.MILLISECONDS)
                .setActivity(FitnessActivities.YOGA)
                .build();
    }

    private void initViews() {
        mStartSession = (Button) findViewById(R.id.btn_start_session);
        mStopSession = (Button) findViewById(R.id.btn_stop_session);
        mInsertSegment = (Button) findViewById(R.id.btn_insert_segment);
        mReadSession = (Button) findViewById(R.id.btn_read_session);
        mDeleteSession = (Button) findViewById(R.id.btn_delete_session);

        mStartSession.setOnClickListener(this);
        mStopSession.setOnClickListener(this);
        mInsertSegment.setOnClickListener(this);
        mReadSession.setOnClickListener(this);
        mDeleteSession.setOnClickListener(this);
    }


    public void startSession() {


        PendingResult<Status> pendingResult =
                Fitness.SessionsApi.startSession(mGoogleApiClient, mSession);

        pendingResult.setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Successfully started session");
                        } else {
                            Log.i(TAG, "Failed to start session: " + status.getStatusMessage());
                        }
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }


    @OnClick(R.id.btn_stop_session)
    public void stopSession() {

        PendingResult<SessionStopResult> pendingResult =
                Fitness.SessionsApi.stopSession(mGoogleApiClient, mSession.getIdentifier());

        pendingResult.setResultCallback(new ResultCallback<SessionStopResult>() {
            @Override
            public void onResult(SessionStopResult sessionStopResult) {
                if( sessionStopResult.getStatus().isSuccess() ) {
                    Log.i("Tuts+", "Successfully stopped session");
                    if( sessionStopResult.getSessions() != null && !sessionStopResult.getSessions().isEmpty() ) {
                        Log.i("Tuts+", "Session name: " + sessionStopResult.getSessions().get(0).getName());
                        Log.i("Tuts+", "Session start: " + sessionStopResult.getSessions().get(0).getStartTime(TimeUnit.MILLISECONDS));
                        Log.i("Tuts+", "Session end: " + sessionStopResult.getSessions().get(0).getEndTime(TimeUnit.MILLISECONDS));
                    }
                } else {
                    Log.i("Tuts+", "Failed to stop session: " + sessionStopResult.getStatus().getStatusMessage());
                }
            }

        });
    }

    @OnClick(R.id.btn_insert_segment)
    public void insertSegment() {
        if( !mGoogleApiClient.isConnected() ) {
            Toast.makeText(this, "Not connected to Google", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        Date now = new Date();
        calendar.setTime(now);

        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.MINUTE, -15);
        long walkEndTime = calendar.getTimeInMillis();
        calendar.add(Calendar.MINUTE, -5);
        long walkStartTime = calendar.getTimeInMillis();
        calendar.add(Calendar.MINUTE, -15);
        long startTime = calendar.getTimeInMillis();

        float firstRunSpeed = 15;
        float walkSpeed = 5;
        float secondRunSpeed = 13;

        DataSource speedSegmentDataSource = new DataSource.Builder()
                .setAppPackageName(this.getPackageName())
                .setDataType(DataType.TYPE_SPEED)
                .setName("Tuts+ speed dataset")
                .setType(DataSource.TYPE_RAW)
                .build();

        DataSource activitySegmentDataSource = new DataSource.Builder()
                .setAppPackageName(this.getPackageName())
                .setDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                .setName("Tuts+ activity segments dataset")
                .setType(DataSource.TYPE_RAW)
                .build();

        DataSet speedDataSet = DataSet.create(speedSegmentDataSource);
        DataSet activityDataSet = DataSet.create(activitySegmentDataSource);

        //Create speed data point for first run segment
        DataPoint firstRunSpeedDataPoint = speedDataSet.createDataPoint()
                .setTimeInterval(startTime, walkStartTime, TimeUnit.MILLISECONDS);
        firstRunSpeedDataPoint.getValue(Field.FIELD_SPEED).setFloat(firstRunSpeed);
        speedDataSet.add(firstRunSpeedDataPoint);

        //Create speed data point for walking segment
        DataPoint walkingSpeedDataPoint = speedDataSet.createDataPoint()
                .setTimeInterval(walkStartTime, walkEndTime, TimeUnit.MILLISECONDS);
        walkingSpeedDataPoint.getValue(Field.FIELD_SPEED).setFloat(walkSpeed);
        speedDataSet.add(walkingSpeedDataPoint);

        //Create speed data point for second run segment
        DataPoint secondRunSpeedDataPoint = speedDataSet.createDataPoint()
                .setTimeInterval(walkEndTime, endTime, TimeUnit.MILLISECONDS);
        secondRunSpeedDataPoint.getValue(Field.FIELD_SPEED).setFloat(secondRunSpeed);
        speedDataSet.add(secondRunSpeedDataPoint);

        //Create activity data point for first run segment
        DataPoint firstRunActivityDataPoint = activityDataSet.createDataPoint()
                .setTimeInterval(startTime, walkStartTime, TimeUnit.MILLISECONDS);
        firstRunActivityDataPoint.getValue(Field.FIELD_ACTIVITY).setActivity(FitnessActivities.RUNNING);
        activityDataSet.add(firstRunActivityDataPoint);

        //Create activity data point for walking segment
        DataPoint walkingActivityDataPoint = activityDataSet.createDataPoint()
                .setTimeInterval(walkStartTime, walkEndTime, TimeUnit.MILLISECONDS);
        walkingActivityDataPoint.getValue(Field.FIELD_ACTIVITY).setActivity(FitnessActivities.WALKING);
        activityDataSet.add(walkingActivityDataPoint);

        //Create activity data point for second run segment
        DataPoint secondRunActivityDataPoint = activityDataSet.createDataPoint()
                .setTimeInterval(walkEndTime, endTime, TimeUnit.MILLISECONDS);
        secondRunActivityDataPoint.getValue(Field.FIELD_ACTIVITY).setActivity(FitnessActivities.RUNNING);
        activityDataSet.add(secondRunActivityDataPoint);

        Session session = new Session.Builder()
                .setName(SESSION_NAME)
                .setIdentifier(getString(R.string.app_name) + " " + System.currentTimeMillis())
                .setDescription("Running in Segments")
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setEndTime(endTime, TimeUnit.MILLISECONDS)
                .setActivity(FitnessActivities.RUNNING)
                .build();

        SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                .setSession(session)
                .addDataSet(speedDataSet)
                .addDataSet(activityDataSet)
                .build();

        PendingResult<Status> pendingResult =
                Fitness.SessionsApi.insertSession(mGoogleApiClient, insertRequest);

        pendingResult.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if( status.isSuccess() ) {
                    Log.i("Tuts+", "successfully inserted running session");
                } else {
                    Log.i("Tuts+", "Failed to insert running session: " + status.getStatusMessage());
                }
            }
        });

    }

    @OnClick(R.id.btn_read_session)
    public void readSession() {
        if( !mGoogleApiClient.isConnected() ) {
            Toast.makeText(this, "Not connected to Google", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, -1);
        long startTime = cal.getTimeInMillis();

        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_SPEED)
                .setSessionName(SESSION_NAME)
                .build();

        PendingResult<SessionReadResult> sessionReadResult =
                Fitness.SessionsApi.readSession(mGoogleApiClient, readRequest);

        sessionReadResult.setResultCallback(new ResultCallback<SessionReadResult>() {
            @Override
            public void onResult(SessionReadResult sessionReadResult) {
                if (sessionReadResult.getStatus().isSuccess()) {
                    Log.i(TAG, "Successfully read session data");
                    for (Session session : sessionReadResult.getSessions()) {
                        Log.i(TAG, "Session name: " + session.getName());
                        for (DataSet dataSet : sessionReadResult.getDataSet(session)) {
                            for (DataPoint dataPoint : dataSet.getDataPoints()) {
                                Log.i(TAG, "Speed: " + dataPoint.getValue(Field.FIELD_SPEED));
                            }
                        }
                    }
                } else {
                    Log.i(TAG, "Failed to read session data");
                }
            }
        });
    }

    @OnClick(R.id.btn_delete_session)
    public void deleteSessions() {
        Calendar calendar = Calendar.getInstance();
        Date now = new Date();
        calendar.setTime(now);
        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        long startTime = calendar.getTimeInMillis();

        DataDeleteRequest request = new DataDeleteRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .addDataType(DataType.TYPE_SPEED)
                .deleteAllSessions()
                .build();

        PendingResult<Status> deleteRequest = Fitness.HistoryApi.deleteData(mGoogleApiClient, request);
        deleteRequest.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if( status.isSuccess() ) {
                    Log.i(TAG, "Successfully deleted sessions");
                } else {
                    Log.i(TAG, "Failed to delete sessions");
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btn_start_session: {
                startSession();
                break;
            }
            case R.id.btn_stop_session: {
                stopSession();
                break;
            }
            case R.id.btn_insert_segment: {
                insertSegment();
                break;
            }
            case R.id.btn_read_session: {
                readSession();
                break;
            }
            case R.id.btn_delete_session: {
                deleteSessions();
                break;
            }
        }
    }

    private void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);
        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);
        // On screen logging via a customized TextView.
        LogView logView = (LogView) findViewById(R.id.sample_logview);

        // Fixing this lint errors adds logic without benefit.
        // noinspection AndroidLintDeprecation
        logView.setTextAppearance(R.style.Log);

        logView.setBackgroundColor(Color.WHITE);
        msgFilter.setNext(logView);
        Log.i(TAG, "Ready");
    }
}
