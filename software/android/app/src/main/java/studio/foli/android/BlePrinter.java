package studio.foli.android;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.*;

/** Activity-owned BLE session. All mutable session state is confined to the main looper. */
// Runtime access changes are recovered by runBle/postBle and cleanup guards.
@SuppressLint("MissingPermission")
public final class BlePrinter {
    static final UUID SERVICE=UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    static final UUID CHARACTERISTIC=UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    static final UUID CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public interface Listener { void changed(); void message(String text); }
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Listener listener;
    public final LinkedHashMap<String,BluetoothDevice> devices=new LinkedHashMap<>();
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothLeScanner scanner;
    private Runnable timeout, scanStop, watchdog, poll;
    private boolean closed, pending, scanning, started, failedJob;
    public boolean ready, connecting, sending, awaiting, dirty;
    public PrintOutcome jobOutcome=PrintOutcome.IDLE;
    public int battery=-1, temperature=-1, paper=-1, printState=-1, progress;
    public int mtu=23;
    public String status="Not connected", deviceName="Your FOLI", address="";
    private byte[][] rows;
    private int index, density;
    private long session;

    public BlePrinter(Context context,Listener listener) { this.context=context; this.listener=listener; }
    public boolean busy() { return sending||awaiting; }
    public boolean isScanning() { return scanning; }
    private BluetoothAdapter adapter() {
        BluetoothManager manager=(BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        return manager==null?null:manager.getAdapter();
    }
    public boolean available() { try { BluetoothAdapter a=adapter(); return a!=null&&a.isEnabled(); } catch(SecurityException|IllegalStateException e) { return false; } }
    private void runBle(Runnable operation) {
        BleOperation.run(operation, () -> {
            stopScan();
            fail("Bluetooth access changed. Check permissions and reconnect.");
        });
    }
    private void postBle(Runnable operation) { main.post(() -> runBle(operation)); }
    private void change() { if (!closed) listener.changed(); }
    private void say(String value) { status=value; change(); }
    private void arm(String operation) {
        pending=true;
        if(timeout!=null) main.removeCallbacks(timeout);
        timeout=()->fail(operation+" timed out. Reconnect and try again.");
        main.postDelayed(timeout,12000);
    }
    private void disarm() { pending=false; if(timeout!=null)main.removeCallbacks(timeout); }
    public void scan() {
        stopScan();
        if(!available()){listener.message("Turn on Bluetooth to find your printer.");return;}
        if(connecting||busy()){listener.message("Finish the current connection or print first.");return;}
        devices.clear();
        try { scanner=adapter().getBluetoothLeScanner(); } catch(SecurityException|IllegalStateException e) { fail("Bluetooth access changed. Check permissions and reconnect."); return; }
        if(scanner==null){listener.message("Bluetooth scanning is unavailable.");return;}
        scanning=true; say("Looking for your FOLI…");
        try {
            scanner.startScan(null,new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),scanCallback);
            scanStop=()->{stopScan();say(devices.isEmpty()?"No printer found. Check power and try again.":"Choose your printer below.");};
            main.postDelayed(scanStop,12000);
        } catch(SecurityException|IllegalStateException e){stopScan();say("Bluetooth permission or scanning is unavailable.");}
    }
    private final ScanCallback scanCallback=new ScanCallback(){
        @Override public void onScanResult(int type, ScanResult result){
            postBle(()->{
                if(!scanning||closed)return;
                BluetoothDevice d=result.getDevice();
                String name=result.getScanRecord()==null?null:result.getScanRecord().getDeviceName();
                if(name==null)name=d.getName();
                boolean match=name!=null&&(name.equals("Mini-Printer")||name.toLowerCase(Locale.ROOT).startsWith("foli"));
                if(result.getScanRecord()!=null&&result.getScanRecord().getServiceUuids()!=null)
                    for(android.os.ParcelUuid id:result.getScanRecord().getServiceUuids())if(SERVICE.equals(id.getUuid()))match=true;
                if(match&&!devices.containsKey(d.getAddress())){devices.put(d.getAddress(),d);change();}
            });
        }
        @Override public void onScanFailed(int error){postBle(()->{stopScan();say("Scan failed ("+error+"). Wait a moment and try again.");});}
    };
    public void stopScan(){
        if(scanStop!=null)main.removeCallbacks(scanStop);
        if(scanner!=null&&scanning)try{scanner.stopScan(scanCallback);}catch(SecurityException|IllegalStateException ignored){}
        scanning=false;
    }
    public void connect(BluetoothDevice device){
        if(busy())return;
        stopScan(); disconnect();
        deviceName="FOLI";
        connecting=true; say("Connecting to your printer…");
        try {address=device.getAddress();gatt=device.connectGatt(context,false,callback,BluetoothDevice.TRANSPORT_LE);arm("Connection");}
        catch(SecurityException|IllegalArgumentException|IllegalStateException e){fail("Could not connect. Check Bluetooth permissions.");}
    }
    private final BluetoothGattCallback callback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int result,int state){postBle(()->{
            if(g!=gatt||closed)return;
            disarm();
            if(result!=BluetoothGatt.GATT_SUCCESS||state==BluetoothProfile.STATE_DISCONNECTED){fail("Printer disconnected. Check power and reconnect.");return;}
            if(state==BluetoothProfile.STATE_CONNECTED){
                say("Preparing a reliable connection…");arm("MTU negotiation");
                if(!g.requestMtu(185))fail("Could not request a larger BLE packet size.");
            }
        });}
        @Override public void onMtuChanged(BluetoothGatt g,int value,int result){postBle(()->{
            if(g!=gatt||closed)return;
            disarm(); mtu=value;
            if(result!=BluetoothGatt.GATT_SUCCESS||mtu<51){fail("This connection cannot carry a full print row. Reconnect; an MTU of at least 51 is required.");return;}
            arm("Service discovery");if(!g.discoverServices())fail("Could not discover printer services.");
        });}
        @Override public void onServicesDiscovered(BluetoothGatt g,int result){postBle(()->{
            if(g!=gatt||closed)return;
            disarm();BluetoothGattService service=g.getService(SERVICE);
            characteristic=service==null?null:service.getCharacteristic(CHARACTERISTIC);
            if(result!=0||characteristic==null){fail("This device does not expose the FOLI-compatible printing service.");return;}
            int props=characteristic.getProperties();
            if((props&BluetoothGattCharacteristic.PROPERTY_WRITE)==0||(props&BluetoothGattCharacteristic.PROPERTY_NOTIFY)==0){fail("Printer must support acknowledged writes and status notifications.");return;}
            BluetoothGattDescriptor descriptor=characteristic.getDescriptor(CCCD);
            if(descriptor==null||!g.setCharacteristicNotification(characteristic,true)){fail("Printer status notifications are unavailable.");return;}
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            arm("Status subscription");if(!g.writeDescriptor(descriptor))fail("Could not subscribe to printer status.");
        });}
        @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor descriptor,int result){postBle(()->{
            if(g!=gatt||closed)return;
            disarm();if(result!=0){fail("Could not enable printer status.");return;}
            ready=true;connecting=false;say(dirty?"Connected. Power-cycle the printer before another job.":"Connected. Waiting for device status…");
            readStatus();
        });}
        @Override public void onCharacteristicRead(BluetoothGatt g,BluetoothGattCharacteristic c,int result){
            byte[] value=c.getValue()==null?null:c.getValue().clone();
            postBle(()->{if(g!=gatt||closed)return;disarm();if(result==0)receive(value);else say("Connected. Status could not be read yet.");});
        }
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){
            byte[] value=c.getValue()==null?null:c.getValue().clone();
            postBle(()->{if(g==gatt&&!closed)receive(value);});
        }
        @Override public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int result){postBle(()->{
            if(g!=gatt||closed||!sending)return;
            disarm();if(result!=0){fail("A print packet was not accepted. Restart the printer before retrying.");return;}
            index++;
            if(index>rows.length){
                sending=false;awaiting=true;rows=null;progress=100;
                say(started?"Printing…":"Sent. Waiting for the printer to report progress…");
                watchdog=()->{if(awaiting){awaiting=false;dirty=true;jobOutcome=PrintOutcome.STOPPED_UNCONFIRMED;say("Completion not confirmed. Check the paper result; do not resend automatically.");}};
                main.postDelayed(watchdog,90000);
                schedulePoll();
            }else{
                progress=Math.min(99,Math.max(0,index-1)*100/rows.length);change();
                long token=session;
                main.postDelayed(()->{if(token==session&&sending)writeNext();},20);
            }
        });}
    };
    private void receive(byte[] value){
        int[] s=PrinterProtocol.decodeStatus(value);if(s==null)return;
        battery=s[0];temperature=s[1];paper=s[2];printState=s[3];
        if(busy()&&(paper==1||temperature>=65)){
            fail(paper==1?"Out of paper. Load paper, then restart the printer before retrying.":"Print head is hot. Let it cool; check the print before retrying.");return;
        }
        if(busy()&&(printState==1||printState==2)){started=true;if(awaiting)status="Printing…";}
        PrintOutcome reported=PrintOutcome.fromStatus(awaiting,started,failedJob,paper,temperature,printState);
        if(reported==PrintOutcome.STOPPED_UNCONFIRMED){
            awaiting=false;dirty=true;jobOutcome=reported;
            if(watchdog!=null)main.removeCallbacks(watchdog);
            if(poll!=null)main.removeCallbacks(poll);
            status="Printer stopped. Check your paper. Completion is not confirmed; restart the printer before another job.";
        }
        else if(!busy()&&!dirty)status=paper==1?"Out of paper. Load a fresh roll.":temperature>=65?"Print head is hot. Give it a moment to cool.":"Ready for your next little idea.";
        change();
    }
    private void readStatus(){
        if(!ready||pending||sending||gatt==null||characteristic==null)return;
        if((characteristic.getProperties()&BluetoothGattCharacteristic.PROPERTY_READ)==0)return;
        arm("Status read");runBle(() -> { if(!gatt.readCharacteristic(characteristic))disarm(); });
    }
    public void refresh(){if(!busy())readStatus();}
    private void schedulePoll(){
        if(poll!=null)main.removeCallbacks(poll);
        poll=()->{if(awaiting&&ready){readStatus();schedulePoll();}};
        main.postDelayed(poll,3000);
    }
    public String canPrint(){
        if(!ready)return "Connect your printer first.";
        if(dirty)return "The previous job was interrupted or unconfirmed. Restart the printer to clear its buffer, then confirm that restart in Your device.";
        if(busy())return "A print is already in progress.";
        if(pending)return "Synchronizing device status. Wait a moment, then try again.";
        if(paper<0||temperature<0||battery<0)return "Waiting for real device status. Refresh status or wait for the next notification.";
        if(paper==1)return "Out of paper. Load paper before printing.";
        if(temperature>=65)return "The print head is hot. Let it cool before printing.";
        if(printState==1||printState==2)return "The printer is already working. Wait for it to finish.";
        return null;
    }
    public void print(byte[][] data,int level){
        String error=canPrint();if(error!=null){listener.message(error);return;}
        if(data==null||data.length==0||data.length>PrinterProtocol.MAX_ROWS){listener.message("This paper is too long. Shorten it before printing.");return;}
        for(byte[] row:data)if(row.length!=48)throw new IllegalArgumentException("Rows must be exactly 48 bytes");
        rows=data;density=level;index=-1;progress=0;started=false;failedJob=false;jobOutcome=PrintOutcome.IN_PROGRESS;sending=true;awaiting=false;
        session++;say("Sending to printer…");writeNext();
    }
    private void writeNext(){
        if(!sending||gatt==null||characteristic==null)return;
        byte[] packet=index<0?PrinterProtocol.density(density):index<rows.length?rows[index]:PrinterProtocol.finish();
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        characteristic.setValue(packet);
        arm("Print transfer");
        runBle(() -> { if(!gatt.writeCharacteristic(characteristic))fail("Could not send a print packet. Restart the printer before retrying."); });
    }
    public void stopSending(){if(busy())fail("Transfer stopped. A started print may continue; there is no remote stop command. Restart the printer before retrying.");}
    public void acknowledgeRestart(){
        if(busy())return;
        dirty=false;started=false;failedJob=false;jobOutcome=PrintOutcome.RESTART_CONFIRMED;say("Printer restart confirmed. Refreshing status…");readStatus();
    }
    private void fail(String message){
        if(busy()){dirty=true;failedJob=true;jobOutcome=PrintOutcome.FAILED;}
        session++;sending=false;awaiting=false;rows=null;
        if(watchdog!=null)main.removeCallbacks(watchdog);if(poll!=null)main.removeCallbacks(poll);
        disarm();releaseGatt();say(message);
    }
    private void releaseGatt(){
        BluetoothGatt old=gatt;gatt=null;characteristic=null;ready=false;connecting=false;mtu=23;
        battery=-1;temperature=-1;paper=-1;printState=-1;
        if(old!=null){
            try{old.disconnect();}catch(SecurityException|IllegalStateException ignored){}
            try{old.close();}catch(SecurityException|IllegalStateException ignored){}
        }
    }
    public void disconnect(){
        if(busy()){dirty=true;failedJob=true;jobOutcome=PrintOutcome.FAILED;}
        session++;sending=false;awaiting=false;rows=null;disarm();
        if(watchdog!=null)main.removeCallbacks(watchdog);if(poll!=null)main.removeCallbacks(poll);
        releaseGatt();say("Not connected");
    }
    public void close(){stopScan();disconnect();closed=true;main.removeCallbacksAndMessages(null);}
}
