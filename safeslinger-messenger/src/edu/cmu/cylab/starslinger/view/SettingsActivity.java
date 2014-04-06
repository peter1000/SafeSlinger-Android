/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2010-2014 Carnegie Mellon University
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.cmu.cylab.starslinger.view;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.res.Resources.NotFoundException;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.RingtonePreference;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.model.AccountData;
import edu.cmu.cylab.starslinger.model.IntegerListPreference;
import edu.cmu.cylab.starslinger.util.SSUtil;
import edu.cmu.cylab.starslinger.util.StorageOptions;

public class SettingsActivity extends PreferenceActivity {

    public static final int RESULT_NEW_PASSPHRASE = 5;
    private static final int VIEW_FILEDIR_ID = 6;
    private IntegerListPreference mPassPhraseCacheTtl;
    private CheckBoxPreference mRemindBackupDelay;
    private ListPreference mAccountNameType;
    private ListPreference mFileManagerDirectory;
    private EditTextPreference mContactName;
    private Preference mContactPubKeyId;
    private Preference mContactPushToken;
    private Preference mBackupRequestDate;
    private Preference mBackupCompleteDate;
    private Preference mRestoreCompleteDate;
    private Preference mShowLicense;
    private CheckBoxPreference mShowTutorial;
    private Preference mShowAbout;
    private Preference mChangePassphrase;
    private Preference mChangeDownloadDir;
    private CheckBoxPreference mNotificationVibrate;
    private RingtonePreference mNotificationRingtone;
    private ListPreference mFontSize;
    private CheckBoxPreference mAutoDecrypt;
    private ArrayList<AccountData> mAccounts = new ArrayList<AccountData>();
    private String mUnsyncName = null;
    private String mUnsyncType = null;
    private static final String UNSYNC_PKG = "unsynchronized";

    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat);
        super.onCreate(savedInstanceState);

        mUnsyncName = getString(R.string.label_None);
        mUnsyncType = getString(R.string.label_phoneOnly);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setPrefs();

        // TODO: ActionBar actionBar = getActionBar();
        // TODO: actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void setPrefs() {
        addPreferencesFromResource(R.xml.ss_preferences);

        mAccountNameType = (ListPreference) findPreference(SafeSlingerPrefs.pref.TEMPKEY_SYNCACCOUNT_LIST);
        mFileManagerDirectory = (ListPreference) findPreference(SafeSlingerPrefs.pref.FILEMANAGER_ROOTDIR);
        mShowAbout = findPreference(SafeSlingerPrefs.pref.SHOW_ABOUT);
        mShowLicense = findPreference(SafeSlingerPrefs.pref.SHOW_LICENSE);
        mChangePassphrase = findPreference(SafeSlingerPrefs.pref.CHANGE_PASSPHRASE);
        mChangeDownloadDir = findPreference(SafeSlingerPrefs.pref.DOWNLOAD_DIRECTORY);
        mShowTutorial = (CheckBoxPreference) findPreference(SafeSlingerPrefs.pref.SHOW_WALKTHROUGH);
        mRemindBackupDelay = (CheckBoxPreference) findPreference(SafeSlingerPrefs.pref.REMIND_BACKUP_DELAY);
        mPassPhraseCacheTtl = (IntegerListPreference) findPreference(SafeSlingerPrefs.pref.PASSPHRASE_CACHE_TTL);
        mContactName = (EditTextPreference) findPreference(SafeSlingerPrefs.pref.CONTACT_NAME);
        mContactPushToken = findPreference(SafeSlingerPrefs.pref.PUSH_REGISTRATION_ID_DIRECT);
        mContactPubKeyId = findPreference(SafeSlingerPrefs.pref.KEYID_STRING);
        mBackupRequestDate = findPreference(SafeSlingerPrefs.pref.BACKUP_REQUEST_DATE);
        mBackupCompleteDate = findPreference(SafeSlingerPrefs.pref.BACKUP_COMPLETE_DATE);
        mRestoreCompleteDate = findPreference(SafeSlingerPrefs.pref.RESTORE_COMPLETE_DATE);
        mNotificationVibrate = (CheckBoxPreference) findPreference(SafeSlingerPrefs.pref.NOTIFICATION_VIBRATE);
        mNotificationRingtone = (RingtonePreference) findPreference(SafeSlingerPrefs.pref.NOTIFICATION_RINGTONE);
        mFontSize = (ListPreference) findPreference(SafeSlingerPrefs.pref.FONT_SIZE);
        mAutoDecrypt = (CheckBoxPreference) findPreference(SafeSlingerPrefs.pref.AUTO_DECRYPT);

        setMessagePreferences();
    }

    private void restoreDefaultPreferences() {
        SafeSlingerPrefs.restoreUserDefaultSettings();
        setPreferenceScreen(null);
        setPrefs();
    }

    private void setMessagePreferences() {
        setShowAbout();
        setShowLicense();
        setChangePassphrase();
        setFileManagerRootDirectory();
        setChangeDownloadDir();
        setShowTutorial();
        setPassPhraseCacheTtl();
        setRemindBackupDelay();
        setContactPushToken();
        setContactPubKeyId();
        setBackupRequestDate();
        setBackupCompleteDate();
        setRestoreCompleteDate();
        setContactName();
        setContactSyncAccount();
        setRingtone();
        setVibrate();
        setFontSize();
        setAutoDecrypt();
    }

    protected void setContactSyncAccount() {
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        Account[] a = accountManager.getAccounts();

        mAccounts.clear();
        AuthenticatorDescription[] accountTypes = AccountManager.get(this).getAuthenticatorTypes();

        // Also, get a list of all sync adapters and find the ones that
        // support contacts:
        SyncAdapterType[] syncs = ContentResolver.getSyncAdapterTypes();
        ArrayList<String> contactAccountTypes = new ArrayList<String>();
        for (SyncAdapterType sync : syncs) {
            if (ContactsContract.AUTHORITY.equals(sync.authority) && sync.supportsUploading()) {
                contactAccountTypes.add(sync.accountType);
            }
        }

        // Populate tables
        for (int i = 0; i < a.length; i++) {
            // The user may have multiple accounts with the same name, so we
            // need to construct a
            // meaningful display name for each.
            String systemAccountType = a[i].type;
            AuthenticatorDescription ad = getAuthenticatorDescription(systemAccountType,
                    accountTypes);
            if (ad != null) {
                AccountData data = new AccountData(this, a[i].name, ad);

                // filter on accounts that support contacts
                if (contactAccountTypes.contains(a[i].type))
                    mAccounts.add(data);
            }
        }

        // unsync account
        AuthenticatorDescription adNull = new AuthenticatorDescription(mUnsyncType, UNSYNC_PKG, 0,
                0, 0, 0);
        AccountData aNull = new AccountData(this, mUnsyncName, adNull);
        mAccounts.add(aNull);

        String prefAccountName = SafeSlingerPrefs.getAccountName();
        String prefAccountType = SafeSlingerPrefs.getAccountType();

        final CharSequence[] entries = new CharSequence[mAccounts.size()];
        CharSequence[] entryValues = new CharSequence[mAccounts.size()];

        int index = 0;
        int i = 0;
        for (AccountData acct : mAccounts) {
            entryValues[i] = String.valueOf(i);
            if (acct.getName().equals(mUnsyncName)) {
                entries[i] = mUnsyncName + "\n" + mUnsyncType;
            } else {
                entries[i] = acct.getName() + "\n" + acct.getType();
            }

            if (acct.getName().equals(prefAccountName) && acct.getType().equals(prefAccountType)) {
                index = i;
            } else if (acct.getName().equals(mUnsyncName) && prefAccountName == null) {
                index = i;
            }
            i++;
        }

        mAccountNameType.setEntries(entries);
        mAccountNameType.setSummary(prefAccountName);
        mAccountNameType.setEntryValues(entryValues);
        if (mAccounts.size() < index) {
            mAccountNameType.setValueIndex(index);
        }
        mAccountNameType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int newIndex = Integer.valueOf((String) newValue);
                mAccountNameType.setSummary(mAccounts.get(newIndex).getName());
                mAccountNameType.setValueIndex(newIndex);

                // save selected
                if (mAccounts.get(newIndex).getName().contentEquals(mUnsyncName)) {
                    SafeSlingerPrefs.setAccountName(null);
                    SafeSlingerPrefs.setAccountType(null);
                } else {
                    SafeSlingerPrefs.setAccountName(mAccounts.get(newIndex).getName());
                    SafeSlingerPrefs.setAccountType(mAccounts.get(newIndex).getType());
                }
                return false;
            }
        });
    }

    protected void setFileManagerRootDirectory() {

        StorageOptions.determineStorageOptions(this);
        final int count = StorageOptions.count;
        final String[] labels = StorageOptions.labels;
        final String[] paths = StorageOptions.paths;
        final CharSequence[] entries = new CharSequence[count];
        final CharSequence[] entryValues = new CharSequence[count];
        final String selection = SafeSlingerPrefs.getFileManagerRootDir();

        int index = 0;
        int i = 0;
        for (String path : paths) {
            entryValues[i] = String.valueOf(i);
            entries[i] = labels[i] + "\n" + path;
            if (path.equalsIgnoreCase(selection)) {
                index = i;
            }
            i++;
        }

        mFileManagerDirectory.setEntries(entries);
        mFileManagerDirectory.setSummary(selection);
        mFileManagerDirectory.setEntryValues(entryValues);
        if (count < index) {
            mAccountNameType.setValueIndex(index);
        }
        mFileManagerDirectory
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        int newIndex = Integer.valueOf((String) newValue);
                        mFileManagerDirectory.setSummary(paths[newIndex]);
                        mFileManagerDirectory.setValueIndex(newIndex);

                        // save selected
                        SafeSlingerPrefs.setFileManagerRootDir(paths[newIndex]);
                        return false;
                    }
                });
    }

    protected void setShowAbout() {
        mShowAbout.setSummary(SafeSlingerConfig.getVersionName());
        mShowAbout.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!isFinishing()) {
                    removeDialog(BaseActivity.DIALOG_ABOUT);
                    showDialog(BaseActivity.DIALOG_ABOUT);
                }
                return false;
            }
        });
    }

    protected void setShowLicense() {
        mShowLicense.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!isFinishing()) {
                    removeDialog(BaseActivity.DIALOG_LICENSE);
                    showDialog(BaseActivity.DIALOG_LICENSE);
                }
                return false;
            }
        });
    }

    protected void setChangePassphrase() {
        mChangePassphrase.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                setResult(RESULT_NEW_PASSPHRASE);
                finish();
                return true;
            }
        });
    }

    protected void setChangeDownloadDir() {
        mChangeDownloadDir.setSummary(SafeSlingerPrefs.getDownloadDir());
        mChangeDownloadDir.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                showFileDir();
                return true;
            }
        });
    }

    private void showFileDir() {
        if (SSUtil.isExternalStorageWritable()) {
            Intent intent = new Intent(SettingsActivity.this, FileSaveActivity.class);
            startActivityForResult(intent, VIEW_FILEDIR_ID);
        } else {
            showNote(R.string.error_FileStorageUnavailable);
        }
    }

    protected void setShowTutorial() {
        mShowTutorial.setChecked(SafeSlingerPrefs.getShowWalkthrough());
        mShowTutorial.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mShowTutorial.setChecked((Boolean) newValue);
                SafeSlingerPrefs.setShowWalkthrough((Boolean) newValue);
                return false;
            }
        });
    }

    protected void setRemindBackupDelay() {
        mRemindBackupDelay.setChecked(SafeSlingerPrefs.getRemindBackupDelay());
        mRemindBackupDelay
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mRemindBackupDelay.setChecked((Boolean) newValue);
                        SafeSlingerPrefs.setRemindBackupDelay((Boolean) newValue);
                        return false;
                    }
                });
    }

    protected void setPassPhraseCacheTtl() {
        mPassPhraseCacheTtl.setValue("" + SafeSlingerPrefs.getPassPhraseCacheTtl());
        mPassPhraseCacheTtl.setSummary(mPassPhraseCacheTtl.getEntry());
        mPassPhraseCacheTtl
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mPassPhraseCacheTtl.setValue(newValue.toString());
                        mPassPhraseCacheTtl.setSummary(mPassPhraseCacheTtl.getEntry());
                        SafeSlingerPrefs.setPassPhraseCacheTtl(Integer.parseInt(newValue.toString()));
                        SafeSlinger.startCacheService(SettingsActivity.this);
                        return false;
                    }
                });
    }

    protected void setContactName() {
        mContactName.setText(SafeSlingerPrefs.getContactName());
        mContactName.setSummary(SafeSlingerPrefs.getContactName());
        mContactName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mContactName.setText((String) newValue);
                mContactName.setSummary((String) newValue);
                SafeSlingerPrefs.setContactName((String) newValue);
                return false;
            }
        });
    }

    protected void setContactPushToken() {
        mContactPushToken.setSummary(SafeSlingerPrefs.getPushRegistrationId());
    }

    protected void setContactPubKeyId() {
        mContactPubKeyId.setSummary(SafeSlingerPrefs.getKeyIdString());
    }

    protected void setBackupRequestDate() {
        long date = SafeSlingerPrefs.getBackupRequestDate();
        String str = date > 0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                Locale.getDefault()).format(new Date(date)) : getString(R.string.label_None);
        mBackupRequestDate.setSummary(str);
    }

    protected void setBackupCompleteDate() {
        long date = SafeSlingerPrefs.getBackupCompleteDate();
        String str = date > 0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                Locale.getDefault()).format(new Date(date)) : getString(R.string.label_None);
        mBackupCompleteDate.setSummary(str);
    }

    protected void setRestoreCompleteDate() {
        long date = SafeSlingerPrefs.getRestoreCompleteDate();
        String str = date > 0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                Locale.getDefault()).format(new Date(date)) : getString(R.string.label_None);
        mRestoreCompleteDate.setSummary(str);
    }

    private void setRingtone() {
        mNotificationRingtone
                .setSummary(getRingSummary(SafeSlingerPrefs.getNotificationRingTone()));
        mNotificationRingtone
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        SafeSlingerPrefs.setNotificationRingTone((String) newValue);
                        mNotificationRingtone.setSummary(getRingSummary((String) newValue));
                        return true;
                    }
                });
    }

    private String getRingSummary(String soundValue) throws NotFoundException {
        Uri soundUri = TextUtils.isEmpty(soundValue) ? null : Uri.parse(soundValue);
        Ringtone tone = soundUri != null ? RingtoneManager.getRingtone(this, soundUri) : null;
        return (tone != null ? tone.getTitle(this) : getResources().getString(
                R.string.msgr_silent_ringtone));
    }

    protected void setVibrate() {
        mNotificationVibrate.setChecked(SafeSlingerPrefs.getNotificationVibrate());
        mNotificationVibrate
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mNotificationVibrate.setChecked((Boolean) newValue);
                        SafeSlingerPrefs.setNotificationVibrate((Boolean) newValue);
                        return false;
                    }
                });
    }

    protected void setFontSize() {
        mFontSize.setSummary(mFontSize.getEntry());
        mFontSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mFontSize.setValue(newValue.toString());
                mFontSize.setSummary(mFontSize.getEntry());
                SafeSlingerPrefs.setFontSize(Integer.parseInt(newValue.toString()));
                return false;
            }
        });
    }

    protected void setAutoDecrypt() {
        mAutoDecrypt.setChecked(SafeSlingerPrefs.getAutoDecrypt());
        mAutoDecrypt.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mAutoDecrypt.setChecked((Boolean) newValue);
                SafeSlingerPrefs.setAutoDecrypt((Boolean) newValue);
                return false;
            }
        });
    }

    private static AuthenticatorDescription getAuthenticatorDescription(String type,
            AuthenticatorDescription[] dictionary) {
        for (int i = 0; i < dictionary.length; i++) {
            if (dictionary[i].type.equals(type)) {
                return dictionary[i];
            }
        }
        // No match found
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case VIEW_FILEDIR_ID:
                switch (resultCode) {
                    case RESULT_OK:
                        String chosenPath = data.getStringExtra(extra.FPATH);
                        SafeSlingerPrefs.setDownloadDir(chosenPath);
                        mChangeDownloadDir = findPreference(SafeSlingerPrefs.pref.DOWNLOAD_DIRECTORY);
                        mChangeDownloadDir.setSummary(SafeSlingerPrefs.getDownloadDir());
                        break;
                    case RESULT_CANCELED:
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case BaseActivity.DIALOG_ABOUT:
                return BaseActivity.xshowAbout(SettingsActivity.this).create();
            case BaseActivity.DIALOG_LICENSE:
                return BaseActivity.xshowLicense(SettingsActivity.this).create();
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void onUserInteraction() {
        // update the cache timeout
        SafeSlinger.updateCachedPassPhrase(SafeSlingerPrefs.getKeyIdString());
    }

    private void showNote(int resId) {
        Toast.makeText(SettingsActivity.this, resId, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.msgr_restore_default);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESTORE_DEFAULTS:
                restoreDefaultPreferences();
                return true;
            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar.
                // Take them back from
                // wherever they came from
                finish();
                return true;
            default:
                break;
        }
        return false;
    }

}