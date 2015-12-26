package com.bleyl.recurrence.ui.activities;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Explode;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bleyl.recurrence.database.DatabaseHelper;
import com.bleyl.recurrence.models.Reminder;
import com.bleyl.recurrence.R;
import com.bleyl.recurrence.receivers.AlarmReceiver;
import com.bleyl.recurrence.receivers.DismissReceiver;
import com.bleyl.recurrence.receivers.SnoozeActionReceiver;
import com.bleyl.recurrence.utils.AlarmUtil;
import com.bleyl.recurrence.utils.DateAndTimeUtil;
import com.bleyl.recurrence.utils.NotificationUtil;

import java.util.Calendar;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ViewActivity extends AppCompatActivity {

    @Bind(R.id.notification_title) TextView mNotificationTitleText;
    @Bind(R.id.notification_time) TextView mNotificationTimeText;
    @Bind(R.id.notification_content) TextView mContentText;
    @Bind(R.id.notification_icon) ImageView mIconImage;
    @Bind(R.id.notification_circle) ImageView mCircleImage;
    @Bind(R.id.time) TextView mTimeText;
    @Bind(R.id.date) TextView mDateText;
    @Bind(R.id.nag) TextView mNagText;
    @Bind(R.id.repeat) TextView mRepeatText;
    @Bind(R.id.shown) TextView mShownText;
    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.detail_layout) LinearLayout mLinearLayout;
    @Bind(R.id.toolbar_shadow) View mShadowView;
    @Bind(R.id.scroll) ScrollView mScrollView;
    @Bind(R.id.header) View mHeaderView;
    @Bind(R.id.view_coordinator) CoordinatorLayout mCoordinatorLayout;
    @Bind(R.id.notification_actions) LinearLayout mNotificationActions;

    private Reminder mReminder;
    private boolean mReminderChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        ButterKnife.bind(this);

        // Setup the shared element transitions for this activity
        setupTransitions();

        setSupportActionBar(mToolbar);
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        // Add drawable shadow and adjust layout if build version is before lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mLinearLayout.setPadding(0, 0, 0, 0);
            mShadowView.setVisibility(View.VISIBLE);
        } else {
            ViewCompat.setElevation(mHeaderView, getResources().getDimension(R.dimen.toolbar_elevation));
        }

        DatabaseHelper database = DatabaseHelper.getInstance(this);
        Intent intent = getIntent();
        int mNotificationID = intent.getIntExtra("NOTIFICATION_ID", 0);

        // Check if notification has been deleted
        if (database.isNotificationPresent(mNotificationID)) {
            mReminder = database.getNotification(mNotificationID);
            database.close();
            if (Boolean.parseBoolean(mReminder.getActiveState())) {
                mNotificationActions.setVisibility(View.VISIBLE);
            }
            assignReminderValues();

        } else {
            database.close();
            // Return home as this notification has been deleted
            returnHome();
        }
    }

    public void assignReminderValues() {
        // Assign notification values to views
        Calendar calendar = DateAndTimeUtil.parseDateAndTime(mReminder.getDateAndTime());
        mNotificationTitleText.setText(mReminder.getTitle());
        mContentText.setText(mReminder.getContent());
        mDateText.setText(DateAndTimeUtil.toStringReadableDate(calendar));
        int iconResId = getResources().getIdentifier(mReminder.getIcon(), "drawable", getPackageName());
        mIconImage.setImageResource(iconResId);
        mCircleImage.setColorFilter(Color.parseColor(mReminder.getColour()));
        String readableTime = DateAndTimeUtil.toStringReadableTime(calendar, this);
        mTimeText.setText(readableTime);
        mNotificationTimeText.setText(readableTime);

        int nagTimer = mReminder.getNagTimer();
        if (nagTimer == 0) {
            mNagText.setText(getResources().getString(R.string.no_nag));
        }
        else {
            mNagText.setText(getResources().getQuantityString(R.plurals.time_minute, nagTimer, nagTimer));
        }

        if (mReminder.getRepeatType() == 5) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getResources().getString(R.string.repeats_on));
            stringBuilder.append(" ");
            String[] shortWeekDays = DateAndTimeUtil.getShortWeekDays();
            for (int i = 0; i < mReminder.getDaysOfWeek().length; i++) {
                if (mReminder.getDaysOfWeek()[i]) {
                    stringBuilder.append(shortWeekDays[i]);
                    stringBuilder.append(" ");
                }
            }
            mRepeatText.setText(stringBuilder);
        } else {
            String[] repeatTexts = getResources().getStringArray(R.array.repeat_array);
            mRepeatText.setText(repeatTexts[mReminder.getRepeatType()]);
        }

        if (Boolean.parseBoolean(mReminder.getForeverState())) {
            mShownText.setText(getResources().getString(R.string.forever));
        } else {
            String shown = (getResources().getString(R.string.times_shown, mReminder.getNumberShown(), mReminder.getNumberToShow()));
            mShownText.setText(shown);
        }

        invalidateOptionsMenu();
    }

    public void setupTransitions() {
        // Add shared element transition animation if on Lollipop or later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Enter transitions
            TransitionSet setEnter = new TransitionSet();

            Transition slideDown = new Explode();
            slideDown.addTarget(mHeaderView);
            slideDown.excludeTarget(mScrollView, true);
            slideDown.setDuration(500);
            setEnter.addTransition(slideDown);

            Transition fadeOut = new Slide(Gravity.BOTTOM);
            fadeOut.addTarget(mScrollView);
            fadeOut.setDuration(500);
            setEnter.addTransition(fadeOut);

            // Exit transitions
            TransitionSet setExit = new TransitionSet();

            Transition slideDown2 = new Explode();
            slideDown2.addTarget(mHeaderView);
            slideDown2.setDuration(570);
            setExit.addTransition(slideDown2);

            Transition fadeOut2 = new Slide(Gravity.BOTTOM);
            fadeOut2.addTarget(mScrollView);
            fadeOut2.setDuration(280);
            setExit.addTransition(fadeOut2);

            getWindow().setEnterTransition(setEnter);
            getWindow().setReturnTransition(setExit);
        }
    }

    public void confirmDelete() {
        new AlertDialog.Builder(this, R.style.Dialog)
                .setMessage(getResources().getString(R.string.delete_confirmation))
                .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        actionDelete();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no), null).show();
    }

    public void actionShowNow() {
        NotificationUtil.createNotification(this, mReminder);
    }

    public void actionDelete() {
        DatabaseHelper database = DatabaseHelper.getInstance(this);
        database.deleteNotification(mReminder);
        database.close();
        Intent alarmIntent = new Intent(getApplicationContext(), AlarmReceiver.class);
        AlarmUtil.cancelAlarm(getApplicationContext(), alarmIntent, mReminder.getId());
        finish();
    }

    public void actionEdit() {
        Intent intent = new Intent(this, CreateEditActivity.class);
        intent.putExtra("NOTIFICATION_ID", mReminder.getId());
        startActivity(intent);
        finish();
    }

    public void actionDismiss(View view) {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, DismissReceiver.class);
        int reminderId = mReminder.getId();
        intent.putExtra("NOTIFICATION_ID", reminderId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminderId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
        finish();
    }

    public void actionSnooze(View view) {
        Context context = getApplicationContext();
        int reminderId = mReminder.getId();

        Intent snoozeIntent = new Intent(context, SnoozeActionReceiver.class);
        snoozeIntent.putExtra("NOTIFICATION_ID", reminderId);
        PendingIntent pendingSnooze = PendingIntent.getBroadcast(context, reminderId, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingSnooze.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
        finish();
    }

    public void returnHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_viewer, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mReminderChanged) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_delete:
                confirmDelete();
                return true;
            case R.id.action_edit:
                actionEdit();
                return true;
            case R.id.action_show_now:
                actionShowNow();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}