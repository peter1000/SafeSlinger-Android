/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2010-2015 Carnegie Mellon University
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

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import edu.cmu.cylab.starslinger.MyLog;
import edu.cmu.cylab.starslinger.R;
import edu.cmu.cylab.starslinger.SafeSlinger;
import edu.cmu.cylab.starslinger.SafeSlingerConfig;
import edu.cmu.cylab.starslinger.SafeSlingerConfig.extra;
import edu.cmu.cylab.starslinger.SafeSlingerPrefs;
import edu.cmu.cylab.starslinger.model.UserData;

public class PassPhraseActivity extends Activity {
    private static final String TAG = SafeSlingerConfig.LOG_TAG;
    private static final int MENU_HELP = 1;
    private static final int MENU_FEEDBACK = 2;
    private static final int DIALOG_FORGOT = 3;
    private static final int DIALOG_HELP = 4;
    private static final int MENU_EULA = 5;
    private static final int MENU_PRIVACY = 6;
    private static final int MENU_ABOUT = 7;
    private Button mButtonOk;
    private Button mButtonForgot;
    private ImageButton mButtonHelp;
    private TextView mTextViewVersion;
    private EditText mEditTextPassOld;
    private EditText mEditTextPassNew;
    private EditText mEditTextPassDone;
    private boolean mChangePassPhrase = false;
    private boolean mCreatePassPhrase = false;
    private int mUserNumber = 0;
    private Handler mHandler;
    private final static int mMsPollInterval = 1000;
    public static final int RESULT_CLOSEANDCONTINUE = 999;
    public static final int RESULT_BACKPRESSED = 998;
    private ArrayList<UserData> mUsers;
    private UserAdapter mUserAdapter;
    private Spinner mSpinnerUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Safeslinger_PassPhrase);
        super.onCreate(savedInstanceState);
        SafeSlinger.setPassphraseOpen(true);

        setContentView(R.layout.passphrase);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mButtonOk = (Button) findViewById(R.id.PassButtonOK);
        mButtonForgot = (Button) findViewById(R.id.PassButtonForgot);
        mButtonHelp = (ImageButton) findViewById(R.id.PassButtonHelp);
        mSpinnerUser = (Spinner) findViewById(R.id.PassSpinnerUserName);
        mTextViewVersion = (TextView) findViewById(R.id.PassTextViewVersion);
        mEditTextPassOld = (EditText) findViewById(R.id.EditTextPassphraseConfirm);
        mEditTextPassNew = (EditText) findViewById(R.id.EditTextPassphrase);
        mEditTextPassDone = (EditText) findViewById(R.id.EditTextPassphraseAgain);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        int userTotal = 0;
        if (extras != null) {
            userTotal = extras.getInt(extra.USER_TOTAL);
            mCreatePassPhrase = extras.getBoolean(extra.CREATE_PASS_PHRASE);
            mChangePassPhrase = extras.getBoolean(extra.CHANGE_PASS_PHRASE);
        }

        mTextViewVersion.setText(SafeSlingerConfig.getVersionName());

        // find all user accounts
        mUsers = new ArrayList<UserData>();
        for (int userNumber = 0; userNumber < userTotal; userNumber++) {
            String name = SafeSlingerPrefs.getContactName(userNumber);
            long date = SafeSlingerPrefs.getKeyDate(userNumber);
            mUsers.add(new UserData(name, date, SafeSlingerPrefs.getUser() == userNumber));
        }

        if (mCreatePassPhrase) {
            mEditTextPassOld.setVisibility(View.GONE);

            mEditTextPassNew.setVisibility(View.VISIBLE);
            mEditTextPassNew.setHint(R.string.label_PassHintCreate);

            mEditTextPassDone.setVisibility(View.VISIBLE);
            mEditTextPassDone.setHint(R.string.label_PassHintRepeat);

            mButtonForgot.setVisibility(View.INVISIBLE);
        } else if (mChangePassPhrase) {
            mEditTextPassOld.setVisibility(View.VISIBLE);
            mEditTextPassOld.setHint(R.string.label_PassHintCurrent);

            mEditTextPassNew.setVisibility(View.VISIBLE);
            mEditTextPassNew.setHint(R.string.label_PassHintChange);

            mEditTextPassDone.setVisibility(View.VISIBLE);
            mEditTextPassDone.setHint(R.string.label_PassHintRepeat);

            mButtonForgot.setVisibility(View.INVISIBLE);

            mHandler = new Handler();
            mHandler.removeCallbacks(checkBackoffRelease);
            mHandler.post(checkBackoffRelease);
        } else { // login
            mEditTextPassOld.setVisibility(View.GONE);

            mEditTextPassNew.setVisibility(View.GONE);

            mEditTextPassDone.setVisibility(View.VISIBLE);
            mEditTextPassDone.setHint(R.string.label_PassHintEnter);

            mButtonForgot.setVisibility(View.VISIBLE);

            mHandler = new Handler();
            mHandler.removeCallbacks(checkBackoffRelease);
            mHandler.post(checkBackoffRelease);
        }

        mButtonOk.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // if soft input open, close it...
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mEditTextPassDone.getWindowToken(), 0);

                doValidatePassphrase();
            }
        });

        mButtonForgot.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showForgot();
            }
        });

        mButtonHelp.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // open menu
                PassPhraseActivity.this.openOptionsMenu();
            }
        });

        mEditTextPassDone.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doValidatePassphrase();
                    return true;
                }
                return false;
            }
        });

        mUserAdapter = new UserAdapter(this, this, mUsers);
        mSpinnerUser.setAdapter(mUserAdapter);

        // make sure user selection is valid
        int userNumber = SafeSlingerPrefs.getUser();
        if (userNumber >= userTotal) {
            mUserNumber = 0; // back to default
            SafeSlingerPrefs.setUser(mUserNumber);
        } else {
            mUserNumber = userNumber;
        }

        mSpinnerUser.setSelection(mUserNumber);
        mSpinnerUser.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long i) {
                doUpdateUserSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // We don't need to worry about nothing being selected, since
                // Spinners don't allow
                // this.
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SafeSlinger.setPassphraseOpen(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SafeSlinger.appRunning();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void doUpdateUserSelection(int position) {
        // Read current user selection
        if (mUserNumber != position) {
            mUserNumber = position;
            // save selected
            SafeSlingerPrefs.setUser(mUserNumber);
            // set new user choice and exit...
            setResult(RESULT_OK);
            finish();
        }
        mUserNumber = SafeSlingerPrefs.getUser();

        // Update the user spinner
        mUserAdapter.notifyDataSetChanged();
    }

    private void doValidatePassphrase() {
        String passPhrase3 = "" + mEditTextPassOld.getText();
        String passPhrase2 = "" + mEditTextPassDone.getText();
        if (mChangePassPhrase || mCreatePassPhrase) {
            String passPhrase1 = "" + mEditTextPassNew.getText();
            if (!passPhrase1.equals(passPhrase2)) {
                showNote(R.string.error_passPhrasesDoNotMatch);
                return;
            } else if (passPhrase2.length() < SafeSlingerConfig.MIN_PASSLEN) {
                showNote(String.format(getString(R.string.error_minPassphraseRequire),
                        SafeSlingerConfig.MIN_PASSLEN));
                return;
            } else if (passPhrase2.equals("")) {
                showNote(R.string.error_noPassPhrase);
                return;
            }
        }
        Intent data = new Intent();
        if (mCreatePassPhrase) {
            data.putExtra(extra.PASS_PHRASE_NEW, passPhrase2);
        } else if (mChangePassPhrase) {
            data.putExtra(extra.PASS_PHRASE_OLD, passPhrase3);
            data.putExtra(extra.PASS_PHRASE_NEW, passPhrase2);
        } else { // login
            data.putExtra(extra.PASS_PHRASE_OLD, passPhrase2);
        }

        setResult(RESULT_OK, data);
        finish();
    }

    private Runnable checkBackoffRelease = new Runnable() {

        @Override
        public void run() {

            long nextPassAttemptDate = SafeSlingerPrefs.getNextPassAttemptDate();
            long now = new Date().getTime();
            boolean enabled = (nextPassAttemptDate == 0 || now > (nextPassAttemptDate - 1000));
            if (!enabled) {
                String waitTime = DateUtils.getRelativeTimeSpanString(nextPassAttemptDate, now,
                        DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString();

                mEditTextPassDone.clearComposingText();
                mEditTextPassDone.setText("");
                mEditTextPassDone.setHint(getString(R.string.label_PassHintBackoff) + " "
                        + waitTime);
            } else {
                if (mCreatePassPhrase) {
                    mEditTextPassDone.setHint(R.string.label_PassHintRepeat);
                } else if (mChangePassPhrase) {
                    mEditTextPassDone.setHint(R.string.label_PassHintRepeat);
                } else { // login
                    mEditTextPassDone.setHint(R.string.label_PassHintEnter);
                }
            }
            mEditTextPassNew.setEnabled(enabled);
            mEditTextPassDone.setEnabled(enabled);
            mButtonOk.setEnabled(enabled);

            mHandler.postDelayed(this, mMsPollInterval);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem iHelp = menu.add(0, MENU_HELP, 0, R.string.menu_Help).setIcon(
                android.R.drawable.ic_menu_help);
        MenuItem iAbout = menu.add(0, MENU_ABOUT, 0, R.string.menu_About).setIcon(
                android.R.drawable.ic_menu_info_details);
        MenuItem iEula = menu.add(0, MENU_EULA, 0, R.string.menu_License).setIcon(
                android.R.drawable.ic_menu_edit);
        MenuItem iPrivacy = menu.add(0, MENU_PRIVACY, 0, R.string.menu_PrivacyPolicy).setIcon(
                android.R.drawable.ic_menu_edit);
        MenuItem iFeedback = menu.add(0, MENU_FEEDBACK, 0, R.string.menu_sendFeedback).setIcon(
                android.R.drawable.ic_menu_send);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HELP:
                showHelp(getString(R.string.title_passphrase), getString(R.string.help_passphrase));
                return true;
            case MENU_FEEDBACK:
                SafeSlinger.getApplication().showFeedbackEmail(PassPhraseActivity.this);
                return true;
            case MENU_EULA:
                showWebPage(SafeSlingerConfig.EULA_URL);
                return true;
            case MENU_PRIVACY:
                showWebPage(SafeSlingerConfig.PRIVACY_URL);
                return true;
            case MENU_ABOUT:
                if (!isFinishing()) {
                    try {
                        removeDialog(BaseActivity.DIALOG_ABOUT);
                        showDialog(BaseActivity.DIALOG_ABOUT);
                    } catch (BadTokenException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            default:
                break;
        }
        return false;
    }

    protected void showWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        boolean actionAvailable = getPackageManager().resolveActivity(intent, 0) != null;
        if (actionAvailable) {
            startActivity(intent);
        } else {
            showNote(SafeSlinger.getUnsupportedFeatureString("View Web Page"));
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_FORGOT:
                return xshowForgot(PassPhraseActivity.this).create();
            case DIALOG_HELP:
                return xshowHelp(PassPhraseActivity.this, args).create();
            case BaseActivity.DIALOG_ABOUT:
                return BaseActivity.xshowAbout(PassPhraseActivity.this).create();
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

    protected void showNote(int resId) {
        showNote(getString(resId));
    }

    protected void showNote(Exception e) {
        String msg = e.getLocalizedMessage();
        if (TextUtils.isEmpty(msg)) {
            showNote(e.getClass().getSimpleName());
        } else {
            showNote(msg);
        }
    }

    protected void showNote(String msg) {
        MyLog.i(TAG, msg);
        if (msg != null) {
            int readDuration = msg.length() * SafeSlingerConfig.MS_READ_PER_CHAR;
            if (readDuration <= SafeSlingerConfig.SHORT_DELAY) {
                Toast toast = Toast.makeText(PassPhraseActivity.this, msg.trim(),
                        Toast.LENGTH_SHORT);
                toast.show();
            } else if (readDuration <= SafeSlingerConfig.LONG_DELAY) {
                Toast toast = Toast
                        .makeText(PassPhraseActivity.this, msg.trim(), Toast.LENGTH_LONG);
                toast.show();
            } else {
                showHelp(getString(R.string.app_name), msg.trim());
            }
        }
    }

    protected void showForgot() {
        if (!isFinishing()) {
            try {
                removeDialog(DIALOG_FORGOT);
                showDialog(DIALOG_FORGOT);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    protected AlertDialog.Builder xshowForgot(Activity act) {
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(R.string.menu_ForgotPassphrase);
        ad.setMessage(R.string.label_WarnForgotPassphrase);
        ad.setCancelable(true);
        ad.setPositiveButton(R.string.btn_CreateNewKey, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // new key generation, by setting last position +1
                doUpdateUserSelection(mUsers.size());
            }
        });
        ad.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    protected void showHelp(String title, String msg) {
        Bundle args = new Bundle();
        args.putString(extra.RESID_TITLE, title);
        args.putString(extra.RESID_MSG, msg);
        if (!isFinishing()) {
            try {
                removeDialog(DIALOG_HELP);
                showDialog(DIALOG_HELP, args);
            } catch (BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    protected static AlertDialog.Builder xshowHelp(Activity act, Bundle args) {
        String title = args.getString(extra.RESID_TITLE);
        String msg = args.getString(extra.RESID_MSG);
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(title);
        ad.setMessage(msg);
        ad.setCancelable(true);
        ad.setNeutralButton(R.string.btn_Close, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        return ad;
    }

    @Override
    public void onBackPressed() {
        // eat the back key to find the manual vs auto close
        setResult(RESULT_BACKPRESSED);
        finish();
    }
}
