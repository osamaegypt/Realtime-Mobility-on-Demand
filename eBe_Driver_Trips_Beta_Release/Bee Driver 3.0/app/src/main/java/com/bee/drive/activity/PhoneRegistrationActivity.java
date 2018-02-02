package com.bee.drive.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.bee.drive.MainChatActivity;
import com.bee.drive.R;
import com.bee.drive.data.FriendDB;
import com.bee.drive.data.GroupDB;
import com.bee.drive.data.StaticConfig;
import com.bee.drive.model.User;
import com.bee.drive.service.ServiceUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.rilixtech.Country;
import com.rilixtech.CountryCodePicker;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;


/**
 * Created by Djimgou Patrick
 * Created on 09-oct-17.
 */

public class PhoneRegistrationActivity extends AppCompatActivity {

    Button mStartButton ;
    private EditText  email_user , UserNameRegistration ;
    private Spinner DriverType;
    private FloatingActionButton CloseRegisterCarte;



    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser user;
    private static final String TAG = "PhoneAuthActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_registration);


        mStartButton = (Button) findViewById(R.id.bt_go);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String Driver_Type = DriverType.getSelectedItem().toString().trim();
                String Name = UserNameRegistration.getText().toString().trim();
                if(Name.length() < 3 ){
                    Toast.makeText(getApplicationContext(), "invalid Name  !" , Toast.LENGTH_LONG).show();

                }else {

                    User newUser = new User();
                    newUser.DriverType = Driver_Type;
                    newUser.email = "no_email@no_email.com";
                    newUser.phone = user.getPhoneNumber();
                    newUser.name = Name;
                    newUser.avata = StaticConfig.STR_DEFAULT_BASE64;
                    // FirebaseDatabase.getInstance().getReference().child("Driver/"+ user.getUid()).setValue(newUser);

                    FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers/"+ StaticConfig.UID).setValue(newUser);

                }



            }
        });

        // Registration ...
        CloseRegisterCarte = (FloatingActionButton) findViewById(R.id.fab_close);
        CloseRegisterCarte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(PhoneRegistrationActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Registration  Dialog")
                        .setMessage("Are you sure you want to stop the registration process and close the app ?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                try{

                                    FirebaseAuth.getInstance().signOut();
                                    FriendDB.getInstance(getApplicationContext()).dropDB();
                                    GroupDB.getInstance(getApplicationContext()).dropDB();
                                    ServiceUtils.stopServiceFriendChat(getApplicationContext(), true);
                                    finish();

                                }catch (Exception ex){
                                    ex.printStackTrace();
                                }
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
                finish();
            }
        });


        UserNameRegistration = (EditText) findViewById(R.id.username);
        email_user = (EditText) findViewById(R.id.email_user);
        DriverType = (Spinner)findViewById(R.id.spinner);

        mAuth = FirebaseAuth.getInstance();
        initFirebase();

    }

    private void initFirebase() {

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    StaticConfig.UID = user.getUid();
                } else {
                    PhoneRegistrationActivity.this.finish();

                }
            }
        };
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new AlertDialog.Builder(PhoneRegistrationActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Registration  Dialog")
                .setMessage("Are you sure you want to stop the registration process and close the app ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        try{

                            FirebaseAuth.getInstance().signOut();
                            FriendDB.getInstance(getApplicationContext()).dropDB();
                            GroupDB.getInstance(getApplicationContext()).dropDB();
                            ServiceUtils.stopServiceFriendChat(getApplicationContext(), true);
                            finish();

                        }catch (Exception ex){
                            ex.printStackTrace();
                        }
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }


}
