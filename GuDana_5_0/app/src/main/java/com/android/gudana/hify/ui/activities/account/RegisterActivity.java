package com.android.gudana.hify.ui.activities.account;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.transition.Fade;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.gudana.gpslocationtracking.LocationTrack;
import com.android.gudana.hify.utils.AnimationUtil;
import com.android.gudana.hify.utils.PathUtil;
import com.android.gudana.hify.utils.database.UserHelper;
import com.android.gudana.R;
import com.android.gudana.tindroid.Cache;
import com.android.gudana.tindroid.CredentialsFragment;
import com.android.gudana.tindroid.account.Utils;
import com.android.gudana.tindroid.media.VxCard;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.yalantis.ucrop.UCrop;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.ServerResponseException;
import co.tinode.tinodesdk.Tinode;
import co.tinode.tinodesdk.model.Credential;
import co.tinode.tinodesdk.model.MetaSetDesc;
import co.tinode.tinodesdk.model.ServerMessage;
import de.hdodenhof.circleimageview.CircleImageView;
import es.dmoral.toasty.Toasty;
import id.zelory.compressor.Compressor;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "SignUpFragment";

    private static final int PICK_IMAGE =100 ;
    public Uri imageUri;

    private static final int PIC_CROP = 1;
    public StorageReference storageReference;
    public ProgressDialog mDialog;
    public String name_, pass_, pass_2, email_,username_,location_ = "--##--";
    private EditText name,email,password_2, password,username;
    private CircleImageView profile_image;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;
    LocationTrack locationTrack;
    String UserAdresse = "";
    private ProgressDialog mDialog_compress_image;
    Button register ;


    public static void startActivity(Activity activity, Context context, View view) {
        Intent intent = new Intent(context, RegisterActivity.class);
        context.startActivity(intent);

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hi_activity_register);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

        mAuth= FirebaseAuth.getInstance();
        storageReference= FirebaseStorage.getInstance().getReference().child("images");
        firebaseFirestore= FirebaseFirestore.getInstance();
        imageUri=null;
        UserHelper userHelper = new UserHelper(this);


        name=(EditText)findViewById(R.id.name);
        email=(EditText)findViewById(R.id.email);
        password=(EditText)findViewById(R.id.password);
        username=(EditText)findViewById(R.id.username);
        password_2 = (EditText)findViewById(R.id.password_repeat);

        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Please wait... we proceed your registration ");
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);
        mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


                try{
                    mDialog.dismiss();
                    dialog.dismiss();
                    Toasty.info(RegisterActivity.this, "Registration Cancelled", Toast.LENGTH_SHORT).show();
                    Intent LoginEmail = new Intent(RegisterActivity.this, com.android.gudana.hify.ui.activities.account.LoginActivity.class);
                    startActivity(LoginEmail);
                    RegisterActivity.this.finish();

                }catch(Exception ex){
                    ex.printStackTrace();
                }

            }
        });
        mDialog.setCanceledOnTouchOutside(false);


        register = findViewById(R.id.button);

        profile_image=findViewById(R.id.profile_image);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Fade fade = new Fade();
            fade.excludeTarget(findViewById(R.id.layout), true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fade.excludeTarget(android.R.id.statusBarBackground, true);
                fade.excludeTarget(android.R.id.navigationBarBackground, true);
                getWindow().setEnterTransition(fade);
                getWindow().setExitTransition(fade);
            }
        }

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(imageUri!=null){
                    username_=username.getText().toString();
                    name_=name.getText().toString();
                    email_=email.getText().toString();
                    pass_=password.getText().toString();
                    pass_2 = password_2.getText().toString();

                    mDialog.show();

                    if (TextUtils.isEmpty(username_)) {

                        AnimationUtil.shakeView(username, RegisterActivity.this);
                        mDialog.dismiss();

                    }

                    if (TextUtils.isEmpty(name_)) {

                        AnimationUtil.shakeView(name, RegisterActivity.this);
                        mDialog.dismiss();

                    }
                    if (TextUtils.isEmpty(email_)) {

                        AnimationUtil.shakeView(email, RegisterActivity.this);
                        mDialog.dismiss();

                    }
                    if (TextUtils.isEmpty(pass_) ||  TextUtils.isEmpty(pass_2)){


                        // chehck password  are correct  ...
                        AnimationUtil.shakeView(password, RegisterActivity.this);
                        AnimationUtil.shakeView(password_2, RegisterActivity.this);

                        mDialog.dismiss();

                    }

                    if (TextUtils.isEmpty(location_)) {

                        mDialog.dismiss();

                    }

                    if (!TextUtils.isEmpty(name_) || !TextUtils.isEmpty(email_) ||
                            !TextUtils.isEmpty(pass_) || !TextUtils.isEmpty(username_) || !TextUtils.isEmpty(location_)) {

                        if(pass_.equals(pass_2)){

                            // mDialog.show();
                            register.setEnabled(false);
                            firebaseFirestore.collection("Usernames")
                                    .document(username_)
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if(!documentSnapshot.exists()){
                                                registerUser("xxxxxxwwwwwwwww");
                                                // onSignUp(email_.trim() ,name_.trim() ,email_.trim(),pass_.trim());

                                            }else{
                                                register.setEnabled(true);

                                                //Toast.makeText(RegisterActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                                                AnimationUtil.shakeView(username, RegisterActivity.this);
                                                mDialog.dismiss();
                                                new LovelyInfoDialog(RegisterActivity.this)
                                                        .setTopColorRes(R.color.colorPrimary)
                                                        .setIcon(R.mipmap.ic_infos)
                                                        //This will add Don't show again checkbox to the dialog. You can pass any ID as argument
                                                        //.setNotShowAgainOptionEnabled(0)
                                                        //.setNotShowAgainOptionChecked(true)
                                                        .setTitle("Infos ")
                                                        .setMessage("Username already exists")
                                                        .show();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("Error",e.getMessage());

                                            register.setEnabled(true);
                                            mDialog.dismiss();
                                            Toasty.error(RegisterActivity.this, "error!", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        }else{
                            register.setEnabled(true);
                            mDialog.dismiss();
                            Toasty.error(RegisterActivity.this, "please  chechk your password !", Toast.LENGTH_SHORT).show();
                        }

                    }else{

                        AnimationUtil.shakeView(username, RegisterActivity.this);
                        AnimationUtil.shakeView(name, RegisterActivity.this);
                        AnimationUtil.shakeView(email, RegisterActivity.this);
                        AnimationUtil.shakeView(password, RegisterActivity.this);
                        mDialog.dismiss();
                        register.setEnabled(true);
                        Toasty.error(RegisterActivity.this, "error", Toast.LENGTH_SHORT).show();

                    }

                }else{
                    AnimationUtil.shakeView(profile_image, RegisterActivity.this);
                    //Toast.makeText(RegisterActivity.this, "We recommend you to set a profile picture", Toast.LENGTH_SHORT).show();
                    mDialog.dismiss();
                    register.setEnabled(true);
                    mDialog.dismiss();
                    Toasty.error(RegisterActivity.this, "error", Toast.LENGTH_SHORT).show();

                    new LovelyInfoDialog(RegisterActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.mipmap.ic_infos)
                            //This will add Don't show again checkbox to the dialog. You can pass any ID as argument
                            //.setNotShowAgainOptionEnabled(0)
                            //.setNotShowAgainOptionChecked(true)
                            .setTitle("Infos ")
                            .setMessage("We recommend you to set a profile picture")
                            .show();
                }
            }
        });


        locationTrack = new LocationTrack(RegisterActivity.this);
        // chech if gps is enable  ....
        if (locationTrack.canGetLocation()) {

            double longitude = locationTrack.getLongitude();
            double latitude = locationTrack.getLatitude();
            location_ = getAddress(latitude, longitude);

        } else {
            locationTrack.showSettingsAlert();
        }


        // 77 ASK pERMISSION
        askPermission();
    }


    //get the adresse
    public String getAddress(double lat, double lng) {

        String Adresse = "";
        Geocoder geocoder = new Geocoder(RegisterActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj = addresses.get(0);
            String add = obj.getAddressLine(0);
            add = add + "\n" + obj.getCountryName();
            add = add + "\n" + obj.getCountryCode();
            add = add + "\n" + obj.getAdminArea();
            add = add + "\n" + obj.getPostalCode();
            //add = add + "\n" + obj.getSubAdminArea();
            add = add + "\n" + obj.getLocality();
            //add = add + "\n" + obj.getSubThoroughfare();

            Log.v("IGA", "Address" + add);
            Adresse = add;
            // Toast.makeText(this, "Address=>" + add,
            // Toast.LENGTH_SHORT).show();

            // TennisAppActivity.showDialog(add);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return  Adresse;

    }

    private void registerUser(final String Uid_tindroid) {

        mAuth.createUserWithEmailAndPassword(email_, pass_).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull final Task<AuthResult> task) {
                if (task.isSuccessful()) {

                    Map<String,Object> usernameMap=new HashMap<String, Object>();
                    usernameMap.put("username",username_);

                    firebaseFirestore.collection("Usernames")
                            .document(username_)
                            .set(usernameMap)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    task.getResult()
                                            .getUser()
                                            .sendEmailVerification()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {

                                                    final String userUid = task.getResult().getUser().getUid();
                                                    final StorageReference user_profile = storageReference.child(userUid + ".jpg");
                                                    user_profile.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                                        @Override
                                                        public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {
                                                            if (task.isSuccessful()) {

                                                               user_profile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                                   @Override
                                                                   public void onSuccess(Uri uri) {

                                                                       String token_id = FirebaseInstanceId.getInstance().getToken();
                                                                       Map<String, Object> userMap = new HashMap<>();
                                                                       userMap.put("id", userUid);
                                                                       userMap.put("name", name_);
                                                                       userMap.put("image", uri.toString());
                                                                       userMap.put("email", email_);
                                                                       userMap.put("bio",getString(R.string.default_bio));
                                                                       userMap.put("username", username_);
                                                                       userMap.put("location", location_);
                                                                       userMap.put("uid_tindroid", Uid_tindroid);
                                                                       userMap.put("token_id", FirebaseInstanceId.getInstance().getToken()); // hier we must put the token id    ....   ...

                                                                       firebaseFirestore.collection("Users").document(userUid).set(userMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                           @Override
                                                                           public void onSuccess(Void aVoid) {

                                                                               FirebaseAuth.getInstance().signOut();
                                                                               onSignUp(email_.trim() ,name_.trim() ,email_.trim(),pass_.trim() , userUid);

                                                                           }
                                                                       }).addOnFailureListener(new OnFailureListener() {
                                                                           @Override
                                                                           public void onFailure(@NonNull Exception e) {
                                                                               mDialog.dismiss();
                                                                               register.setEnabled(true);
                                                                               //Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                               new LovelyInfoDialog(RegisterActivity.this)
                                                                                       .setTopColorRes(R.color.colorPrimary)
                                                                                       .setIcon(R.mipmap.ic_infos)
                                                                                       //This will add Don't show again checkbox to the dialog. You can pass any ID as argument
                                                                                       //.setNotShowAgainOptionEnabled(0)
                                                                                       //.setNotShowAgainOptionChecked(true)
                                                                                       .setTitle("Infos")
                                                                                       .setMessage("Error : "+e.getMessage())
                                                                                       .show();
                                                                           }
                                                                       });


                                                                       // registering new User s
                                                                       // Registering user with data he gave us
                                                                       FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

                                                                       if(firebaseUser != null)
                                                                       {
                                                                           String userid = firebaseUser.getUid();

                                                                           // "Packing" user data #
                                                                           // je doi declarer un objet user icic et voir comment ce  se passe ...

                                                                           Map map = new HashMap<>();
                                                                           map.put("token", FirebaseInstanceId.getInstance().getToken()); // i can use this  for cloud mess
                                                                           map.put("name", username_);
                                                                           map.put("email", email_);
                                                                           map.put("status", "Welcome to my GuDana Profile!");
                                                                           map.put("image", uri.toString());
                                                                           map.put("cover", uri.toString());
                                                                           map.put("date", ServerValue.TIMESTAMP);

                                                                           // Uploading user data
                                                                           // beause of final dig .... :) ;
                                                                           //StaticConfigUser_fromFirebase.USER_URL_IMAGE = uri.toString();


                                                                           FirebaseDatabase.getInstance().getReference().child("Users").child(userid).setValue(map).addOnCompleteListener(new OnCompleteListener<Void>()
                                                                           {
                                                                               @Override
                                                                               public void onComplete(@NonNull Task<Void> task)
                                                                               {
                                                                                   if(task.isSuccessful())
                                                                                   {
                                                                                       //mDialog.dismiss();
                                                                                       Toast.makeText(getApplicationContext(), "User registered .", Toast.LENGTH_LONG).show();

                                                                                       // after  save the   User data on Device  for later Use  ....   ...
                                                                                       // StaticConfigUser_fromFirebase.STR_EXTRA_USERNAME = username_;
                                                                                       //StaticConfigUser_fromFirebase.STR_EXTRA_EMAIL = email_;
                                                                                       //StaticConfigUser_fromFirebase.STR_USER_ID = userUid;
                                                                                       //StaticConfigUser_fromFirebase.USER_NAME = name_;
                                                                                       //StaticConfigUser_fromFirebase.STR_USER_TOKEN_FCM = FirebaseInstanceId.getInstance().getToken();
                                                                                   }
                                                                                   else
                                                                                   {
                                                                                       mDialog.dismiss();
                                                                                       Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();

                                                                                   }
                                                                               }
                                                                           });
                                                                       }


                                                                   }
                                                               }).addOnFailureListener(new OnFailureListener() {
                                                                           @Override
                                                                           public void onFailure(@NonNull Exception e) {
                                                                               mDialog.dismiss();
                                                                               Toasty.error(RegisterActivity.this, "Registration failure ...please try again", Toast.LENGTH_SHORT).show();

                                                                           }
                                                                });


                                                            } else {
                                                                mDialog.dismiss();
                                                                Toasty.error(RegisterActivity.this, "Registration failure ...please try again", Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });

                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    mDialog.dismiss();
                                                    task.getResult().getUser().delete();
                                                    Toasty.error(RegisterActivity.this, "Registration failure ...please try again", Toast.LENGTH_SHORT).show();

                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    mDialog.dismiss();
                                    Log.e("Error",e.getMessage());
                                    Toasty.error(RegisterActivity.this, "Registration failure ...please try again", Toast.LENGTH_SHORT).show();
                                }
                            });


                } else {
                    mDialog.dismiss();
                    register.setEnabled(true);

                    //Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    new LovelyInfoDialog(RegisterActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.mipmap.ic_infos)
                            //This will add Don't show again checkbox to the dialog. You can pass any ID as argument
                            //.setNotShowAgainOptionEnabled(0)
                            //.setNotShowAgainOptionChecked(true)
                            .setTitle("Infos")
                            .setMessage("Error : " + task.getException().getMessage())
                            .show();

                }
            }
        });

    }


    private void askPermission() {

        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.GET_ACCOUNTS,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.ACCESS_NETWORK_STATE


                )
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if(report.isAnyPermissionPermanentlyDenied()){
                            Toast.makeText(RegisterActivity.this, "You have denied some permissions permanently, if the app force close try granting permission from settings.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                }).check();

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // we must cimpresse the image here ....

        Uri realPaturi = null;
        if(requestCode==PICK_IMAGE){
            if(resultCode==RESULT_OK){
                imageUri=data.getData();
                realPaturi = data.getData();
                // start crop activity
                final UCrop.Options options = new UCrop.Options();
                options.setCompressionFormat(Bitmap.CompressFormat.PNG);
                options.setCompressionQuality(100);
                options.setShowCropGrid(true);

                // compress images

                mDialog_compress_image = new ProgressDialog(this);
                mDialog_compress_image.setMessage("wait while we compress selected image ... ");
                mDialog_compress_image.setIndeterminate(true);
                mDialog_compress_image.setCanceledOnTouchOutside(false);
                mDialog_compress_image.setCancelable(false);
                mDialog_compress_image.show();

                try{

                    new Compressor(this)
                            .compressToFileAsFlowable(new File(PathUtil.getPath(RegisterActivity.this, imageUri)))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<File>() {
                                @Override
                                public void accept(File file) {
                                    //compressedImage = file;
                                    // new PostImage.UploadFileToServer(file, Config.IMAGES_UPLOAD_URL,finalI).execute();
                                    // dismis dialog
                                    mDialog_compress_image.dismiss();
                                    System.out.println(file.toURI());
                                    // je ne suis pas sur du concept global
                                    UCrop.of(Uri.fromFile(file), Uri.fromFile(new File(getCacheDir(), "hify_user_profile_picture.png")))
                                            .withAspectRatio(1, 1)
                                            .withOptions(options)
                                            .start(RegisterActivity.this);

                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) {
                                    mDialog_compress_image.dismiss();
                                    Toasty.error(RegisterActivity.this, "please select another image ", Toast.LENGTH_SHORT).show();
                                    throwable.printStackTrace();
                                    //showError(throwable.getMessage());
                                }
                            });

                }catch (Exception ex){
                    mDialog_compress_image.dismiss();
                    Toasty.error(RegisterActivity.this, "please select another image ", Toast.LENGTH_SHORT).show();
                    ex.printStackTrace();
                }


            }
        }
        if (requestCode == UCrop.REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                imageUri = UCrop.getOutput(data);
                CircleImageView new_profile_image;

                //profile_image = null;
                //profile_image=findViewById(R.id.profile_image);
                if(imageUri!=null){
                    profile_image.setImageURI(imageUri);
                }else{
                    Toasty.error(RegisterActivity.this, "wrong image paths  ...please chehck your image", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == UCrop.RESULT_ERROR) {
                Log.e("Error", "Crop error:" + UCrop.getError(data).getMessage());
            }
        }

    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransitionExit();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransitionEnter();
    }

    /**
     * Overrides the pending Activity transition by performing the "Enter" animation.
     */
    protected void overridePendingTransitionEnter() {
        overridePendingTransition(R.anim.hi_slide_from_right, R.anim.hi_slide_to_left);
    }

    /**
     * Overrides the pending Activity transition by performing the "Exit" animation.
     */
    protected void overridePendingTransitionExit() {
        overridePendingTransition(R.anim.hi_slide_from_left, R.anim.hi_slide_to_right);
    }

    public void setProfilepic(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE);
    }

    // add register methode   Tindroid

    public void onSignUp(final String login ,final String fullName , final String email  ,final String password , final String User_uid) {

        final Button signUp = (Button) findViewById(R.id.button);
        signUp.setEnabled(false);
        register.setEnabled(false);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(RegisterActivity.this);
        String hostName = sharedPref.getString(Utils.PREFS_HOST_NAME, Cache.HOST_NAME);
        boolean tls = sharedPref.getBoolean(Utils.PREFS_USE_TLS, false);
        final CircleImageView avatar = findViewById(R.id.profile_image);

        final Tinode tinode = Cache.getTinode();
        try {
            // This is called on the websocket thread.
            mDialog.show();
            tinode.connect(hostName, tls)
                    .thenApply(
                            new PromisedReply.SuccessListener<ServerMessage>() {
                                @Override
                                public PromisedReply<ServerMessage> onSuccess(ServerMessage ignored_msg) throws Exception {
                                    // Try to create a new account.
                                    Bitmap bmp = null;
                                    try {
                                        bmp = ((BitmapDrawable) avatar.getDrawable()).getBitmap();
                                    } catch (ClassCastException ignored) {
                                        // If image is not loaded, the drawable is a vector.
                                        // Ignore it.
                                        ignored.printStackTrace();
                                    }

                                    // cocat user name  with some information   firestore user usi
                                    String UserFullname_and_firebase_uid = fullName + "#####" +User_uid;
                                    VxCard vcard = new VxCard(UserFullname_and_firebase_uid, bmp);
                                    return tinode.createAccountBasic(
                                            login, password, true, null,
                                            new MetaSetDesc<VxCard,String>(vcard, null),
                                            Credential.append(null, new Credential("email", email)));
                                }
                            }, null)
                    .thenApply(
                            new PromisedReply.SuccessListener<ServerMessage>() {
                                @Override
                                public PromisedReply<ServerMessage> onSuccess(final ServerMessage msg) {
                                    // Flip back to login screen on success;
                                    RegisterActivity.this.runOnUiThread(new Runnable() {
                                        public void run() {
                                            if (msg.ctrl.code >= 300 && msg.ctrl.text.contains("validate credentials")) {
                                                signUp.setEnabled(true);
                                                CredentialsFragment cf = new CredentialsFragment();
                                                Iterator<String> it = msg.ctrl.getStringIteratorParam("cred");
                                                if (it != null) {
                                                    cf.setMethod(it.next());
                                                }
                                            } else {


                                                // We are requesting immediate login with the new account.
                                                // If the action succeeded, assume we have logged in.
                                                // here we should call the login   call the login Activity with  intent to tell that
                                                // the new users are registerde but he should chech his email to vmake a confirmation  ..
                                                // UiUtils.onLoginSuccess(RegisterActivity.this, signUp);
                                                // start registration on Firebase  or Firestore  ....


                                                mDialog.dismiss();
                                                String TindroidUniqueId = Cache.getTinode().getMyId();
                                                //registerUser(TindroidUniqueId);
                                                Intent LoginEmail = new Intent(RegisterActivity.this, com.android.gudana.hify.ui.activities.account.LoginActivity.class);
                                                LoginEmail.putExtra("Email_Confirmation",true);
                                                startActivity(LoginEmail);
                                                RegisterActivity.this.finish();
                                                //signUp.setEnabled(true);
                                                //
                                            }
                                        }
                                    });
                                    Toasty.error(RegisterActivity.this, "Registration of new Account failed  ! ", Toast.LENGTH_LONG).show();
                                    return null;
                                }
                            },
                            new PromisedReply.FailureListener<ServerMessage>() {
                                @Override
                                public PromisedReply<ServerMessage> onFailure(Exception err) {
                                    final String cause = ((ServerResponseException)err).getReason();
                                    if (cause != null) {
                                        RegisterActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                signUp.setEnabled(true);
                                                switch (cause) {
                                                    case "auth":
                                                        // Invalid login
                                                        // ((EditText) parent.findViewById(R.id.newLogin)).setError(getText(R.string.login_rejected));
                                                        // invalide Login
                                                        mDialog.dismiss();
                                                        Toasty.error(RegisterActivity.this, "Invalid Login ...please check your credentials", Toast.LENGTH_LONG).show();
                                                        break;
                                                    case "email":
                                                        // Duplicate email:
                                                        mDialog.dismiss();
                                                        Toasty.error(RegisterActivity.this, "Invalid Email please check your Email !", Toast.LENGTH_LONG).show();
                                                        // ((EditText) parent.findViewById(R.id.email)).setError(getText(R.string.email_rejected));
                                                        break;
                                                }
                                            }
                                        });
                                    }
                                    // parent.reportError(err, signUp, R.string.error_new_account_failed);
                                    mDialog.dismiss();
                                    Toasty.error(RegisterActivity.this, "Registration of new Account failed  ! ", Toast.LENGTH_LONG).show();
                                    return null;
                                }
                            });

        } catch (Exception e) {
            mDialog.dismiss();
            Toast.makeText(this, "hmmm ...:) Something went wrong with your registration  ", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Something went wrong", e);
            signUp.setEnabled(true);
        }
    }


    public  static void update_tindroid_on_firebase_uid(String uid){

        FirebaseFirestore mFirestore;
        FirebaseAuth mAuth;

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        final DocumentReference userDocument=mFirestore.collection("Users").document(mAuth.getCurrentUser().getUid());

        Map<String,Object> map=new HashMap<>();
            map.put("uid_tindroid",uid.trim());

            userDocument.update(map)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i("Update","success");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i("Update","failed: "+e.getMessage());

                        }
                    });


    }


}
