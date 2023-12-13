package dz.bechar.messagetestwiththrd;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import dz.bechar.messagetestwiththrd.R;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;


    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
   //public String message,phoneNumber,decryptedText,encryptedText,encryptedTextsent,secretKey = "YourSecretKey123";

    private TextView messageTextView;
    private SMSReceiver smsReceiver;
    Button btSENT;
    CheckBox chkINFO;
    EditText messageEditText,phoneNumberEditText;
    String MSGsent="", MSGincoming="",MSGphone="";
    LinearLayout MESSAGELAYOT;

    public String message,phoneNumber,decryptedText,
            encryptedText,encryptedTextsent,
            secretKey = "YourSecretKey123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // bar status
        getWindow().setStatusBarColor(ContextCompat.getColor(MainActivity.this, R.color.Orenge1));

        messageEditText=findViewById(R.id.EDTmessage);
        phoneNumberEditText=findViewById(R.id.EDTphone);
        MESSAGELAYOT=findViewById(R.id.linearLayout);
        btSENT=findViewById(R.id.BTsent);
        chkINFO=findViewById(R.id.CHKinformation);
        messageTextView = findViewById(R.id.TVincoming);

        // when you press sent button
        btSENT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String n = messageEditText.getText().toString();
                //sendSMS(n);
                try {
                    encryptedText = encrypt(n, secretKey);
                    decryptedText = decrypt(encryptedText, secretKey);
                    // System.out.println("Original Text: " + originalText);
                    System.out.println("Encrypted Text: " + encryptedText);
                    System.out.println("Decrypted Text: " + decryptedText);
                    messageEditText.setText(encryptedText);
                    messageTextView.setText("");
                    sendSMS(encryptedText);
                    //   messageTextView.setText(originalText+"  is  :"+encryptedText);
                } catch (Exception e) {
                    e.printStackTrace();
                }
               // messageTextView.setText(n+"");
            }  });

        if (checkSmsPermission()) {
            setupSmsReceiver();
        } else {
            requestSmsPermission();
        }
    }
    // sent message FUNCTION

    public void sendSMS(String msg) {
        String phoneNumber = phoneNumberEditText.getText().toString();
        String test_message = "hello ,how are you ! ";
        //String message = messageEditText.getText().toString();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // Demande d'autorisation pour envoyer des SMS
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
        } else {
            // L'autorisation est déjà accordée, envoyez le SMS
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, msg, null, null);
        }
    }

// PERMISSION
    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_REQUEST_CODE);
    }

    // recieve FUNCTOION
    private void setupSmsReceiver() {
        smsReceiver = new SMSReceiver();
        IntentFilter intentFilter = new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        registerReceiver(smsReceiver, intentFilter);

        // Register a content observer to listen for changes to the SMS inbox
        getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true
                , new SMSContentObserver(new Handler()));
    }

    // PERMISSION
    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupSmsReceiver();
            } else {
                Toast.makeText(this, "Permission SMS refusée. Impossible de lire les messages.", Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (smsReceiver != null) {
            unregisterReceiver(smsReceiver);
        }
    }

// SMS FUNCTION
    private class SMSReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                try {
                    displayLastReceivedMessage();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private class SMSContentObserver extends ContentObserver {
        public SMSContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                displayLastReceivedMessage();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void displayLastReceivedMessage() throws Exception {
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = getContentResolver().query(uri, null, null, null, "date DESC");

        if (cursor != null && cursor.moveToFirst()) {
            String message = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            messageTextView.setText("" +decrypt(message, secretKey));
            cursor.close();
        } else {
            messageTextView.setText("Aucun message trouvé dans la boîte de réception.");
        }
    }

    // CHIFFREMENT

    public static String encrypt(String plainText, String secretKey) throws Exception {
        SecretKey key = generateKey(secretKey);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String encryptedText, String secretKey) throws Exception {
        SecretKey key = generateKey(secretKey);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes);
    }

    private static SecretKey generateKey(String secretKey) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        return new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
    }
    // BEFORE END
}