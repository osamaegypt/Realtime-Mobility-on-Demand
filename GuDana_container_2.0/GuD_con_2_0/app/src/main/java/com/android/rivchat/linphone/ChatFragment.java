package com.android.rivchat.linphone;
/*
ChatFragment.java
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.rivchat.R;
import com.android.rivchat.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneBuffer;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatMessage.State;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.mediastream.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class ChatFragment extends Fragment implements OnClickListener, LinphoneChatMessage.LinphoneChatMessageListener {
	private static final int ADD_PHOTO = 1337;
	private static final int MENU_DELETE_MESSAGE = 0;
	private static final int MENU_PICTURE_SMALL = 2;
	private static final int MENU_PICTURE_MEDIUM = 3;
	private static final int MENU_PICTURE_LARGE = 4;
	private static final int MENU_PICTURE_REAL = 5;
	private static final int MENU_COPY_TEXT = 6;
	private static final int MENU_RESEND_MESSAGE = 7;
	private static final int SIZE_SMALL = 500;
	private static final int SIZE_MEDIUM = 1000;
	private static final int SIZE_LARGE = 1500;
	private static final int SIZE_MAX = 2048;

	private LinphoneChatRoom chatRoom;
	private String sipUri;
	private EditText message;
	private ImageView edit, selectAll, deselectAll, startCall, delete, sendImage, sendMessage, cancel;
	private TextView contactName, remoteComposing;
	private ImageView back, backToCall;
	private EditText searchContactField;
	private LinearLayout topBar, editList;
	private SearchContactsListAdapter searchAdapter;
	private ListView messagesList, resultContactsSearch;
	private LayoutInflater inflater;
	private Bitmap defaultBitmap;

	private boolean isEditMode = false;
	private LinphoneContact contact;
	private Uri imageToUploadUri;
	private String filePathToUpload;
	private TextWatcher textWatcher;
	private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;
	private ChatMessageAdapter adapter;

	private LinphoneCoreListenerBase mListener;
	private ByteArrayInputStream mUploadingImageStream;
	private boolean newChatConversation = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final View view = inflater.inflate(R.layout.lin_chat, container, false);

		LinphoneManager.addListener(this);
		// Retain the fragment across configuration changes
		setRetainInstance(true);

		this.inflater = inflater;

		if(getArguments() == null || getArguments().getString("SipUri") == null) {
			newChatConversation = true;
		} else {
			//Retrieve parameter from intent
			sipUri = getArguments().getString("SipUri");
		}

		//Initialize UI
		defaultBitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.chat_picture_over);

		contactName = (TextView) view.findViewById(R.id.contact_name);
		messagesList = (ListView) view.findViewById(R.id.chat_message_list);
		searchContactField = (EditText) view.findViewById(R.id.search_contact_field);
		resultContactsSearch = (ListView) view.findViewById(R.id.result_contacts);

		editList = (LinearLayout) view.findViewById(R.id.edit_list);
		topBar = (LinearLayout) view.findViewById(R.id.top_bar);

		sendMessage = (ImageView) view.findViewById(R.id.send_message);
		sendMessage.setOnClickListener(this);

		remoteComposing = (TextView) view.findViewById(R.id.remote_composing);
		remoteComposing.setVisibility(View.GONE);

		cancel = (ImageView) view.findViewById(R.id.cancel);
		cancel.setOnClickListener(this);

		edit = (ImageView) view.findViewById(R.id.edit);
		edit.setOnClickListener(this);

		startCall = (ImageView) view.findViewById(R.id.start_call);
		startCall.setOnClickListener(this);

		backToCall = (ImageView) view.findViewById(R.id.back_to_call);
		backToCall.setOnClickListener(this);

		selectAll = (ImageView) view.findViewById(R.id.select_all);
		selectAll.setOnClickListener(this);

		deselectAll = (ImageView) view.findViewById(R.id.deselect_all);
		deselectAll.setOnClickListener(this);

		delete = (ImageView) view.findViewById(R.id.delete);
		delete.setOnClickListener(this);

		if (newChatConversation) {
			initNewChatConversation();
		}

		message = (EditText) view.findViewById(R.id.message);

		sendImage = (ImageView) view.findViewById(R.id.send_picture);
		if (!getResources().getBoolean(R.bool.disable_chat_send_file)) {
			sendImage.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pickImage();
					LinphoneActivity.instance().checkAndRequestPermissionsToSendImage();
				}
			});
			//registerForContextMenu(sendImage);
		} else {
			sendImage.setEnabled(false);
		}

		back = (ImageView) view.findViewById(R.id.back);
		if(getResources().getBoolean(R.bool.isTablet)){
			back.setVisibility(View.INVISIBLE);
		} else {
			back.setOnClickListener(this);
		}

		mListener = new LinphoneCoreListenerBase(){
			@Override
			public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr, LinphoneChatMessage message) {
				LinphoneAddress from = cr.getPeerAddress();
				if (from.asStringUriOnly().equals(sipUri)) {
					LinphoneService.instance().removeMessageNotification();
					cr.markAsRead();
					LinphoneActivity.instance().updateMissedChatCount();
					adapter.addMessage(cr.getHistory(1)[0]);

					String externalBodyUrl = message.getExternalBodyUrl();
					LinphoneContent fileTransferContent = message.getFileTransferInformation();
					if (externalBodyUrl != null || fileTransferContent != null) {
						LinphoneActivity.instance().checkAndRequestExternalStoragePermission();
					}
				}
			}

			@Override
			public void isComposingReceived(LinphoneCore lc, LinphoneChatRoom room) {
				if (chatRoom != null && room != null && chatRoom.getPeerAddress().asStringUriOnly().equals(room.getPeerAddress().asStringUriOnly())) {
					remoteComposing.setVisibility(chatRoom.isRemoteComposing() ? View.VISIBLE : View.GONE);
				}
			}
		};

		textWatcher = new TextWatcher() {
			public void afterTextChanged(Editable arg0) {}

			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				if (message.getText().toString().equals("")) {
					sendMessage.setEnabled(false);
				} else {
					if (chatRoom != null)
						chatRoom.compose();
					sendMessage.setEnabled(true);
				}
			}
		};

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (message != null) {
			outState.putString("messageDraft", message.getText().toString());
		}
		if (contact != null) {
			outState.putSerializable("contactDraft",contact);
			outState.putString("sipUriDraft",sipUri);
		}
		super.onSaveInstanceState(outState);
	}

	private void addVirtualKeyboardVisiblityListener() {
		keyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
			Rect visibleArea = new Rect();
			getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleArea);

			int heightDiff = getActivity().getWindow().getDecorView().getRootView().getHeight() - (visibleArea.bottom - visibleArea.top);
				if (heightDiff > 200) {
					showKeyboardVisibleMode();
				} else {
					hideKeyboardVisibleMode();
				}
			}
		};
		getActivity().getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
	}

	private void removeVirtualKeyboardVisiblityListener() {
		Compatibility.removeGlobalLayoutListener(getActivity().getWindow().getDecorView().getViewTreeObserver(), keyboardListener);
	}

	public void showKeyboardVisibleMode() {
		boolean isOrientationLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		if (isOrientationLandscape && topBar != null) {
			//topBar.setVisibility(View.GONE);
		}
		LinphoneActivity.instance().hideTabBar(true);
		//contactPicture.setVisibility(View.GONE);
	}

	public void hideKeyboardVisibleMode() {
		boolean isOrientationLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		//contactPicture.setVisibility(View.VISIBLE);
		if (isOrientationLandscape && topBar != null) {
			//topBar.setVisibility(View.VISIBLE);
		}
		LinphoneActivity.instance().hideTabBar(false);
	}

	public int getNbItemsChecked(){
		int size = messagesList.getAdapter().getCount();
		int nb = 0;
		for(int i=0; i<size; i++) {
			if(messagesList.isItemChecked(i)) {
				nb ++;
			}
		}
		return nb;
	}

	public void enabledDeleteButton(Boolean enabled){
		if(enabled){
			delete.setEnabled(true);
		} else {
			if (getNbItemsChecked() == 0){
				delete.setEnabled(false);
			}
		}
	}

	public void initChatRoom(String sipUri) {
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();

		LinphoneAddress lAddress = null;
		if (sipUri == null) {
			contact = null; // Tablet rotation issue
			initNewChatConversation();
		} else {
			try {
				lAddress = lc.interpretUrl(sipUri);
			} catch (Exception e) {
				//TODO Error popup and quit chat
			}

			if (lAddress != null) {
				chatRoom = lc.getChatRoom(lAddress);
				chatRoom.markAsRead();
				LinphoneActivity.instance().updateMissedChatCount();
				contact = ContactsManager.getInstance().findContactFromAddress(lAddress);
				if (chatRoom != null) {
					searchContactField.setVisibility(View.GONE);
					resultContactsSearch.setVisibility(View.GONE);
					displayChatHeader(lAddress);
					displayMessageList();
				}
			}
		}
	}

	private void redrawMessageList() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	private void displayMessageList() {
		if (chatRoom != null) {
			if (adapter != null) {
				adapter.refreshHistory();
			} else {
				adapter = new ChatMessageAdapter(getActivity());
			}
		}
		messagesList.setAdapter(adapter);
		messagesList.setVisibility(ListView.VISIBLE);
	}

	private void displayChatHeader(LinphoneAddress address) {
		if (contact != null || address != null) {
			if (contact != null) {
				contactName.setText(contact.getFullName());
			} else {
				contactName.setText(LinphoneUtils.getAddressDisplayName(address));
			}
			topBar.setVisibility(View.VISIBLE);
			edit.setVisibility(View.VISIBLE);
			contactName.setVisibility(View.VISIBLE);
		}
	}

	public void changeDisplayedChat(String newSipUri, String displayName, String pictureUri) {
		this.sipUri = newSipUri;
		initChatRoom(sipUri);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.send_picture) {
			menu.add(0, MENU_PICTURE_SMALL, 0, getString(R.string.share_picture_size_small));
			menu.add(0, MENU_PICTURE_MEDIUM, 0, getString(R.string.share_picture_size_medium));
			menu.add(0, MENU_PICTURE_LARGE, 0, getString(R.string.share_picture_size_large));
			//			Not a good idea, very big pictures cause Out of Memory exceptions, slow display, ...
			//			menu.add(0, MENU_PICTURE_REAL, 0, getString(R.string.share_picture_size_real));
		} else {
			menu.add(v.getId(), MENU_DELETE_MESSAGE, 0, getString(R.string.delete));
			menu.add(v.getId(), MENU_COPY_TEXT, 0, getString(R.string.copy_text));
		}

		LinphoneChatMessage msg = getMessageForId(v.getId());
		if (msg != null && msg.getStatus() == LinphoneChatMessage.State.NotDelivered) {
			menu.add(v.getId(), MENU_RESEND_MESSAGE, 0, getString(R.string.retry));
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_DELETE_MESSAGE:
				if (chatRoom != null) {
					LinphoneChatMessage message = getMessageForId(item.getGroupId());
					if (message != null) {
						chatRoom.deleteMessage(message);
						invalidate();
					}
				}
				break;
			case MENU_COPY_TEXT:
				copyTextMessageToClipboard(item.getGroupId());
				break;
			case MENU_RESEND_MESSAGE:
				resendMessage(item.getGroupId());
				break;
			case MENU_PICTURE_SMALL:
				sendImageMessage(filePathToUpload, SIZE_SMALL);
				break;
			case MENU_PICTURE_MEDIUM:
				sendImageMessage(filePathToUpload, SIZE_MEDIUM);
				break;
			case MENU_PICTURE_LARGE:
				sendImageMessage(filePathToUpload, SIZE_LARGE);
				break;
			case MENU_PICTURE_REAL:
				sendImageMessage(filePathToUpload, SIZE_MAX);
				break;
		}
		return true;
	}

	@Override
	public void onPause() {
		message.removeTextChangedListener(textWatcher);
		removeVirtualKeyboardVisiblityListener();

		LinphoneService.instance().removeMessageNotification();

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			lc.removeListener(mListener);
		}

		LinphoneManager.removeListener(this);
		onSaveInstanceState(getArguments());

		//Hide keybord
		InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(message.getWindowToken(), 0);

		super.onPause();
	}

	@Override
	public void onDestroy() {
		if (adapter != null) {
			adapter.destroy();
		}
		if (defaultBitmap != null) {
			defaultBitmap.recycle();
			defaultBitmap = null;
		}
		super.onDestroy();
	}

	@SuppressLint("UseSparseArrays")
	@Override
	public void onResume() {
		super.onResume();

		message.addTextChangedListener(textWatcher);
		addVirtualKeyboardVisiblityListener();

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			lc.addListener(mListener);
		}

		if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().selectMenu(FragmentsAvailable.CHAT);
		}

		if(LinphoneManager.getLc().isIncall()){
			backToCall.setVisibility(View.VISIBLE);
			startCall.setVisibility(View.GONE);
		} else {
			if(!newChatConversation) {
				backToCall.setVisibility(View.GONE);
				startCall.setVisibility(View.VISIBLE);
			}
		}

		LinphoneManager.addListener(this);

		// Force hide keyboard
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

		String draft = getArguments().getString("messageDraft");
		message.setText(draft);
		contact = (LinphoneContact)getArguments().getSerializable("contactDraft");
		if (contact != null) {
			contactName.setText(contact.getFullName());
			sipUri = getArguments().getString("sipUriDraft");
			getArguments().clear();
		}

		if (!newChatConversation || contact != null) {
			initChatRoom(sipUri);
			searchContactField.setVisibility(View.GONE);
			resultContactsSearch.setVisibility(View.GONE);
			remoteComposing.setVisibility(chatRoom.isRemoteComposing() ? View.VISIBLE : View.GONE);
		}
	}

	private void selectAllList(boolean isSelectAll) {
		int size = messagesList.getAdapter().getCount();
		for (int i = 0; i < size; i++) {
			messagesList.setItemChecked(i, isSelectAll);
		}
	}

	public void quitEditMode(){
		isEditMode = false;
		editList.setVisibility(View.GONE);
		topBar.setVisibility(View.VISIBLE);
		redrawMessageList();
	}

	private void removeChats(){
		int size = messagesList.getAdapter().getCount();
		for (int i = 0; i < size; i++) {
			if (messagesList.isItemChecked(i)) {
				LinphoneChatMessage message = (LinphoneChatMessage) messagesList.getAdapter().getItem(i);
				chatRoom.deleteMessage(message);
			}
		}
		invalidate();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();

		if (id == R.id.back_to_call) {
			LinphoneActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
			return;
		}
		if (id == R.id.select_all) {
			deselectAll.setVisibility(View.VISIBLE);
			selectAll.setVisibility(View.GONE);
			enabledDeleteButton(true);
			selectAllList(true);
			return;
		}
		if (id == R.id.deselect_all) {
			deselectAll.setVisibility(View.GONE);
			selectAll.setVisibility(View.VISIBLE);
			enabledDeleteButton(false);
			selectAllList(false);
			return;
		}
		if (id == R.id.cancel) {
			quitEditMode();
			return;
		}
		if (id == R.id.delete) {
			final Dialog dialog = LinphoneActivity.instance().displayDialog(getString(R.string.delete_text));
			Button delete = (Button) dialog.findViewById(R.id.delete_button);
			Button cancel = (Button) dialog.findViewById(R.id.cancel);

			delete.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					removeChats();
					dialog.dismiss();
					quitEditMode();
				}
			});

			cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					dialog.dismiss();
					quitEditMode();
				}
			});
			dialog.show();
			return;
		}
		if(id == R.id.send_message){
			sendTextMessage();
		}
		if (id == R.id.edit) {
			topBar.setVisibility(View.INVISIBLE);
			editList.setVisibility(View.VISIBLE);
			isEditMode = true;
			redrawMessageList();
		}
		if(id == R.id.start_call){
			LinphoneActivity.instance().setAddresGoToDialerAndCall(sipUri, LinphoneUtils.getUsernameFromAddress(sipUri), null);
		}
		if (id == R.id.back) {
			getFragmentManager().popBackStackImmediate();
		}
	}

	private void sendTextMessage() {
		sendTextMessage(message.getText().toString());
		message.setText("");
	}

	private void sendTextMessage(String messageToSend) {
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		boolean isNetworkReachable = lc == null ? false : lc.isNetworkReachable();
		LinphoneAddress lAddress = null;

		//Start new conversation in fast chat
		if(newChatConversation && chatRoom == null) {
			String address = searchContactField.getText().toString().toLowerCase(Locale.getDefault());
			if (address != null && !address.equals("")) {
				initChatRoom(address);
			}
		}
		if (chatRoom != null && messageToSend != null && messageToSend.length() > 0 && isNetworkReachable) {
			LinphoneChatMessage message = chatRoom.createLinphoneChatMessage(messageToSend);
			chatRoom.sendChatMessage(message);
			lAddress = chatRoom.getPeerAddress();

			if (LinphoneActivity.isInstanciated()) {
				LinphoneActivity.instance().onMessageSent(sipUri, messageToSend);
			}

			message.setListener(LinphoneManager.getInstance());
			if (newChatConversation) {
				exitNewConversationMode(lAddress.asStringUriOnly());
			} else {
				adapter.addMessage(message);
			}

			Log.i("Sent message current lin_status: " + message.getStatus());
		} else if (!isNetworkReachable && LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().displayCustomToast(getString(R.string.error_network_unreachable), Toast.LENGTH_LONG);
		}
	}

	private void sendImageMessage(String path, int imageSize) {
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		boolean isNetworkReachable = lc == null ? false : lc.isNetworkReachable();

		if(newChatConversation && chatRoom == null) {
			String address = searchContactField.getText().toString();
			if (address != null && !address.equals("")) {
				initChatRoom(address);
			}
		}

		if (chatRoom != null && path != null && path.length() > 0 && isNetworkReachable) {
			try {
				Bitmap bm = BitmapFactory.decodeFile(path);
				if (bm != null) {
					FileUploadPrepareTask task = new FileUploadPrepareTask(getActivity(), path, imageSize);
					task.execute(bm);
				} else {
					Log.e("Error, bitmap factory can't read " + path);
				}
			} catch (RuntimeException e) {
				Log.e("Error, not enough memory to create the bitmap");
			}
		} else if (!isNetworkReachable && LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().displayCustomToast(getString(R.string.error_network_unreachable), Toast.LENGTH_LONG);
		}
	}

	private LinphoneChatMessage getMessageForId(int id) {
		if (adapter == null) return null;
		for (int i = 0; i < adapter.getCount(); i++) {
			LinphoneChatMessage message = adapter.getItem(i);
			if (message.getStorageId() == id) {
				return message;
			}
		}
		return null;
	}

	private void invalidate() {
		adapter.refreshHistory();
		chatRoom.markAsRead();
	}

	private void resendMessage(int id) {
		LinphoneChatMessage message = getMessageForId(id);
		if (message == null)
			return;

		chatRoom.deleteMessage(getMessageForId(id));
		invalidate();

		if (message.getText() != null && message.getText().length() > 0) {
			sendTextMessage(message.getText());
		} else {
			sendImageMessage(message.getAppData(), 0);
		}
	}

	private void copyTextMessageToClipboard(int id) {
		LinphoneChatMessage message = null;
		for (int i = 0; i < adapter.getCount(); i++) {
			LinphoneChatMessage msg = adapter.getItem(i);
			if (msg.getStorageId() == id) {
				message = msg;
				break;
			}
		}

		String txt = null;
		if (message != null) {
			txt = message.getText();
		}
		if (txt != null) {
			ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		    ClipData clip = ClipData.newPlainText("Message", txt);
		    clipboard.setPrimaryClip(clip);
			LinphoneActivity.instance().displayCustomToast(getString(R.string.text_copied_to_clipboard), Toast.LENGTH_SHORT);
		}
	}

	//File transfer
	private void pickImage() {
		List<Intent> cameraIntents = new ArrayList<Intent>();
		Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		File file = new File(Environment.getExternalStorageDirectory(), getString(R.string.temp_photo_name_with_date).replace("%s", String.valueOf(System.currentTimeMillis())));
		imageToUploadUri = Uri.fromFile(file);
		captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageToUploadUri);
		cameraIntents.add(captureIntent);

		Intent galleryIntent = new Intent();
		galleryIntent.setType("image/*");
		galleryIntent.setAction(Intent.ACTION_PICK);

		Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.image_picker_title));
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

		startActivityForResult(chooserIntent, ADD_PHOTO);
	}

	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = {MediaStore.Images.Media.DATA};
		CursorLoader loader = new CursorLoader(getActivity(), contentUri, proj, null, null, null);
		Cursor cursor = loader.loadInBackground();
		if (cursor != null && cursor.moveToFirst()) {
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			String result = cursor.getString(column_index);
			cursor.close();
			return result;
		}
		return null;
	}

	class FileUploadPrepareTask extends AsyncTask<Bitmap, Void, byte[]> {
		private String path;
		private ProgressDialog progressDialog;

		public FileUploadPrepareTask(Context context, String fileToUploadPath, int size) {
			path = fileToUploadPath;

			progressDialog = new ProgressDialog(context);
			progressDialog.setIndeterminate(true);
			progressDialog.setMessage(getString(R.string.processing_image));
			progressDialog.show();
		}

		@Override
		protected byte[] doInBackground(Bitmap... params) {
			Bitmap bm = params[0];

			if (bm.getWidth() >= bm.getHeight() && bm.getWidth() > SIZE_MAX) {
				bm = Bitmap.createScaledBitmap(bm, SIZE_MAX, (SIZE_MAX * bm.getHeight()) / bm.getWidth(), false);
			} else if (bm.getHeight() >= bm.getWidth() && bm.getHeight() > SIZE_MAX) {
				bm = Bitmap.createScaledBitmap(bm, (SIZE_MAX * bm.getWidth()) / bm.getHeight(), SIZE_MAX, false);
			}

			// Rotate the bitmap if possible/needed, using EXIF data
			try {
				if (path != null) {
					ExifInterface exif = new ExifInterface(path);
					int pictureOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
					Matrix matrix = new Matrix();
					if (pictureOrientation == 6) {
						matrix.postRotate(90);
					} else if (pictureOrientation == 3) {
						matrix.postRotate(180);
					} else if (pictureOrientation == 8) {
						matrix.postRotate(270);
					}
					bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
				}
			} catch (Exception e) {
				Log.e(e);
			}

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			String extension = LinphoneUtils.getExtensionFromFileName(path);
			if (extension != null && extension.toLowerCase(Locale.getDefault()).equals("png")) {
				bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
			} else {
				bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
			}
			byte[] byteArray = stream.toByteArray();
			return byteArray;
		}

		@Override
		protected void onPostExecute(byte[] result) {
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
			mUploadingImageStream = new ByteArrayInputStream(result);

			String fileName = path.substring(path.lastIndexOf("/") + 1);
			String extension = LinphoneUtils.getExtensionFromFileName(fileName);
			LinphoneContent content = LinphoneCoreFactory.instance().createLinphoneContent("image", extension, result, null);
			content.setName(fileName);

			LinphoneChatMessage message = chatRoom.createFileTransferMessage(content);
			message.setListener(LinphoneManager.getInstance());
			message.setAppData(path);

			LinphoneManager.getInstance().setUploadPendingFileMessage(message);
			LinphoneManager.getInstance().setUploadingImageStream(mUploadingImageStream);

			chatRoom.sendChatMessage(message);
			adapter.addMessage(message);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ADD_PHOTO && resultCode == Activity.RESULT_OK) {
			String fileToUploadPath = null;

			if (data != null && data.getData() != null) {
				fileToUploadPath = getRealPathFromURI(data.getData());
			} else if (imageToUploadUri != null) {
				fileToUploadPath = imageToUploadUri.getPath();
			}

			if (fileToUploadPath != null) {
				//showPopupMenuAskingImageSize(fileToUploadPath);
				sendImageMessage(fileToUploadPath,0);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	//New conversation
	private void exitNewConversationMode(String address) {
		sipUri = address;
		searchContactField.setVisibility(View.GONE);
		resultContactsSearch.setVisibility(View.GONE);
		messagesList.setVisibility(View.VISIBLE);
		contactName.setVisibility(View.VISIBLE);
		edit.setVisibility(View.VISIBLE);
		startCall.setVisibility(View.VISIBLE);

		if(getResources().getBoolean(R.bool.isTablet)){
			back.setVisibility(View.INVISIBLE);
		} else {
			back.setOnClickListener(this);
		}

		newChatConversation = false;
		initChatRoom(sipUri);
	}

	private void initNewChatConversation(){
		messagesList.setVisibility(View.GONE);
		edit.setVisibility(View.INVISIBLE);
		startCall.setVisibility(View.INVISIBLE);
		contactName.setVisibility(View.INVISIBLE);
		resultContactsSearch.setVisibility(View.VISIBLE);
		searchAdapter = new SearchContactsListAdapter(null);
		resultContactsSearch.setAdapter(searchAdapter);
		searchContactField.setVisibility(View.VISIBLE);
		searchContactField.requestFocus();
		searchContactField.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				searchContacts(searchContactField.getText().toString());
			}
		});
	}

	private class ContactAddress {
		LinphoneContact contact;
		String address;

		private ContactAddress(LinphoneContact c, String a){
			this.contact = c;
			this.address = a;
		}
	}

	private void searchContacts(String search) {
		if (search == null || search.length() == 0) {
			resultContactsSearch.setAdapter(new SearchContactsListAdapter(null));
			return;
		}

		List<ContactAddress> result = new ArrayList<ContactAddress>();
		if(search != null) {
			for (ContactAddress c : searchAdapter.contacts) {
				String address = c.address;
				if (address.startsWith("sip:")) address = address.substring(4);
				if (c.contact.getFullName() != null && c.contact.getFullName().toLowerCase(Locale.getDefault()).startsWith(search.toLowerCase(Locale.getDefault()))
						|| address.toLowerCase(Locale.getDefault()).startsWith(search.toLowerCase(Locale.getDefault()))) {
					result.add(c);
				}
			}
		}

		resultContactsSearch.setAdapter(new SearchContactsListAdapter(result));
		searchAdapter.notifyDataSetChanged();
	}

	class ChatMessageAdapter extends BaseAdapter {
		private class ViewHolder implements LinphoneChatMessage.LinphoneChatMessageListener {
			public int id;
			public RelativeLayout bubbleLayout;
			public CheckBox delete;
			public LinearLayout background;
			public ImageView contactPicture;
			public TextView contactName;
			public TextView messageText;
			public ImageView messageImage;
			public RelativeLayout fileTransferLayout;
			public ProgressBar fileTransferProgressBar;
			public Button fileTransferAction;
			public ImageView messageStatus;
			public ProgressBar messageSendingInProgress;
			public ImageView contactPictureMask;

			public ViewHolder(View view) {
				id = view.getId();
				bubbleLayout = (RelativeLayout) view.findViewById(R.id.bubble);
				delete = (CheckBox) view.findViewById(R.id.delete_message);
				background = (LinearLayout) view.findViewById(R.id.background);
				contactPicture = (ImageView) view.findViewById(R.id.contact_picture);
				contactName = (TextView) view.findViewById(R.id.contact_header);
				messageText = (TextView) view.findViewById(R.id.message);
				messageImage = (ImageView) view.findViewById(R.id.image);
				fileTransferLayout = (RelativeLayout) view.findViewById(R.id.file_transfer_layout);
				fileTransferProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
				fileTransferAction = (Button) view.findViewById(R.id.file_transfer_action);
				messageStatus = (ImageView) view.findViewById(R.id.status);
				messageSendingInProgress = (ProgressBar) view.findViewById(R.id.inprogress);
				contactPictureMask = (ImageView) view.findViewById(R.id.mask);
			}

			@Override
			public void onLinphoneChatMessageStateChanged(LinphoneChatMessage msg, State state) {

			}

			@Override
			public void onLinphoneChatMessageFileTransferReceived(LinphoneChatMessage msg, LinphoneContent content, LinphoneBuffer buffer) {

			}

			@Override
			public void onLinphoneChatMessageFileTransferSent(LinphoneChatMessage msg, LinphoneContent content, int offset, int size, LinphoneBuffer bufferToFill) {

			}

			@Override
			public void onLinphoneChatMessageFileTransferProgressChanged(LinphoneChatMessage msg, LinphoneContent content, int offset, int total) {
				if (msg.getStorageId() == id) fileTransferProgressBar.setProgress(offset * 100 / total);
			}
		}

		ArrayList<LinphoneChatMessage> history;
		Context context;

		public ChatMessageAdapter(Context c) {
			context = c;
			history = new ArrayList<LinphoneChatMessage>();
			refreshHistory();
		}

		public void destroy() {
			if (history != null) {
				history.clear();
			}
		}

		public void refreshHistory() {
			history.clear();
			LinphoneChatMessage[] messages = chatRoom.getHistory();
			history.addAll(Arrays.asList(messages));
			notifyDataSetChanged();
		}

		public void addMessage(LinphoneChatMessage message) {
			history.add(message);
			notifyDataSetChanged();
			messagesList.setSelection(getCount() - 1);
		}

		@Override
		public int getCount() {
			return history.size();
		}

		@Override
		public LinphoneChatMessage getItem(int position) {
			return history.get(position);
		}

		@Override
		public long getItemId(int position) {
			return history.get(position).getStorageId();
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final LinphoneChatMessage message = history.get(position);
			View view = null;
			final ViewHolder holder;
			boolean sameMessage = false;

			if (convertView != null) {
				view = convertView;
				holder = (ViewHolder) view.getTag();
				LinphoneManager.removeListener(holder);
			} else {
				view = LayoutInflater.from(context).inflate(R.layout.lin_chat_bubble, null);
				holder = new ViewHolder(view);
				view.setTag(holder);
			}

			if (holder.id == message.getStorageId()) {
				// Horrible workaround to not reload image on edit chat list
				if (holder.messageImage.getTag() != null
						&& (holder.messageImage.getTag().equals(message.getAppData())
							|| ((String) holder.messageImage.getTag()).substring(7).equals(message.getAppData()))
						){
					sameMessage = true;
				}
			} else {
				holder.id = message.getStorageId();
			}
			view.setId(holder.id);
			registerForContextMenu(view);

			LinphoneChatMessage.State status = message.getStatus();
			String externalBodyUrl = message.getExternalBodyUrl();
			LinphoneContent fileTransferContent = message.getFileTransferInformation();

			holder.delete.setVisibility(View.GONE);
			holder.messageText.setVisibility(View.GONE);
			holder.messageImage.setVisibility(View.GONE);
			holder.fileTransferLayout.setVisibility(View.GONE);
			holder.fileTransferProgressBar.setProgress(0);
			holder.fileTransferAction.setEnabled(true);
			holder.messageStatus.setVisibility(View.INVISIBLE);
			holder.messageSendingInProgress.setVisibility(View.GONE);

			String displayName = message.getFrom().getDisplayName();
			if (displayName == null) {
				displayName = message.getFrom().getUserName();
			}
			if (!message.isOutgoing()) {
				if (contact != null) {
					if (contact != null && contact.getFullName() != null) {
						displayName = contact.getFullName();
					}
					if (contact.hasPhoto()) {
						Bitmap photo = contact.getPhoto();
						if (photo != null) {
							holder.contactPicture.setImageBitmap(photo);
						} else {
							LinphoneUtils.setImagePictureFromUri(getActivity(), holder.contactPicture, contact.getPhotoUri(), contact.getThumbnailUri());
						}
					} else {
						holder.contactPicture.setImageResource(R.drawable.avatar);
					}
				} else {
					holder.contactPicture.setImageResource(R.drawable.avatar);
				}
			} else {
				holder.contactPicture.setImageResource(R.drawable.avatar);
			}
			holder.contactName.setText(timestampToHumanDate(context, message.getTime()) + " - " + displayName);

			if (status == LinphoneChatMessage.State.NotDelivered) {
				holder.messageStatus.setVisibility(View.VISIBLE);
				holder.messageStatus.setImageResource(R.drawable.chat_message_not_delivered);
			} else if (status == LinphoneChatMessage.State.InProgress) {
				holder.messageSendingInProgress.setVisibility(View.VISIBLE);
			}

			if (externalBodyUrl != null || fileTransferContent != null) {
				String appData = message.getAppData();

				if (message.isOutgoing() && appData != null) {
					holder.messageImage.setVisibility(View.VISIBLE);
					if (!sameMessage) {
						loadBitmap(message.getAppData(), holder.messageImage);
						holder.messageImage.setTag(message.getAppData());
					}

					if (LinphoneManager.getInstance().getMessageUploadPending() != null  && LinphoneManager.getInstance().getMessageUploadPending().getStorageId() == message.getStorageId()) {
						holder.messageSendingInProgress.setVisibility(View.GONE);
						holder.fileTransferLayout.setVisibility(View.VISIBLE);
						LinphoneManager.addListener(holder);
					}
				} else {
					if (appData != null && !LinphoneManager.getInstance().isMessagePending(message) && appData.contains(context.getString(R.string.temp_photo_name_with_date).split("%s")[0])) {
						appData = null;
					}

					if (appData == null) {
						LinphoneManager.addListener(holder);
						holder.fileTransferLayout.setVisibility(View.VISIBLE);
					} else {
						if (LinphoneManager.getInstance().isMessagePending(message)) {
							LinphoneManager.addListener(holder);
							holder.fileTransferAction.setEnabled(false);
							holder.fileTransferLayout.setVisibility(View.VISIBLE);
						} else {
							LinphoneManager.removeListener(holder);
							holder.fileTransferLayout.setVisibility(View.GONE);
							holder.messageImage.setVisibility(View.VISIBLE);
							if (!sameMessage) {
								loadBitmap(appData, holder.messageImage);
								holder.messageImage.setTag(message.getAppData());
							}
						}
					}
				}
			} else {
				Spanned text = null;
				String msg = message.getText();
				if (msg != null) {
					text = getTextWithHttpLinks(msg);
					holder.messageText.setText(text);
					holder.messageText.setMovementMethod(LinkMovementMethod.getInstance());
					holder.messageText.setVisibility(View.VISIBLE);
				}
			}

			if (message.isOutgoing()) {
				holder.fileTransferAction.setText(getString(R.string.cancel));
				holder.fileTransferAction.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (LinphoneManager.getInstance().getMessageUploadPending() != null) {
							holder.fileTransferProgressBar.setVisibility(View.GONE);
							holder.fileTransferProgressBar.setProgress(0);
							message.cancelFileTransfer();
							LinphoneManager.getInstance().setUploadPendingFileMessage(null);
						}
					}
				});
			} else {
				holder.fileTransferAction.setText(getString(R.string.accept));
				holder.fileTransferAction.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (context.getPackageManager().checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
							v.setEnabled(false);
							String extension = message.getFileTransferInformation().getSubtype();
							String filename = context.getString(R.string.temp_photo_name_with_date).replace("%s", String.valueOf(System.currentTimeMillis())) + "." + extension;
							File file = new File(Environment.getExternalStorageDirectory(), filename);
							message.setAppData(filename);
							LinphoneManager.getInstance().addDownloadMessagePending(message);
							message.setListener(LinphoneManager.getInstance());
							message.setFileTransferFilepath(file.getPath());
							message.downloadFile();
						} else {
							Log.w("WRITE_EXTERNAL_STORAGE permission not granted, won't be able to store the downloaded file");
							LinphoneActivity.instance().checkAndRequestExternalStoragePermission();
						}
					}
				});
			}

			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			if (message.isOutgoing()) {
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				layoutParams.setMargins(100, 10, 10, 10);
				holder.background.setBackgroundResource(R.drawable.resizable_chat_bubble_outgoing);
				Compatibility.setTextAppearance(holder.contactName, getActivity(), R.style.font3);
				Compatibility.setTextAppearance(holder.fileTransferAction, getActivity(), R.style.font15);
				holder.fileTransferAction.setBackgroundResource(R.drawable.resizable_confirm_delete_button);
				holder.contactPictureMask.setImageResource(R.drawable.avatar_chat_mask_outgoing);
			} else {
				if (isEditMode) {
					layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					layoutParams.setMargins(100, 10, 10, 10);
				} else {
					layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					layoutParams.setMargins(10, 10, 100, 10);
				}
				holder.background.setBackgroundResource(R.drawable.resizable_chat_bubble_incoming);
				Compatibility.setTextAppearance(holder.contactName, getActivity(), R.style.font9);
				Compatibility.setTextAppearance(holder.fileTransferAction, getActivity(), R.style.font8);
				holder.fileTransferAction.setBackgroundResource(R.drawable.resizable_assistant_button);
				holder.contactPictureMask.setImageResource(R.drawable.avatar_chat_mask);
			}
			holder.bubbleLayout.setLayoutParams(layoutParams);

			if (isEditMode) {
				holder.delete.setVisibility(View.VISIBLE);
				holder.delete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
						messagesList.setItemChecked(position, b);
						if (getNbItemsChecked() == getCount()) {
							deselectAll.setVisibility(View.VISIBLE);
							selectAll.setVisibility(View.GONE);
							enabledDeleteButton(true);
						} else {
							if (getNbItemsChecked() == 0) {
								deselectAll.setVisibility(View.GONE);
								selectAll.setVisibility(View.VISIBLE);
								enabledDeleteButton(false);
							} else {
								deselectAll.setVisibility(View.GONE);
								selectAll.setVisibility(View.VISIBLE);
								enabledDeleteButton(true);
							}
						}
					}
				});

				if (messagesList.isItemChecked(position)) {
					holder.delete.setChecked(true);
				} else {
					holder.delete.setChecked(false);
				}
			}

			return view;
		}

		private String timestampToHumanDate(Context context, long timestamp) {
			try {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(timestamp);

				SimpleDateFormat dateFormat;
				if (isToday(cal)) {
					dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.today_date_format));
				} else {
					dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.messages_date_format));
				}

				return dateFormat.format(cal.getTime());
			} catch (NumberFormatException nfe) {
				return String.valueOf(timestamp);
			}
		}

		private boolean isToday(Calendar cal) {
			return isSameDay(cal, Calendar.getInstance());
		}

		private boolean isSameDay(Calendar cal1, Calendar cal2) {
			if (cal1 == null || cal2 == null) {
				return false;
			}

			return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
					cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
					cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
		}

		private Spanned getTextWithHttpLinks(String text) {
			if (text.contains("<")) {
				text = text.replace("<", "&lt;");
			}
			if (text.contains(">")) {
				text = text.replace(">", "&gt;");
			}
			if (text.contains("http://")) {
				int indexHttp = text.indexOf("http://");
				int indexFinHttp = text.indexOf(" ", indexHttp) == -1 ? text.length() : text.indexOf(" ", indexHttp);
				String link = text.substring(indexHttp, indexFinHttp);
				String linkWithoutScheme = link.replace("http://", "");
				text = text.replaceFirst(link, "<a href=\"" + link + "\">" + linkWithoutScheme + "</a>");
			}
			if (text.contains("https://")) {
				int indexHttp = text.indexOf("https://");
				int indexFinHttp = text.indexOf(" ", indexHttp) == -1 ? text.length() : text.indexOf(" ", indexHttp);
				String link = text.substring(indexHttp, indexFinHttp);
				String linkWithoutScheme = link.replace("https://", "");
				text = text.replaceFirst(link, "<a href=\"" + link + "\">" + linkWithoutScheme + "</a>");
			}

			return Compatibility.fromHtml(text);
		}

		public void loadBitmap(String path, ImageView imageView) {
			if (cancelPotentialWork(path, imageView)) {
				BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				final AsyncBitmap asyncBitmap = new AsyncBitmap(context.getResources(), defaultBitmap, task);
				imageView.setImageDrawable(asyncBitmap);
				task.execute(path);
			}
		}

		private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
			private final WeakReference<ImageView> imageViewReference;
			public String path;

			public BitmapWorkerTask(ImageView imageView) {
				path = null;
				// Use a WeakReference to ensure the ImageView can be garbage collected
				imageViewReference = new WeakReference<ImageView>(imageView);
			}

			// Decode image in background.
			@Override
			protected Bitmap doInBackground(String... params) {
				path = params[0];
				Bitmap bm = null;

				if (path.startsWith("content")) {
					try {
						bm = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse(path));
					} catch (FileNotFoundException e) {
						Log.e(e);
					} catch (IOException e) {
						Log.e(e);
					}
				} else {
					bm = BitmapFactory.decodeFile(path);
					path = "file://" + path;
				}

				if (bm != null) {
					bm = ThumbnailUtils.extractThumbnail(bm, SIZE_MAX, SIZE_MAX);
				}
				return bm;
			}

			// Once complete, see if ImageView is still around and set bitmap.
			@Override
			protected void onPostExecute(Bitmap bitmap) {
				if (isCancelled()) {
					bitmap = null;
				}

				if (imageViewReference != null && bitmap != null) {
					final ImageView imageView = imageViewReference.get();
					final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
					if (this == bitmapWorkerTask && imageView != null) {
						imageView.setImageBitmap(bitmap);
						imageView.setTag(path);
						imageView.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setDataAndType(Uri.parse((String)v.getTag()), "image/*");
								context.startActivity(intent);
							}
						});
					}
				}
			}
		}

		class AsyncBitmap extends BitmapDrawable {
			private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

			public AsyncBitmap(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
				super(res, bitmap);
				bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
			}

			public BitmapWorkerTask getBitmapWorkerTask() {
				return bitmapWorkerTaskReference.get();
			}
		}

		private boolean cancelPotentialWork(String path, ImageView imageView) {
			final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

			if (bitmapWorkerTask != null) {
				final String bitmapData = bitmapWorkerTask.path;
				// If bitmapData is not yet set or it differs from the new data
				if (bitmapData == null || bitmapData != path) {
					// Cancel previous task
					bitmapWorkerTask.cancel(true);
				} else {
					// The same work is already in progress
					return false;
				}
			}
			// No task associated with the ImageView, or an existing task was cancelled
			return true;
		}

		private BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
			if (imageView != null) {
				final Drawable drawable = imageView.getDrawable();
				if (drawable instanceof AsyncBitmap) {
					final AsyncBitmap asyncDrawable = (AsyncBitmap) drawable;
					return asyncDrawable.getBitmapWorkerTask();
				}
			}
			return null;
		}
	}

	class SearchContactsListAdapter extends BaseAdapter {
		private class ViewHolder {
			public TextView name;
			public TextView address;

			public ViewHolder(View view) {
				name = (TextView) view.findViewById(R.id.contact_name);
				address = (TextView) view.findViewById(R.id.contact_address);
			}
		}

		private List<ContactAddress> contacts;
		private LayoutInflater mInflater;

		SearchContactsListAdapter(List<ContactAddress> contactsList) {
			mInflater = inflater;
			if (contactsList == null) {
				contacts = getContactsList();
			} else {
				contacts = contactsList;
			}
		}

		public List<ContactAddress> getContactsList() {
			List<ContactAddress> list = new ArrayList<ContactAddress>();
			if(ContactsManager.getInstance().hasContacts()) {
				for (LinphoneContact con : ContactsManager.getInstance().getContacts()) {
					for (LinphoneNumberOrAddress noa : con.getNumbersOrAddresses()) {
						String value = noa.getValue();
						// Fix for sip:username compatibility issue
						if (value.startsWith("sip:") && !value.contains("@")) {
							value = value.substring(4);
							value = LinphoneUtils.getFullAddressFromUsername(value);
						}
						list.add(new ContactAddress(con, value));
					}
				}
			}
			return list;
		}

		public int getCount() {
			return contacts.size();
		}

		public ContactAddress getItem(int position) {
			if (contacts == null || position >= contacts.size()) {
				contacts = getContactsList();
				return contacts.get(position);
			} else {
				return contacts.get(position);
			}
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = null;
			ContactAddress contact;
			ViewHolder holder = null;

			do {
				contact = getItem(position);
			} while (contact == null);

			if (convertView != null) {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			} else {
				view = mInflater.inflate(R.layout.search_contact_cell, parent, false);
				holder = new ViewHolder(view);
				view.setTag(holder);
			}

			final String a = contact.address;
			LinphoneContact c = contact.contact;

			holder.name.setText(c.getFullName());
			holder.address.setText(a);

			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					exitNewConversationMode(a);
				}
			});

			return view;
		}
	}

	//LinphoneChatMessage Listener
	@Override
	public void onLinphoneChatMessageStateChanged(LinphoneChatMessage msg, State state) {
		redrawMessageList();
	}

	@Override
	public void onLinphoneChatMessageFileTransferReceived(LinphoneChatMessage msg, LinphoneContent content, LinphoneBuffer buffer) {}

	@Override
	public void onLinphoneChatMessageFileTransferSent(LinphoneChatMessage msg, LinphoneContent content, int offset, int size, LinphoneBuffer bufferToFill) {}

	@Override
	public void onLinphoneChatMessageFileTransferProgressChanged(LinphoneChatMessage msg, LinphoneContent content, int offset, int total) {}
}
