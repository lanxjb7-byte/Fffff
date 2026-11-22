package com.lixxzy.rat;

import android.app.*;
import android.os.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class MainActivity extends Activity {
    LinearLayout root;
    FirebaseDatabase db;
    String code="";
    DatabaseReference deviceRef, commandRef;
    ValueEventListener commandListener;
    TextView status;
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    TextView txt(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(0xffffffff);t.setPadding(0,dp(10),0,dp(10));return t;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    void initUi(){
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(22),dp(28),dp(22),dp(20));root.setBackgroundColor(0xff0c0c10);
        ScrollView sc=new ScrollView(this);sc.addView(root);setContentView(sc);
        root.addView(txt(BuildConfig.IS_AGENT?"LIXXZY AGENT":"LIXXZY CONTROLLER",26));
        root.addView(txt("CONSENT-BASED DEVICE STATUS SHARING",11));
        status=txt("Connecting to Firebase…",14);root.addView(status);
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);initUi();
        try{FirebaseApp.initializeApp(this);}catch(Exception ignored){}
        if(FirebaseApp.getApps(this).isEmpty()){status.setText("Firebase config missing. Add google-services.json to android/app/.");return;}
        db=FirebaseDatabase.getInstance();
        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(task->{
            if(task.isSuccessful()){status.setText("Connected to Firebase.");if(BuildConfig.IS_AGENT)agentHome();else controllerHome();}
            else status.setText("Firebase sign-in failed: "+(task.getException()==null?"unknown":task.getException().getMessage()));
        });
    }
    String makeCode(){return String.format(Locale.US,"%06d",new Random().nextInt(1000000));}
    void agentHome(){
        root.removeAllViews();root.addView(txt("LIXXZY AGENT",25));
        root.addView(txt("This phone shares basic status only while this screen is active.",14));
        Button start=button("START SHARING");root.addView(start);
        start.setOnClickListener(v->startAgent());
        status=txt("Not sharing.",14);root.addView(status);
    }
    void startAgent(){
        code=makeCode();deviceRef=db.getReference("devices").child(code);commandRef=db.getReference("commands").child(code);
        root.removeAllViews();root.addView(txt("SHARING IS ACTIVE",22));root.addView(txt("PAIRING CODE",13));
        root.addView(txt(code,36));root.addView(txt("Share this code only with someone you authorize. Keep this screen open.",14));
        status=txt("Publishing device status…",14);root.addView(status);
        Button stop=button("STOP SHARING");root.addView(stop);stop.setOnClickListener(v->stopAgent());
        publish();
        commandListener=new ValueEventListener(){
            public void onDataChange(DataSnapshot s){String type=s.child("type").getValue(String.class);
                if("refresh".equals(type))publish();
                if("disconnect".equals(type))stopAgent();
            }
            public void onCancelled(DatabaseError e){status.setText("Listener error: "+e.getMessage());}
        };
        commandRef.addValueEventListener(commandListener);
    }
    void publish(){
        Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level=i==null?-1:i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
        int scale=i==null?100:i.getIntExtra(BatteryManager.EXTRA_SCALE,100);
        int battery=level<0?-1:level*100/Math.max(scale,1);
        Map<String,Object> m=new HashMap<>();
        m.put("deviceName",Build.MANUFACTURER+" "+Build.MODEL);
        m.put("androidVersion",Build.VERSION.RELEASE);
        m.put("battery",battery);m.put("lastSeen",System.currentTimeMillis());m.put("sharing",true);
        deviceRef.setValue(m).addOnSuccessListener(v->status.setText("Sharing • Battery "+battery+"%"))
            .addOnFailureListener(e->status.setText("Database error: "+e.getMessage()));
    }
    void stopAgent(){
        if(deviceRef!=null)deviceRef.child("sharing").setValue(false);
        if(commandRef!=null&&commandListener!=null)commandRef.removeEventListener(commandListener);
        root.removeAllViews();root.addView(txt("SHARING STOPPED",24));root.addView(txt("This phone is no longer sharing.",14));
        Button again=button("BACK");root.addView(again);again.setOnClickListener(v->agentHome());
    }
    void controllerHome(){
        root.removeAllViews();root.addView(txt("LIXXZY CONTROLLER",25));root.addView(txt("Enter the code shown on the Agent phone.",14));
        EditText input=new EditText(this);input.setSingleLine(true);input.setInputType(2);input.setHint("6-digit pairing code");root.addView(input);
        Button pair=button("CONNECT");root.addView(pair);status=txt("Only connect to devices whose owner shared their code.",13);root.addView(status);
        pair.setOnClickListener(v->{String c=input.getText().toString().trim();if(!c.matches("\\\\d{6}")){status.setText("Enter a valid six-digit code.");return;}watch(c);});
    }
    void watch(String c){
        db.getReference("devices").child(c).addValueEventListener(new ValueEventListener(){
            public void onDataChange(DataSnapshot s){
                root.removeAllViews();root.addView(txt("CONNECTED DEVICE",22));
                Boolean sharing=s.child("sharing").getValue(Boolean.class);
                if(!s.exists()||!Boolean.TRUE.equals(sharing)){root.addView(txt("Device offline or sharing stopped.",14));return;}
                String name=s.child("deviceName").getValue(String.class),ver=s.child("androidVersion").getValue(String.class);
                Long battery=s.child("battery").getValue(Long.class),seen=s.child("lastSeen").getValue(Long.class);
                root.addView(txt("Device: "+name,17));root.addView(txt("Android: "+ver,15));
                root.addView(txt("Battery: "+battery+"%",15));root.addView(txt("Last seen: "+new Date(seen==null?0:seen),13));
                Button refresh=button("REQUEST STATUS REFRESH");root.addView(refresh);refresh.setOnClickListener(v->send(c,"refresh"));
                Button disconnect=button("REQUEST DISCONNECT");root.addView(disconnect);disconnect.setOnClickListener(v->send(c,"disconnect"));
            }
            public void onCancelled(DatabaseError e){status.setText(e.getMessage());}
        });
    }
    void send(String c,String type){Map<String,Object> m=new HashMap<>();m.put("type",type);m.put("createdAt",System.currentTimeMillis());
        db.getReference("commands").child(c).setValue(m).addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}
      }
          
