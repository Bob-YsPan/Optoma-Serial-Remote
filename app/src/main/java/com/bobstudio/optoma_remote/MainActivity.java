package com.bobstudio.optoma_remote;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {

    private static final String ACTION_USB_PERMISSION = "com.bobstudio.optoma_remote.USB_PERMISSION";
    
    private UsbSerialPort usbSerialPort;
    private SerialInputOutputManager usbIoManager;
    private TextView tvStatus;
    private Button btnConnect, btnDisconnect;
    private GridLayout gridLayout;
    private boolean isDialogOpen = false;

    private final Map<Integer, String> keycodeBindings = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        gridLayout = findViewById(R.id.gridLayout);

        // Prevent buttons from stealing Enter key focus when using external keyboard
        btnConnect.setFocusable(false);
        btnDisconnect.setFocusable(false);

        btnConnect.setOnClickListener(v -> {
            vibrate();
            connect();
        });
        btnDisconnect.setOnClickListener(v -> {
            vibrate();
            disconnect();
        });

        setupCommands();
    }

    private void setupCommands() {
        // (Function Name, Command Code, Android Keycode if applicable)
        Object[][] commands = {
            {"", "", null},
            {"↑", "00140 10", KeyEvent.KEYCODE_DPAD_UP},
            {"", "", null},
            {"←", "00140 11", KeyEvent.KEYCODE_DPAD_LEFT},
            {"Enter", "00140 12", KeyEvent.KEYCODE_ENTER},
            {"→", "00140 13", KeyEvent.KEYCODE_DPAD_RIGHT},
            {"", "", null},
            {"↓", "00140 14", KeyEvent.KEYCODE_DPAD_DOWN},
            {"", "", null},
            {"Keystone +", "00140 15", KeyEvent.KEYCODE_K},
            {"Vol +", "00140 18", KeyEvent.KEYCODE_F},
            {"Brightness", "00140 19", KeyEvent.KEYCODE_B},
            {"Keystone -", "00140 16", KeyEvent.KEYCODE_COMMA},
            {"Vol -", "00140 17", KeyEvent.KEYCODE_V},
            {"Zoom", "00140 21", KeyEvent.KEYCODE_Z},
            {"AV Mute ON", "0002 1", KeyEvent.KEYCODE_X},
            {"AV Mute OFF", "0002 0", KeyEvent.KEYCODE_C},
            {"Menu", "00140 20", KeyEvent.KEYCODE_M},
            {"Source", "00100 3", KeyEvent.KEYCODE_I},
            {"Power ON", "0000 1", KeyEvent.KEYCODE_P},
            {"Power OFF", "0000 0", KeyEvent.KEYCODE_O},
            {"", "", null},
            {"Message", "CUSTOM_MSG", KeyEvent.KEYCODE_H},
            {"", "", null}
        };

        for (Object[] item : commands) {
            String name = (String) item[0];
            String cmd = (String) item[1];
            Integer keycode = (Integer) item[2];

            if (name.isEmpty() && cmd.isEmpty()) {
                TextView empty = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                empty.setLayoutParams(params);
                gridLayout.addView(empty);
                continue;
            }

            Button btn = new Button(this);
            btn.setFocusable(false); // Prevent remote buttons from stealing keyboard focus
            String label = name;
            if (keycode != null) {
                String keyName = KeyEvent.keyCodeToString(keycode).replace("KEYCODE_", "");
                if (keyName.startsWith("DPAD_")) {
                    keyName = keyName.substring(5);
                } else if (keycode == KeyEvent.KEYCODE_COMMA) {
                    keyName = ",";
                }
                label += "\n(" + keyName + ")";
                keycodeBindings.put(keycode, cmd);
            }
            btn.setText(label);
            btn.setAllCaps(false);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            btn.setLayoutParams(params);
            
            if ("CUSTOM_MSG".equals(cmd)) {
                btn.setOnClickListener(v -> {
                    vibrate();
                    promptMessage();
                });
            } else {
                btn.setOnClickListener(v -> {
                    vibrate();
                    sendCommand(cmd);
                });
            }
            gridLayout.addView(btn);
        }
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(75);
            }
        }
    }

    private void promptMessage() {
        isDialogOpen = true;
        final EditText input = new EditText(this);
        input.setText("Hello World");
        input.setSelection(0, input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("Send Message")
                .setMessage("Enter English message:")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    vibrate();
                    String msg = input.getText().toString();
                    sendCommand("00210 " + msg);
                    isDialogOpen = false;
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    vibrate();
                    dialog.cancel();
                    isDialogOpen = false;
                })
                .setOnDismissListener(dialog -> isDialogOpen = false)
                .show();
    }

    private void connect() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            Toast.makeText(this, "No USB serial devices found", Toast.LENGTH_SHORT).show();
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();

        if (!manager.hasPermission(device)) {
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), flags);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(usbReceiver, new IntentFilter(ACTION_USB_PERMISSION), Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(usbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
            }
            manager.requestPermission(device, usbPermissionIntent);
        } else {
            openPort(driver);
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                            UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
                            if (driver != null) openPort(driver);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                unregisterReceiver(this);
            }
        }
    };

    private void openPort(UsbSerialDriver driver) {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) return;

        usbSerialPort = driver.getPorts().get(0);
        try {
            usbSerialPort.open(connection);
            usbSerialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            
            usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
            usbIoManager.start();

            tvStatus.setText("Status: Connected to " + driver.getDevice().getDeviceName());
            btnConnect.setEnabled(false);
            btnDisconnect.setEnabled(true);
        } catch (IOException e) {
            tvStatus.setText("Status: Connection failed");
            e.printStackTrace();
        }
    }

    private void disconnect() {
        if (usbIoManager != null) {
            usbIoManager.stop();
            usbIoManager = null;
        }
        if (usbSerialPort != null) {
            try {
                usbSerialPort.close();
            } catch (IOException ignored) {}
            usbSerialPort = null;
        }
        tvStatus.setText("Status: Disconnected");
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
    }

    private void sendCommand(String cmd) {
        if (usbSerialPort == null) {
            Toast.makeText(this, "Not connected!", Toast.LENGTH_SHORT).show();
            return;
        }
        String asciiCommand = "~" + cmd + "\r";
        try {
            usbSerialPort.write(asciiCommand.getBytes(), 2000);
            tvStatus.setText("Status: Sent '" + cmd + "'");
        } catch (IOException e) {
            Toast.makeText(this, "Send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (!isDialogOpen && keycodeBindings.containsKey(keyCode)) {
                String cmd = keycodeBindings.get(keyCode);
                if ("CUSTOM_MSG".equals(cmd)) {
                    promptMessage();
                } else {
                    sendCommand(cmd);
                }
                return true; // Consume the event to prevent clicking focused buttons
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onNewData(byte[] data) {
        new Handler(Looper.getMainLooper()).post(() -> {
            String response = new String(data).trim();
            if (response.contains("P")) {
                tvStatus.setText("Status: Success (P)");
            } else if (response.contains("F")) {
                tvStatus.setText("Status: Failed (F)");
            } else if (!response.isEmpty()) {
                tvStatus.setText("Status: Response: " + response);
            }
        });
    }

    @Override
    public void onRunError(Exception e) {
        new Handler(Looper.getMainLooper()).post(() -> disconnect());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }
}
