package com.aero.control.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ListView;
import android.widget.TextView;

import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.adapter.StatisticAdapter;
import com.aero.control.adapter.statisticInit;
import com.aero.control.helpers.FilePath;
import com.echo.holographlibrary.PieGraph;
import com.echo.holographlibrary.PieSlice;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Created by Alexander Christ on 10.01.14.
 * CPU Statistics Fragment
 */
public class StatisticsFragment extends Fragment {
    /*
    TODO: - Take time from other cores in account as well (not critical, governors keep freqs mostly synced)
          - Unify listview and holograph data (?)
     */
    public int mIndex = 0;
    private int mColorIndex = 0;
    public ViewGroup root;
    public String[] data;
    public ListView statisticView;
    public PieGraph pg;
    public TextView txtFreq;
    public TextView txtPercentage;
    public TextView txtTime;
    private double mCompleteTime = 0;
    public ShowcaseView mShowCase;
    public static final String FILENAME_STATISTICS = "firstrun_statistics";
    private final static String NO_DATA_FOUND = "Unavailable";

    public ArrayList<Long> cpuTime = new ArrayList<Long>();
    public ArrayList<Long> cpuOverallTime = new ArrayList<Long>();
    public ArrayList<Long> cpuFreq = new ArrayList<Long>();
    public ArrayList<Long> cpuPercentage = new ArrayList<Long>();
    public ArrayList<Long> cpuResetTime;

    public statisticInit[] mResult = new statisticInit[0];


    // Override for custom view;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        root = (ViewGroup) inflater.inflate(R.layout.statistics, null);

        // Clear UI:
        clearUI();

        loadResetState();

        loadUI(true);

        return root;

    }

    // Create our options menu;
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.statistic_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                clearUI();
                loadUI(true);
                break;
            case R.id.action_reload:
                showResetDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        // Set up our file;
        int output = 0;

        if (AeroActivity.genHelper.doesExist(getActivity().getFilesDir().getAbsolutePath() + "/" + FILENAME_STATISTICS)) {
            output = 1;
        }

        // Only show showcase once;
        if (output == 0)
            DrawFirstStart(R.string.showcase_statistics_fragment, R.string.showcase_statistics_fragment_sum);
    }

    public void DrawFirstStart(int header, int content) {

        try {
            final FileOutputStream fos = getActivity().openFileOutput(FILENAME_STATISTICS, Context.MODE_PRIVATE);
            fos.write("1".getBytes());
            fos.close();
        }
        catch (IOException e) {
            Log.e("Aero", "Could not save file. ", e);
        }

        Target homeTarget = new Target() {
            @Override
            public Point getPoint() {
                // Get approximate position of overflow action icon's center
                int actionBarSize = getActivity().findViewById(R.id.action_refresh).getHeight();
                int x = getResources().getDisplayMetrics().widthPixels - actionBarSize / 2;
                int y = actionBarSize / 2;
                return new Point(x, y);
            }
        };

        mShowCase = new ShowcaseView.Builder(getActivity())
                .setContentTitle(header)
                .setContentText(content)
                .setTarget(homeTarget)
                .build();
    }

    private void showResetDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.about_screen, null);
        TextView aboutText = (TextView) layout.findViewById(R.id.aboutScreen);

        builder.setTitle(R.string.proceed_with_reset);
        builder.setIcon(R.drawable.warning);

        aboutText.setText(R.string.delete_statistics);

        builder.setView(layout)
                .setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Continue with resetting
                        resetStatistics();
                    }
                })
                .setNegativeButton(R.string.maybe_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
        ;

        builder.show();

    }

    private void resetStatistics() {

        // Take the current time;
        Long[] time = cpuOverallTime.toArray(new Long[0]);

        // Add the time to our reset timer;
        if (cpuResetTime != null)
            cpuResetTime = null;
        cpuResetTime = new ArrayList<Long>();

        for (Long t : time) {
            cpuResetTime.add(t);
        }

        Collections.reverse(cpuResetTime);
        Long[] reversedTime = cpuResetTime.toArray(new Long[0]);

        try {
            final FileOutputStream fos = getActivity().openFileOutput("offset_stat", Context.MODE_PRIVATE);

            String a = "";
            for (long f : reversedTime) {
                // Last Time is actually complete Time;
                if (reversedTime.length == 0) {
                    // do nothing;
                } else {
                    a = f + " " + a;
                }
            }
            fos.write(a.getBytes());
            fos.close();
        }
        catch (IOException e) {
            Log.e("Aero", "Could not save file. ", e);
        }

        clearUI();
        loadUI(false);


    }

    public void loadResetState() {

        /*
         * First Case; user resetted statistics, but closed app
         * Second Case; user resetted statistics once, but rebooted
         */

        File a = new File(FilePath.OFFSET_STAT);

        // Handle First case;
        if (a.exists() && cpuResetTime == null) {
            cpuResetTime = new ArrayList<Long>();
            String[] array = AeroActivity.shell.getInfoArray(FilePath.OFFSET_STAT, 0, 0);

            for (String b : array) {
                if (array.length > 1)
                    cpuResetTime.add(Long.parseLong(b));
            }

            //Handle second case here;
            try {
                if (Long.parseLong(array[array.length - 1]) > (SystemClock.elapsedRealtime() / 10)) {
                    a.delete();
                    cpuResetTime = null;
                }
            } catch (NumberFormatException e) {
                a.delete();
                cpuResetTime = null;
            }
        }

    }

    /*
     * Will load all data into different arrays. Some error checks are
     * also calculated here. HoloSlices will be added according to found
     * data.
     */
    private void loadUI(boolean firstView) {

        final ArrayList<String> cpuGraphValues = new ArrayList<String>();
        Long[] cpuFreqArray;
        double a;
        int cpuData = getCpuData();
        mCompleteTime = 0;
        pg = (PieGraph) root.findViewById(R.id.graph);

        // Handle no cpu data found;
        if (cpuData == 0) {
            root.findViewById(R.id.noCpuData).setVisibility(View.VISIBLE);
        } else
            root.findViewById(R.id.noCpuData).setVisibility(View.GONE);

        for (int k = 0; k < cpuData; k++) {
            String b = data[k];
            String[] c = b.split(" ");
            if(k == 0) {
                a = Integer.parseInt(c[0]);
            } else {
                a = Integer.parseInt(c[1]);
            }
            cpuOverallTime.add((long)a);

            mCompleteTime = mCompleteTime + a;
        }
        cpuOverallTime.add((long)mCompleteTime);

        // Handle Uptime here, maybe we don't want to reset it anyway...
        if (cpuResetTime != null) {
            String resetUptime = NO_DATA_FOUND;
            long mResetTime = (long)0;

            if (new File(FilePath.OFFSET_STAT).exists())
                resetUptime = (AeroActivity.shell.getInfoArray(FilePath.OFFSET_STAT, 0, 0))[(AeroActivity.shell.getInfoArray(FilePath.OFFSET_STAT, 0, 0)).length - 1];

            if (!resetUptime.equals(NO_DATA_FOUND))
                mResetTime = Long.parseLong(resetUptime);

            mCompleteTime = mCompleteTime - mResetTime;
        }

        for (int i = 0; i < cpuData; i++) {

            String b = data[i];
            String[] c = b.split(" ");
            Long offsetTime = (long)0;
            File offsetFile = new File(FilePath.OFFSET_STAT);

            if (offsetFile.exists()) {
                try {
                    offsetTime = Long.parseLong(AeroActivity.shell.getInfoArray(FilePath.OFFSET_STAT, 0, 0)[i]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    // The file may be corrupt or didn't save correctly..
                    Log.e("Aero", "The offset file might be smaller as assumed. " + e);
                    offsetFile.delete();
                } catch (NumberFormatException e) {
                    Log.e("Aero", "The offset file might be unavailable. " + e);
                    offsetFile.delete();
                }
            }

            /*
             * Handle deepsleep, if statistics are resetted hook into the calculation process;
             */
            if(i == 0) {
                cpuFreq.add((long)0);
                if (cpuResetTime != null) {
                    cpuTime.add((long) Integer.parseInt(c[0]) - offsetTime);
                } else {
                    cpuTime.add((long)Integer.parseInt(c[0]));
                }
            } else {
                cpuFreq.add((long)Integer.parseInt(c[0]));
                if (cpuResetTime != null) {
                    cpuTime.add((long)Integer.parseInt(c[1]) - offsetTime);
                } else {
                    cpuTime.add((long)Integer.parseInt(c[1]));
                }
            }

        }

        cpuFreqArray = cpuFreq.toArray(new Long[0]);

        int i = 0;
        int j = 0;

        for(long g: cpuTime) {

            PieSlice slice;
            String frequency, time_in_state;
            int percentage;

            // Color change;
            if (j == 8)
                j = 0;

            if(cpuFreqArray[i] == 0)
                frequency = "DeepSleep";
            else
                frequency = AeroActivity.shell.toMHz(cpuFreqArray[i].toString());

            time_in_state = convertTime(g);
            percentage = (int)Math.round((g / mCompleteTime) * 100);
            // Safe all percentages in our array;
            cpuPercentage.add((long) percentage);

            if (g != 0 && percentage >= 1) {

                slice = new PieSlice();
                cpuGraphValues.add(frequency + " " + time_in_state + " " + percentage + "%");

                /*
                 * We are setting the value here to some sort of placeholder
                 * which will be later replaced by the animation handler
                 */
                slice.setValue(10);
                slice.setGoalValue(percentage);
                slice.setColor(Color.parseColor(FilePath.color_code[j]));
                pg.setThickness(30);
                pg.addSlice(slice);

                j++;
            }
            i++;

        }

        // Fill our listview with final values and load TextViews;
        createList(cpuFreq, cpuTime, cpuPercentage);

        if (firstView)
            handleOnClick(cpuGraphValues);

        pg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){

                    handleOnClick(cpuGraphValues);
                    return true;
                }

                return false;
            }
        });
        // Animate the slices here
        pg.setDuration(1000);
        pg.setInterpolator(new AccelerateDecelerateInterpolator());
        pg.animateToGoalValues();

    }

    private void clearUI() {

        /*
         * Cleanup the whole UI.
         * Notice: PieGraph and data might be cleaned anyway,
         * clearing cpuTime/cpuFreq/cpuPercentage AND mResult
         * is _really_ necessary:
         */

        if(pg != null)
            pg.removeSlices();

        if(data != null)
            data = new String[0];

        if(cpuTime != null)
            cpuTime.clear();

        if(cpuOverallTime != null)
            cpuOverallTime.clear();

        if(cpuFreq != null)
            cpuFreq.clear();

        if(cpuPercentage != null)
            cpuPercentage.clear();

        if (statisticView != null)
            mResult = new statisticInit[0];

        mColorIndex = 0;
        mIndex = 0;
    }

    public final void handleOnClick(ArrayList<String> list) {

        final String[] valueArray = list.toArray(new String[0]);

        for (String a: valueArray) {

            int arrayLength = valueArray.length;

            if(mIndex == arrayLength) {
                mIndex = 0;
                mColorIndex = 0;
            }

            /*
             * Fix exception;
             */
            if (mColorIndex >= 8)
                mColorIndex = 0;

            String currentRow = valueArray[mIndex];
            String[] tmp = currentRow.split(" ");

            txtFreq = (TextView)root.findViewById(R.id.statisticFreq);
            txtTime = (TextView)root.findViewById(R.id.statisticTime);
            txtPercentage = (TextView)root.findViewById(R.id.statisticPercentage);

            if (tmp[1].contains("MHz")) {
                tmp[0] = tmp[0] + " MHz";
                tmp[1] = tmp[2];
                tmp[2] = tmp[3];
            }
            txtFreq.setText(tmp[0]);
            txtTime.setText(tmp[1]);
            txtPercentage.setText(tmp[2]);

            txtFreq.setTypeface(FilePath.kitkatFont);
            txtTime.setTypeface(FilePath.kitkatFont);
            txtPercentage.setTypeface(FilePath.kitkatFont);

            txtFreq.setTextColor(Color.parseColor(FilePath.color_code[mColorIndex]));
            txtTime.setTextColor(Color.parseColor(FilePath.color_code[mColorIndex]));
            txtPercentage.setTextColor(Color.parseColor(FilePath.color_code[mColorIndex]));
        }
        mColorIndex++;
        mIndex++;
    }

    /*
     * Convert usertime in human readable values;
     */
    public final String convertTime(long msTime) {

        msTime = msTime * 10;

        return String.format("%02dh:%02dm:%02ds",
                TimeUnit.MILLISECONDS.toHours(msTime),
                TimeUnit.MILLISECONDS.toMinutes(msTime) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(msTime)),
                TimeUnit.MILLISECONDS.toSeconds(msTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(msTime))
        );
    }

    /*
     * Finally creates our list from three array sources
     */

    public final void createList(ArrayList<Long> cpuFreq, ArrayList<Long> cpuTime, ArrayList<Long> cpuPercentage) {

        // Add Complete Uptime;
        cpuFreq.add((long)1);
        cpuTime.add((long)mCompleteTime);
        cpuPercentage.add((long)100);

        // Get Data;
        Long[] freq = cpuFreq.toArray(new Long[0]);
        Long[] time = cpuTime.toArray(new Long[0]);
        Long[] percentage = cpuPercentage.toArray(new Long[0]);

        ArrayDataLoader adl = new ArrayDataLoader();
        adl.loadSingleEntry(freq, time, percentage);

        statisticView = (ListView) root.findViewById(R.id.statisticListView);

        StatisticAdapter adapter = new StatisticAdapter(getActivity(),
                R.layout.statistic_layout, mResult);

        statisticView.setAdapter(adapter);

    }


    public final int getCpuData() {

        if (!(AeroActivity.genHelper.doesExist(FilePath.TIME_IN_STATE_PATH)))
            return 0;

        data = AeroActivity.shell.getInfo(FilePath.TIME_IN_STATE_PATH, true);

        if (data == null)
            return 0;

        return data.length;
    }

    /*
     * Loads our preloaded data into our listview;
     */
    private final class ArrayDataLoader {

        public final void loadSingleEntry(Long[] freq, Long[] time, Long[] percentage) {

            int length = freq.length;

            for(int j = 0; j < length; j++) {

                // Doing the percentage check here again;
                if (percentage[j] != 0 && percentage[j] >= 1) {
                    String convertedFreq = AeroActivity.shell.toMHz(freq[j] + "");

                    // Small UI-Tweak;
                    if(convertedFreq.length() < 8)
                        convertedFreq = convertedFreq + "\t";
                    else if (convertedFreq.length() < 7)
                        convertedFreq = convertedFreq + "\t\t";

                    // Handle Deepsleep
                    if(j == 0)
                        loadArray(mResult, new statisticInit("Deepsleep", convertTime(time[j]) + "", percentage[j] + "%"));
                    else if (j == length - 1)
                        loadArray(mResult, new statisticInit("Uptime   ", convertTime(time[j]) + "", percentage[j] + "%"));
                    else
                        loadArray(mResult, new statisticInit(convertedFreq, convertTime(time[j]) + "", percentage[j] + "%"));
                }
            }

        }

        /*
         * Just a wrapper;
         */
        public final void loadArray (statisticInit[] resultSet, statisticInit data) {

            mResult = fillArray(resultSet, data);
        }

        public final statisticInit[] fillArray (statisticInit[] resultSet, statisticInit data) {

            statisticInit[] result = Arrays.copyOf(resultSet, resultSet.length + 1);
            result[resultSet.length] = data;

            return result;
        }
    }
}