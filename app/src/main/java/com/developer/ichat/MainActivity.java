package com.developer.ichat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.AIServiceException;
import ai.api.android.AIDataService;
import ai.api.android.AIService;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class MainActivity extends AppCompatActivity implements AIListener {
    int flag;
    RecyclerView recyclerView;
    EditText editText;
    RelativeLayout addBtn;
    DatabaseReference ref;
    FirebaseRecyclerAdapter<ChatMessage,chat_rec> adapter;
    Boolean flagFab = true;
    ArrayList<Contact> contactList = new ArrayList<Contact>();
    private AIService aiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},1);

        flag=0;

        recyclerView = (RecyclerView)findViewById(R.id.recyclerView);
        editText = (EditText)findViewById(R.id.editText);
        addBtn = (RelativeLayout)findViewById(R.id.addBtn);

        recyclerView.setHasFixedSize(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        ref = FirebaseDatabase.getInstance().getReference();
        ref.keepSynced(true);
        final AIConfiguration config = new AIConfiguration("0dba62f0cd5642c2befec56bf6a03c0f",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        final AIDataService aiDataService = new AIDataService(this,config);

        final AIRequest aiRequest = new AIRequest();


        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String message = editText.getText().toString().trim();

                if (!message.equals("")) {

                    ChatMessage chatMessage = new ChatMessage(message, "user");

                    if(message.contains("alarm")){
                        common(chatMessage);
                        chatMessage = new ChatMessage("Setting alarm", "bot");
                        ref.child("chat").push().setValue(chatMessage);
                        showTimeDialog();

                    }
                    else if(message.contains("play") && message.contains("music")){
                        common(chatMessage);
                        chatMessage = new ChatMessage("Playing music", "bot");
                        ref.child("chat").push().setValue(chatMessage);
                        openApp("music");
                    }else if(message.contains("play") && message.contains("video")) {
                        common(chatMessage);
                        chatMessage = new ChatMessage("Playing video", "bot");
                        ref.child("chat").push().setValue(chatMessage);
                        openApp("video");
                    }
                    else if(message.contains("call")){
                        common(chatMessage);
                        editText.setText("");
                        flag=1;
                        callNumber(message.substring(message.indexOf("call") + "call".length()));
                    }
                    else if(message.contains("clear chat") || message.contains("clear")){
                        ref.child("chat").removeValue();
                        common(chatMessage);
                        chatMessage = new ChatMessage("Cleared Chat!", "bot");
                        ref.child("chat").push().setValue(chatMessage);
                    }
                    else if(message.contains("open"))
                    {
                        common(chatMessage);
                        openApp(message.substring(message.indexOf("open") + "open".length()).trim().toLowerCase());
                    }else {
                        ref.child("chat").push().setValue(chatMessage);

                        aiRequest.setQuery(message);
                        new AsyncTask<AIRequest, Void, AIResponse>() {

                            @Override
                            protected AIResponse doInBackground(AIRequest... aiRequests) {
                                final AIRequest request = aiRequests[0];
                                try {
                                    final AIResponse response = aiDataService.request(aiRequest);
                                    return response;
                                } catch (AIServiceException e) {
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(AIResponse response) {
                                if (response != null) {

                                    Result result = response.getResult();
                                    String reply = result.getFulfillment().getSpeech();
                                    ChatMessage chatMessage = new ChatMessage(reply, "bot");
                                    Log.e("inside onPost", reply);
                                    ref.child("chat").push().setValue(chatMessage);
                                }
                            }
                        }.execute(aiRequest);
                    }
                }
                else {
                    aiService.startListening();
                }
                if(flag==0)
                    editText.setText("");
                flag=0;

            }
        });



        //updateContacts();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ImageView fab_img = (ImageView)findViewById(R.id.fab_img);
                Bitmap img = BitmapFactory.decodeResource(getResources(),R.drawable.ic_send_white_24dp);
                Bitmap img1 = BitmapFactory.decodeResource(getResources(),R.drawable.ic_mic_white_24dp);

                Log.e("inside onTextChanged","inside onTextChanged");
                if (s.toString().trim().length()!=0 && flagFab){
                    ImageViewAnimatedChange(MainActivity.this,fab_img,img);
                    flagFab=false;

                    Log.e("inside onTextChanged","inside if");

                }
                else if (s.toString().trim().length()==0){
                    ImageViewAnimatedChange(MainActivity.this,fab_img,img1);
                    flagFab=true;

                    Log.e("inside onTextChanged","inside else if");
                }


            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        adapter = new FirebaseRecyclerAdapter<ChatMessage, chat_rec>(ChatMessage.class,R.layout.message_list,chat_rec.class,ref.child("chat")) {
            @Override
            protected void populateViewHolder(chat_rec viewHolder, ChatMessage model, int position) {

                if (model.getMsgUser().equals("user")) {

                    Log.e("inside adapter","inside if");

                    viewHolder.rightText.setText(model.getMsgText());

                    viewHolder.rightText.setVisibility(View.VISIBLE);
                    viewHolder.leftText.setVisibility(View.GONE);
                }
                else {
                    viewHolder.leftText.setText(model.getMsgText());

                    Log.e("inside adapter","inside else");
                    viewHolder.rightText.setVisibility(View.GONE);
                    viewHolder.leftText.setVisibility(View.VISIBLE);
                }
            }
        };

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                int msgCount = adapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisiblePosition == -1 ||
                        (positionStart >= (msgCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    recyclerView.scrollToPosition(positionStart);

                }

            }
        });

        recyclerView.setAdapter(adapter);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 101);
            }
            return;
        }
        updateContacts();

    }
    public void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, R.anim.zoom_out);
        final Animation anim_in  = AnimationUtils.loadAnimation(c, R.anim.zoom_in);
        anim_out.setAnimationListener(new Animation.AnimationListener()
        {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation)
            {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {}
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }

    @Override
    public void onResult(ai.api.model.AIResponse response) {


        Result result = response.getResult();

        String message = result.getResolvedQuery();
        ChatMessage chatMessage0 = new ChatMessage(message, "user");
        ref.child("chat").push().setValue(chatMessage0);
        Log.e("inside onResult",message);

        String reply = result.getFulfillment().getSpeech();
        ChatMessage chatMessage = new ChatMessage(reply, "bot");
        ref.child("chat").push().setValue(chatMessage);


    }

    @Override
    public void onError(ai.api.model.AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    @Override
    public void onListeningCanceled() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onListeningFinished() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private void openApp(String requiredApp) {
        String[] requiredPackage = null;
        int count = 0;
        ArrayList<String> allRequiredPackages = new ArrayList<String>();
        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            String currentPackage = packageInfo.packageName;
           // Log.e(TAG, "Installed package :" + currentPackage);
            if (currentPackage.contains(requiredApp.toLowerCase())) {
                allRequiredPackages.add(currentPackage);
                break;
            }
        }

        if(allRequiredPackages.size() > 0){
            launchApp(allRequiredPackages.get(0),requiredApp);
        }else{

            String reply = "No such app found";
            ChatMessage chatMessage = new ChatMessage(reply, "bot");
            Log.e("inside launchApp", reply);
            ref.child("chat").push().setValue(chatMessage);
        //    Toast.makeText(getApplicationContext(),"Sorry,I can not find such application", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchApp(String packageName, String requiredApp) {
        Intent mIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (mIntent != null) {
            try {

                String reply = "Opening "+requiredApp;
                ChatMessage chatMessage = new ChatMessage(reply, "bot");
                Log.e("inside launchApp", reply);
                ref.child("chat").push().setValue(chatMessage);

                startActivity(mIntent);

            } catch (ActivityNotFoundException err) {
                Toast t = Toast.makeText(getApplicationContext(), "App not found", Toast.LENGTH_SHORT);
                t.show();
            }
        }
    }

    private void callNumber(String callingName) {
        Toast.makeText(getApplicationContext(),callingName.toLowerCase(),Toast.LENGTH_SHORT).show();
        for (int i = 0; i < contactList.size(); i++) {
            callingName=callingName.trim();//replaceAll(" ","");

            Log.e(contactList.get(i).getName().toLowerCase(),"name");
            if (contactList.get(i).getName().toLowerCase().contains(callingName.toLowerCase())) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + contactList.get(i).getNumber()));

                String reply = "Calling "+callingName;
                ChatMessage chatMessage = new ChatMessage(reply, "bot");
                Log.e("inside calling", reply);
                ref.child("chat").push().setValue(chatMessage);

                startActivity(callIntent);break;
            }
        }
    }

    private void updateContacts() {
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null,
                null, null);

        Log.e("contactList in show", contactList.toString());
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            contactList.add(new Contact(name, phNumber));
        }
        cursor.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    private void showTimeDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialogtime, null);
        dialogBuilder.setView(dialogView);

        final TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.timePicker);

        dialogBuilder.setTitle("Select Time");
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                int hour = timePicker.getCurrentHour();
                int min = timePicker.getCurrentMinute();
                setAlarm(hour,min);
                //speakIt("Alarm is set for "+ hour +" O'clock and "+min+"minutes");
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    private void setAlarm(int hours, int min) {
        Intent openClockIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
        openClockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openClockIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openClockIntent.putExtra(AlarmClock.EXTRA_HOUR, hours);
        openClockIntent.putExtra(AlarmClock.EXTRA_MINUTES, min);
        startActivity(openClockIntent);
    }

    private void common(ChatMessage chatMessage){
        ref.child("chat").push().setValue(chatMessage);
        editText.setText("");
        flag=1;
    }

    public void stopRecognition(final View view) {
        aiService.stopListening();
    }

    public void cancelRecognition(final View view) {
        aiService.cancel();
    }
}
