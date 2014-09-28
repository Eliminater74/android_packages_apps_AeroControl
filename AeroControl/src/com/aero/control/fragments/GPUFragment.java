package com.aero.control.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.CustomListPreference;
import com.aero.control.helpers.CustomPreference;
import com.aero.control.helpers.PreferenceHandler;

/**
 * Created by ac on 16.09.13.
 */
public class GPUFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    public boolean checkGpuControl;
    public boolean checkmSweep2wake;
    public boolean checkDoubletap2wake;
    public String[] mColorValues;
    private SharedPreferences prefs;
    private PreferenceCategory PrefCat;
    private PreferenceScreen root;

    private GPUGovernorFragment mGPUGovernorFragment;

    private CustomPreference mGPUControl, mSweep2wake, mDoubletap2Wake, mColorControl;
    private CustomListPreference mGPUControlFrequencies, mGPUGovernor, mDisplayControl;

    public String gpu_file;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // We have to load the xml layout first;
        addPreferencesFromResource(R.layout.gpu_fragment);

        root = this.getPreferenceScreen();
        final PreferenceCategory gpuCategory = (PreferenceCategory) findPreference("gpu_settings");

        // If there are already some entries, kill them all (with fire)
        if (PrefCat != null)
            root.removePreference(PrefCat);

        // Set up gpu control;
        mGPUControl = new CustomPreference(getActivity());
        mGPUControl.setName("gpu_control_enable");
        mGPUControl.setTitle(R.string.pref_gpu_control_enable);
        mGPUControl.setSummary(R.string.pref_gpu_control_enable_sum);
        mGPUControl.setLookUpDefault(AeroActivity.files.GPU_CONTROL_ACTIVE);
        mGPUControl.setOrder(5);
        gpuCategory.addPreference(mGPUControl);

        // Set up sweeptowake;
        mSweep2wake = new CustomPreference(getActivity());
        mSweep2wake.setName("sweeptowake");
        mSweep2wake.setTitle(R.string.pref_sweeptowake);
        mSweep2wake.setSummary(R.string.pref_sweeptowake_sum);
        mSweep2wake.setLookUpDefault(AeroActivity.files.SWEEP2WAKE);
        mSweep2wake.setOrder(10);
        gpuCategory.addPreference(mSweep2wake);

        // Set up doubletaptowake;
        mDoubletap2Wake = new CustomPreference(getActivity());
        mDoubletap2Wake.setName("doubletaptowake");
        mDoubletap2Wake.setTitle(R.string.pref_doubletaptowake);
        mDoubletap2Wake.setSummary(R.string.pref_doubletaptowake_sum);
        mDoubletap2Wake.setLookUpDefault(AeroActivity.files.DOUBLETAP2WAKE);
        mDoubletap2Wake.setOrder(15);
        gpuCategory.addPreference(mDoubletap2Wake);

        mGPUControlFrequencies = new CustomListPreference(getActivity());
        mGPUControlFrequencies.setName("gpu_max_freq");
        mGPUControlFrequencies.setTitle(R.string.pref_gpu_max_freq);
        mGPUControlFrequencies.setDialogTitle(R.string.pref_gpu_max_freq);
        mGPUControlFrequencies.setSummary(R.string.pref_gpu_max_freq_sum);
        mGPUControlFrequencies.setOrder(20);
        gpuCategory.addPreference(mGPUControlFrequencies);


        mGPUGovernor = new CustomListPreference(getActivity());
        mGPUGovernor.setName("set_gpu_governor");
        mGPUGovernor.setTitle("GPU Governor");
        mGPUGovernor.setDialogTitle("GPU Governor");
        mGPUGovernor.setSummary("GPU Governor");
        mGPUGovernor.setOrder(25);
        gpuCategory.addPreference(mGPUGovernor);


        mDisplayControl = new CustomListPreference(getActivity());
        mDisplayControl.setName("display_control");
        mDisplayControl.setTitle(R.string.pref_display_color);
        mDisplayControl.setDialogTitle(R.string.pref_display_color);
        mDisplayControl.setSummary(R.string.pref_display_color_sum);
        mDisplayControl.setOrder(30);
        gpuCategory.addPreference(mDisplayControl);


        mColorControl = (CustomPreference)root.findPreference("rgbValues");
        mColorControl.setOrder(40);
        mColorControl.setLookUpDefault(AeroActivity.files.COLOR_CONTROL);
        mGPUGovernor.setOnPreferenceChangeListener(this);
        mGPUGovernor.setOrder(45);
        mGPUControlFrequencies.setOnPreferenceChangeListener(this);
        mGPUControlFrequencies.setOrder(21);

        /* Find correct gpu path */
        for (String a : AeroActivity.files.GPU_FILES) {
            if (AeroActivity.genHelper.doesExist(a)) {
                gpu_file = a;
                break;
            }
        }

        if(!(AeroActivity.genHelper.doesExist(AeroActivity.files.SWEEP2WAKE)))
            gpuCategory.removePreference(mSweep2wake);

        if(!(AeroActivity.genHelper.doesExist(AeroActivity.files.DOUBLETAP2WAKE)))
            gpuCategory.removePreference(mDoubletap2Wake);

        if(!(AeroActivity.genHelper.doesExist(AeroActivity.files.GPU_CONTROL_ACTIVE)))
                gpuCategory.removePreference(mGPUControl);

        if (gpu_file == null)
            gpuCategory.removePreference(mGPUControlFrequencies);

        if (!(AeroActivity.genHelper.doesExist(AeroActivity.files.COLOR_CONTROL)))
            gpuCategory.removePreference(mColorControl);

        if (AeroActivity.shell.getInfo(AeroActivity.files.DISPLAY_COLOR).equals("Unavailable"))
            gpuCategory.removePreference(mDisplayControl);

        final Preference gpu_gov_settings = root.findPreference("gpu_gov_settings");
        if (AeroActivity.genHelper.doesExist(("/sys/module/msm_kgsl_core/parameters"))) {
            gpu_gov_settings.setOrder(35);

            gpu_gov_settings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    if (mGPUGovernorFragment == null)
                        mGPUGovernorFragment = new GPUGovernorFragment();

                    AeroActivity.mHandler.postDelayed(new Runnable()  {
                        @Override
                        public void run() {
                            getFragmentManager()
                                    .beginTransaction()
                                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                                    .replace(R.id.content_frame, mGPUGovernorFragment)
                                    .addToBackStack("GPU Governor")
                                    .commit();
                            mGPUGovernorFragment.setTitle();
                        }
                    },AeroActivity.genHelper.getDefaultDelay());
                    return true;
                }
            });
        } else {
            gpuCategory.removePreference(gpu_gov_settings);
        }

        // Get our strings;
        CharSequence[] display_entries = {
                getText(R.string.defy_red_colors),
                getText(R.string.defy_green_colors),
                getText(R.string.defy_energy_saver)
        };
        CharSequence[] display_values = {"31", "9", "0"};
        mDisplayControl.setEntries(display_entries);
        mDisplayControl.setEntryValues(display_values);

        // Just throw in our frequencies;
        if (AeroActivity.genHelper.doesExist(AeroActivity.files.GPU_FREQ_NEXUS4_VALUES)) {
            mGPUControlFrequencies.setEntries(AeroActivity.shell.getInfoArray(AeroActivity.files.GPU_FREQ_NEXUS4_VALUES, 1, 0));
            mGPUControlFrequencies.setEntryValues(AeroActivity.shell.getInfoArray(AeroActivity.files.GPU_FREQ_NEXUS4_VALUES, 0, 0));
        } else {
            mGPUControlFrequencies.setEntries(R.array.gpu_frequency_list);
            mGPUControlFrequencies.setEntryValues(R.array.gpu_frequency_list_values);
        }

        if (AeroActivity.genHelper.doesExist(AeroActivity.files.GPU_GOV_BASE + "governor")) {
            mGPUGovernor.setEntries(AeroActivity.shell.getInfoArray(AeroActivity.files.GPU_GOV_BASE + "available_governors", 0, 0));
            mGPUGovernor.setEntryValues(AeroActivity.shell.getInfoArray(AeroActivity.files.GPU_GOV_BASE + "available_governors", 0, 0));
            mGPUGovernor.setValue(AeroActivity.shell.getInfo(AeroActivity.files.GPU_GOV_BASE + "governor"));
            mGPUGovernor.setSummary(AeroActivity.shell.getInfo(AeroActivity.files.GPU_GOV_BASE + "governor"));
            mGPUGovernor.setDialogIcon(R.drawable.gpu_dark);
        } else {
            gpuCategory.removePreference(mGPUGovernor);
        }

        try  {

            if (gpu_file != null) {
                mGPUControlFrequencies.setValue(AeroActivity.shell.getInfoArray(gpu_file, 0, 0)[0]);
                mGPUControlFrequencies.setSummary(AeroActivity.shell.toMHz((AeroActivity.shell.getInfoArray(gpu_file, 0, 0)[0].substring(0,
                        AeroActivity.shell.getInfoArray(gpu_file, 0, 0)[0].length() - 3))));
            }

            // Check if enabled or not;
            if (AeroActivity.shell.getInfo(AeroActivity.files.GPU_CONTROL_ACTIVE).equals("1")) {
                checkGpuControl = true;
                mGPUControl.setSummary(R.string.enabled);
            } else {
                checkGpuControl = false;
                mGPUControl.setSummary(R.string.disabled);
            }

            // Check if enabled or not;
            if (AeroActivity.shell.getInfo(AeroActivity.files.SWEEP2WAKE).equals("1")) {
                checkmSweep2wake = true;
                mSweep2wake.setSummary(R.string.enabled);
            } else {
                checkmSweep2wake = false;
                mSweep2wake.setSummary(R.string.disabled);
            }

            if (AeroActivity.shell.getInfo(AeroActivity.files.DOUBLETAP2WAKE).equals("1")) {
                checkDoubletap2wake = true;
                mDoubletap2Wake.setSummary(R.string.enabled);
            } else {
                checkDoubletap2wake = false;
                mDoubletap2Wake.setSummary(R.string.disabled);
            }

            mSweep2wake.setClicked(checkmSweep2wake);
            mDoubletap2Wake.setClicked(checkDoubletap2wake);
            mGPUControl.setClicked(checkGpuControl);

        } catch (ArrayIndexOutOfBoundsException e) {
            /*
             * If the folder is missing, disable this feature completely;
             */
            mGPUControlFrequencies.setSummary("Unavailable");
            mGPUControlFrequencies.setEnabled(false);
            mGPUControl.setEnabled(false);

            Toast.makeText(getActivity(), "GPU Control is not supported with your kernel.", Toast.LENGTH_LONG).show();
        }
        mGPUControlFrequencies.setDialogIcon(R.drawable.gpu_dark);

        if (!(gpuCategory.getPreferenceCount() > 0)) {
            gpuCategory.setTitle(R.string.nogpu_data);
        }
    }

    private final void showColorControl(final SharedPreferences.Editor editor, final CustomPreference cusPref) {

        mColorValues = AeroActivity.shell.getInfoArray(AeroActivity.files.COLOR_CONTROL, 0, 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.gpu_color_control, null);
        final SeekBar redValues = (SeekBar)layout.findViewById(R.id.redValues);
        final SeekBar greenValues = (SeekBar)layout.findViewById(R.id.greenValues);
        final SeekBar blueValues = (SeekBar)layout.findViewById(R.id.blueValues);

        final EditText redValue = (EditText)layout.findViewById(R.id.redValue);
        final EditText greenValue = (EditText)layout.findViewById(R.id.greenValue);
        final EditText blueValue = (EditText)layout.findViewById(R.id.blueValue);

        redValues.setProgress(Integer.parseInt(mColorValues[0]));
        greenValues.setProgress(Integer.parseInt(mColorValues[1]));
        blueValues.setProgress(Integer.parseInt(mColorValues[2]));

        redValue.setText(mColorValues[0]);
        greenValue.setText(mColorValues[1]);
        blueValue.setText(mColorValues[2]);

        redValues.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                redValue.setText("" + i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        greenValues.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                greenValue.setText("" + i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        blueValues.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                blueValue.setText("" + i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        builder.setTitle(R.string.pref_display_color);

        builder.setView(layout)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        int red = Integer.parseInt(redValue.getText().toString());
                        int green = Integer.parseInt(greenValue.getText().toString());
                        int blue = Integer.parseInt(blueValue.getText().toString());

                        if (red > 255 || blue > 255 || green > 255 ) {
                            Toast.makeText(getActivity(), "The values are out of range!", Toast.LENGTH_LONG).show();
                            return;
                        } else if (red < 10 && blue < 10 && green < 10) {
                            Toast.makeText(getActivity(), "Those values are pretty low, are you sure?", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String rgbValues = redValue.getText() + " " + greenValue.getText() + " " + blueValue.getText();
                        AeroActivity.shell.setRootInfo(rgbValues, AeroActivity.files.COLOR_CONTROL);

                        if (cusPref.isChecked())
                            editor.putString(cusPref.getName(), rgbValues).commit();

                    }
                })
                .setNegativeButton(R.string.maybe_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        builder.show();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        CustomPreference cusPref = null;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPrefs.edit();

        if (preference == mSweep2wake) {

            mSweep2wake.setClicked(!mSweep2wake.isClicked());

            if (mSweep2wake.isClicked())
                AeroActivity.shell.setRootInfo("1", AeroActivity.files.SWEEP2WAKE);
            else
                AeroActivity.shell.setRootInfo("0", AeroActivity.files.SWEEP2WAKE);

            cusPref = (CustomPreference)preference;

        } else if (preference == mDoubletap2Wake) {

            mDoubletap2Wake.setClicked(!mDoubletap2Wake.isClicked());

            if (mDoubletap2Wake.isClicked())
                AeroActivity.shell.setRootInfo("1", AeroActivity.files.DOUBLETAP2WAKE);
            else
                AeroActivity.shell.setRootInfo("0", AeroActivity.files.DOUBLETAP2WAKE);

            cusPref = (CustomPreference)preference;

        } else if (preference == mColorControl) {

            cusPref = (CustomPreference)preference;

            showColorControl(editor, cusPref);

        } else if (preference == mGPUControl) {

            mGPUControl.setClicked(!mGPUControl.isClicked());

            if (mGPUControl.isClicked())
                AeroActivity.shell.setRootInfo("1", AeroActivity.files.GPU_CONTROL_ACTIVE);
            else
                AeroActivity.shell.setRootInfo("0", AeroActivity.files.GPU_CONTROL_ACTIVE);

            cusPref = (CustomPreference)preference;
        }

        // If its checked, we want to save it;
        if (cusPref != null) {
            if (cusPref.isChecked()) {
                if (cusPref.isClicked() != null) {
                    String state = cusPref.isClicked() ? "1" : "0";
                    editor.putString(cusPref.getName(), state).commit();
                }
            }
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        String a = (String) newValue;
        String newSummary = "";
        String path = "";

        if (preference == mGPUControlFrequencies) {

            path = gpu_file;
            newSummary = AeroActivity.shell.toMHz((a.substring(0, a.length() - 3)));

        } else if (preference == mGPUGovernor) {
            // If there are already some entries, kill them all (with fire)
            if (PrefCat != null)
                root.removePreference(PrefCat);

            path = AeroActivity.files.GPU_GOV_BASE + "governor";
            newSummary = a;

        }  else if (preference == mDisplayControl) {

            // Get Permissions first, then execute;
            final String[] commands = new String[]
                    {
                            "chmod 0664 " + AeroActivity.files.DISPLAY_COLOR,
                            "echo " + a + " > " + AeroActivity.files.DISPLAY_COLOR,
                    };
            AeroActivity.shell.setRootInfo(commands);

            Toast.makeText(getActivity(), "Turn your display off/on :)", Toast.LENGTH_LONG).show();

            // Return earlier, since we executed an array;
            return true;
        }

        AeroActivity.shell.setRootInfo(a, path);

        if(!newSummary.equals(""))
            preference.setSummary(newSummary);

        return true;
    }


    // Create our options menu;
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        if (AeroActivity.genHelper.doesExist(AeroActivity.files.GPU_GOV_BASE)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String a = prefs.getString("app_theme", null);

            if (a == null)
                a = "";

            if (a.equals("red"))
                inflater.inflate(R.menu.cpu_menu, menu);
            else if (a.equals("light"))
                inflater.inflate(R.menu.cpu_menu, menu);
            else if (a.equals("dark"))
                inflater.inflate(R.menu.cpu_menu_light, menu);
            else
                inflater.inflate(R.menu.cpu_menu, menu);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_governor_settings:

                String[] completeParamterList;
                try {
                    completeParamterList = AeroActivity.shell.getDirInfo(AeroActivity.files.GPU_GOV_BASE + AeroActivity.shell.getInfo(AeroActivity.files.GPU_GOV_BASE + "governor"), true);
                } catch (NullPointerException e) {
                    Toast.makeText(getActivity(), "Looks like there are no parameter for this governor?", Toast.LENGTH_LONG).show();
                    Log.e("Aero", "Couldn't find any parameters for this governor!", e);
                    return true;
                }

                // If there are already some entries, kill them all (with fire)
                if (PrefCat != null)
                    root.removePreference(PrefCat);

                PrefCat = new PreferenceCategory(getActivity());
                PrefCat.setTitle(R.string.perf_gpu_gov_settings);
                root.addPreference(PrefCat);

                try {

                    PreferenceHandler h = new PreferenceHandler(getActivity(), PrefCat, getPreferenceManager());

                    h.genPrefFromDictionary(completeParamterList, AeroActivity.files.GPU_GOV_BASE + AeroActivity.shell.getInfo(AeroActivity.files.GPU_GOV_BASE + "governor"));

                } catch (NullPointerException e) {
                    Log.e("Aero", "I couldn't get any files!", e);
                }


                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
