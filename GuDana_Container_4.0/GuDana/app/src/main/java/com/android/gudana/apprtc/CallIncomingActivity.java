/*
CallIncomingActivity.java
Copyright (C) 2015  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package com.android.gudana.apprtc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gudana.R;
import com.android.gudana.apprtc.compatibility.Compatibility;
import com.android.gudana.apprtc.LinphoneSliders.LinphoneSliderTriggered;
import com.android.gudana.apprtc.linphone.LinphoneManager;
import com.android.gudana.chatapp.activities.ChatActivity;
import com.android.gudana.hify.adapters.UsersAdapter;
import com.android.gudana.hify.ui.activities.friends.SendActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.mediastream.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import es.dmoral.toasty.Toasty;

import static com.android.gudana.chatapp.activities.ChatActivity.Call_dispo;

public class CallIncomingActivity extends Activity implements LinphoneSliderTriggered {
	private static CallIncomingActivity instance;

	private TextView name, number;
	private ImageView  accept, decline;
	private CircleImageView contactPicture;

	private LinphoneCall mCall;
	private LinphoneCoreListenerBase mListener;
	private LinearLayout acceptUnlock;
	private LinearLayout declineUnlock;
	private boolean isScreenActive, alreadyAcceptedOrDeniedCall;
	private float answerX;
	private float declineX;
	LinphoneManager ViCall ;
	private Context mContext;

	private FirebaseFirestore mFirestore;
	private FirebaseUser currentUser;
	public static DatabaseReference userDB;
	private String call_server_id;
	private String call_type = "video";
	private String user_id = null;


	public static CallIncomingActivity instance() {
		return instance;
	}

	public static boolean isInstanciated() {
		return instance != null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		if (getResources().getBoolean(R.bool.orientation_portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}


		mFirestore = FirebaseFirestore.getInstance();
		currentUser= FirebaseAuth.getInstance().getCurrentUser();

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.call_incoming);

		name = (TextView) findViewById(R.id.contact_name);
		number = (TextView) findViewById(R.id.contact_number);
		contactPicture = (CircleImageView) findViewById(R.id.image);

		// set this flag so this activity will stay in front of the keyguard
		int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
		getWindow().addFlags(flags);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		isScreenActive = Compatibility.isScreenOn(pm);

		final int screenWidth = getResources().getDisplayMetrics().widthPixels;

		acceptUnlock = (LinearLayout) findViewById(R.id.acceptUnlock);
		declineUnlock = (LinearLayout) findViewById(R.id.declineUnlock);

		accept = (ImageView) findViewById(R.id.accept);
		decline = (ImageView) findViewById(R.id.decline);
		accept.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isScreenActive) {
					answer();
				} else {
					decline.setVisibility(View.GONE);
					acceptUnlock.setVisibility(View.VISIBLE);
				}
			}
		});

		if(!isScreenActive) {
			accept.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {

					try{

						ViCall.stopRinging();
						answer();

					}catch(Exception ex){

						ex.printStackTrace();
					}

					float curX;
					switch (motionEvent.getAction()) {
						case MotionEvent.ACTION_DOWN:
							acceptUnlock.setVisibility(View.VISIBLE);
							decline.setVisibility(View.GONE);
							answerX = motionEvent.getX();
							break;
						case MotionEvent.ACTION_MOVE:
							curX = motionEvent.getX();
							if((answerX - curX) >= 0)
								view.scrollBy((int) (answerX - curX), view.getScrollY());
							answerX = curX;
							if (curX < screenWidth/4) {
								answer();
								return true;
							}
							break;
						case MotionEvent.ACTION_UP:
							view.scrollTo(0, view.getScrollY());
							decline.setVisibility(View.VISIBLE);
							acceptUnlock.setVisibility(View.GONE);
							break;
					}
					return true;
				}
			});

			decline.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {

					try{

						Call_dispo = true;
						ViCall.stopRinging();

					}catch(Exception ex){

						Call_dispo = true;
						ex.printStackTrace();
					}

					float curX;
					switch (motionEvent.getAction()) {
						case MotionEvent.ACTION_DOWN:
							declineUnlock.setVisibility(View.VISIBLE);
							accept.setVisibility(View.GONE);
							declineX = motionEvent.getX();
							break;
						case MotionEvent.ACTION_MOVE:
							curX = motionEvent.getX();
							view.scrollBy((int) (declineX - curX), view.getScrollY());
							declineX = curX;
							Log.w(curX);
							if (curX > (screenWidth/2)){
								decline();
								return true;
							}
							break;
						case MotionEvent.ACTION_UP:
							view.scrollTo(0, view.getScrollY());
							accept.setVisibility(View.VISIBLE);
							declineUnlock.setVisibility(View.GONE);
							break;

					}
					return true;
				}
			});
		}

		decline.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isScreenActive) {
					decline();
				} else {
					accept.setVisibility(View.GONE);
					acceptUnlock.setVisibility(View.VISIBLE);
				}
			}
		});





		instance = this;
		ViCall = new LinphoneManager(CallIncomingActivity.this.getApplicationContext());
		/// start ringing  and vibrate
		try{

			ViCall.startRinging();

		}catch(Exception ex){
			ex.printStackTrace();

		}

		user_id = getIntent().getStringExtra("userid");
		// sett context ...
		mContext = CallIncomingActivity.this.getApplicationContext();
		InitCallerProfil(user_id);


		askPermission();
		// start chechker
		Check_Correspondantavailibility(CallIncomingActivity.this.getApplicationContext(),user_id);

	}

	private void askPermission() {

		Dexter.withActivity(CallIncomingActivity.this)
				.withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.ACCESS_COARSE_LOCATION,
						Manifest.permission.READ_EXTERNAL_STORAGE,
						Manifest.permission.CAMERA,
						Manifest.permission.RECORD_AUDIO

				)
				.withListener(new MultiplePermissionsListener() {
					@Override
					public void onPermissionsChecked(MultiplePermissionsReport report) {
						if(report.isAnyPermissionPermanentlyDenied()){
							Toasty.warning(CallIncomingActivity.this, "You have denied some permissions permanently, if the app force close try granting permission from settings.", Toast.LENGTH_LONG).show();
						}
					}

					@Override
					public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

					}
				}).check();

	}

	//  get  users data   and print  that on the screen
	public void InitCallerProfil(String CallerUserId) {

		name.setText("GuDaba User");
		FirebaseFirestore.getInstance().collection("Users")
				.document(CallerUserId)
				.get()
				.addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
					@Override
					public void onSuccess(DocumentSnapshot documentSnapshot) {

						String friend_name = "";
						friend_name=documentSnapshot.getString("name");
						//friend_email=documentSnapshot.getString("email");
						String friend_image =documentSnapshot.getString("image");
						//friend_token=documentSnapshot.getString("token");

						name.setText(friend_name);
						//email.setText(friend_email);
						//location.setText(documentSnapshot.getString("location"));
						//bio.setText(documentSnapshot.getString("bio"));

						Glide.with(mContext)
								.setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
								.load(friend_image)
								.into(contactPicture);
					}
				});

	}


	@Override
	protected void onResume() {
		super.onResume();
		instance = this;

		alreadyAcceptedOrDeniedCall = false;
		mCall = null;

	}
	
	@Override
	protected void onStart() {
		super.onStart();
		checkAndRequestCallPermissions();
	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	@Override
	protected void onDestroy() {

		Call_dispo = true;
		super.onDestroy();
	}



	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		/*
		if (LinphoneManager.isInstanciated() && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)) {
			LinphoneManager.getLc().terminateCall(mCall);
			finish();
		}
		*/
		return super.onKeyDown(keyCode, event);
	}

	private void decline() {
		try{

			ViCall.stopRinging();
			// put the  call  dispo enable
			Call_dispo = true;

		}catch(Exception ex){

			Call_dispo = true;
			ex.printStackTrace();
		}

		if (alreadyAcceptedOrDeniedCall) {
			return;
		}
		alreadyAcceptedOrDeniedCall = true;
		
		finish();
	}


	private void answer() {

		try{

			ViCall.stopRinging();

			userDB = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
			// Set the  Driver Response to true ...
			//HashMap map = new HashMap();
			//map.put("Authentified" , "await");
			//userDB.updateChildren(map);
			userDB.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {
					if(dataSnapshot.exists()){
						try{
							Map<String, Object> map_call = (Map<String, Object>) dataSnapshot.getValue();
							// test if the recors Phone already exist  ...if not than
							// than you are a new user   ...
							if(map_call.get("call_id")!=null){
								// than this user is already registered ...
								call_server_id = map_call.get("call_id").toString();
								// Toasty.info(mContext, "Server Channel  : "+ call_server_id, Toast.LENGTH_LONG).show();
								if(map_call.get("call_type")!=null){
									call_type = map_call.get("call_type").toString();
								}else{
									call_type = "audio";
								}


								Call_dispo = false; // you  are not anymore available to take another call or to start another call
								Intent intentaudio = new Intent(CallIncomingActivity.this, ConnectActivity.class);
								intentaudio.putExtra("vid_or_aud", call_type);
								intentaudio.putExtra("user_id", user_id );
								intentaudio.putExtra("call_channel", call_server_id );
								startActivity(intentaudio);
								finish();


							}else{

							}


						}catch(Exception ex){
							Toasty.error(CallIncomingActivity.this, ex.toString() , Toast.LENGTH_LONG).show();
							ex.printStackTrace();
						}

					}
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					Toasty.error(CallIncomingActivity.this,databaseError.toString(), Toast.LENGTH_LONG).show();

				}
			});



		}catch(Exception ex){

			Call_dispo = true;
			ex.printStackTrace();
		}

		if (alreadyAcceptedOrDeniedCall) {
			return;
		}
		alreadyAcceptedOrDeniedCall = true;


	}

	public void Check_Correspondantavailibility(final Context context , String UserID){

		try{

			ViCall.stopRinging();

			userDB = FirebaseDatabase.getInstance().getReference().child("Users").child(UserID);
			// Set the  Driver Response to true ...
			//HashMap map = new HashMap();
			//map.put("Authentified" , "await");
			//userDB.updateChildren(map);
			userDB.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {
					if(dataSnapshot.exists()){
						try{
							Map<String, Object> map_call = (Map<String, Object>) dataSnapshot.getValue();
							// test if the recors Phone already exist  ...if not than
							// than you are a new user   ...
							if(map_call.get("call_possible")!=null){
								// than this user is already registered ...
								boolean availibilty  = (boolean) map_call.get("call_possible");
								if(availibilty = false) {
									// than we must stop the call  ...
									ViCall.stopRinging();
									// put the  call  dispo enable
									Call_dispo = true;
									finish();
								}

							}else{

							}


						}catch(Exception ex){
							Toasty.error(context, ex.toString() , Toast.LENGTH_LONG).show();
							ex.printStackTrace();
						}

					}
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					Toasty.error(context,databaseError.toString(), Toast.LENGTH_LONG).show();

				}
			});



		}catch(Exception ex){

			Call_dispo = true;
			ex.printStackTrace();
		}


	}



	private void caller_availability_chechker() {

		try{

			ViCall.stopRinging();

			userDB = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
			// Set the  Driver Response to true ...
			//HashMap map = new HashMap();
			//map.put("Authentified" , "await");
			//userDB.updateChildren(map);
			userDB.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {
					if(dataSnapshot.exists()){
						try{
							Map<String, Object> map_call = (Map<String, Object>) dataSnapshot.getValue();
							// test if the recors Phone already exist  ...if not than
							// than you are a new user   ...
							if(map_call.get("call_id")!=null){
								// than this user is already registered ...
								call_server_id = map_call.get("call_id").toString();
								// Toasty.info(mContext, "Server Channel  : "+ call_server_id, Toast.LENGTH_LONG).show();
								if(map_call.get("call_type")!=null){
									call_type = map_call.get("call_type").toString();
								}else{
									call_type = "audio";
								}

								finish();


							}else{

							}


						}catch(Exception ex){
							Toasty.error(CallIncomingActivity.this, ex.toString() , Toast.LENGTH_LONG).show();
							ex.printStackTrace();
						}

					}
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					Toasty.error(CallIncomingActivity.this,databaseError.toString(), Toast.LENGTH_LONG).show();

				}
			});



		}catch(Exception ex){

			Call_dispo = true;
			ex.printStackTrace();
		}

		if (alreadyAcceptedOrDeniedCall) {
			return;
		}
		alreadyAcceptedOrDeniedCall = true;


	}

	@Override
	public void onLeftHandleTriggered() {

	}

	@Override
	public void onRightHandleTriggered() {

	}
	
	private void checkAndRequestCallPermissions() {
		ArrayList<String> permissionsList = new ArrayList<String>();
		
		int recordAudio = getPackageManager().checkPermission(Manifest.permission.RECORD_AUDIO, getPackageName());
		//Log.i("[Permission] Record lin_audio permission is " + (recordAudio == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
		int camera = getPackageManager().checkPermission(Manifest.permission.CAMERA, getPackageName());
		// Log.i("[Permission] Camera permission is " + (camera == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
		

		
		if (permissionsList.size() > 0) {
			String[] permissions = new String[permissionsList.size()];
			permissions = permissionsList.toArray(permissions);
			ActivityCompat.requestPermissions(this, permissions, 0);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		for (int i = 0; i < permissions.length; i++) {
			Toasty.info(instance, "Permission ", Toast.LENGTH_SHORT).show();

		}
	}
}