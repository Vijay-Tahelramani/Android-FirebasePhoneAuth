package com.example.firebasephoneauth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText mobile_num, otp_edt_txt;
    private Button send_otp_btn, verify_otp_btn;
    String phoneNumber;
    FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId="";
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private TextView resend_otp_txt;
    protected CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mobile_num = findViewById(R.id.mobile_num);
        otp_edt_txt = findViewById(R.id.otp_edt_txt);

        send_otp_btn = findViewById(R.id.send_otp_btn);
        send_otp_btn.setOnClickListener(this::onClick);

        verify_otp_btn = findViewById(R.id.verify_otp_btn);
        verify_otp_btn.setOnClickListener(this::onClick);

        resend_otp_txt = findViewById(R.id.resend_otp_txt);
        resend_otp_txt.setOnClickListener(this::onClick);

        mAuth = FirebaseAuth.getInstance();
        setUpOTPCallback();
    }

    @Override
    public void onClick(View view) {

        if (view == send_otp_btn) {
            if (mobile_num.getText().toString().trim().length() == 10) {
                phoneNumber = mobile_num.getText().toString().trim();
                sendOTP();
            }
        }
        else if (view == verify_otp_btn) {
            PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(mVerificationId, otp_edt_txt.getText().toString().trim());
            signInWithPhoneAuthCredential(phoneAuthCredential);
        }
        else if (view == resend_otp_txt) {
            resend_otp_txt.setEnabled(false);
            resend_otp_txt.setTextColor(getResources().getColor(android.R.color.darker_gray));
            reSendOTP();
        }
    }

    private void sendOTP() {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+91"+phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        mAuth.useAppLanguage();
    }

    private void reSendOTP() {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+91"+phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .setForceResendingToken(mResendToken)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void setUpOTPCallback() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.

                String code = credential.getSmsCode();
                Log.d("onVerifyComplete",code);
                if (code != null) {
                    if (otp_edt_txt!=null) {
                        otp_edt_txt.setText(code);
                    }
                }
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.

                Log.w( "onVerificationFailed", e);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Toast.makeText(MainActivity.this, "Invalid Mobile Number!", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                super.onCodeAutoRetrievalTimeOut(s);
                Log.d("onCodeAutoRetrieval","Auto Verification is not possible!");
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                Log.d( "onCodeSent" , verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                send_otp_btn.setVisibility(View.GONE);
                otp_edt_txt.setVisibility(View.VISIBLE);
                mobile_num.clearFocus();
                otp_edt_txt.requestFocus();
                resend_otp_txt.setVisibility(View.VISIBLE);
                startResendCounter();
                verify_otp_btn.setVisibility(View.VISIBLE);
            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                timer.cancel();
                resend_otp_txt.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Your verification is successful.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startResendCounter() {
        timer = new CountDownTimer(30000, 1000) {

            public void onTick(long millisUntilFinished) {
                resend_otp_txt.setText("Resend OTP (" + millisUntilFinished / 1000+")");
            }

            public void onFinish() {
                resend_otp_txt.setText("Resend OTP");
                resend_otp_txt.setEnabled(true);
                resend_otp_txt.setTextColor(getResources().getColor(R.color.black));
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        if (timer!=null){
            timer.cancel();
        }
        super.onBackPressed();
    }
}