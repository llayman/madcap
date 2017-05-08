package edu.umd.fcmd.sensorlisteners.listener.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import java.util.Set;

import javax.inject.Inject;

import edu.umd.fcmd.sensorlisteners.issuehandling.PermissionsManager;
import edu.umd.fcmd.sensorlisteners.listener.Listener;
import edu.umd.fcmd.sensorlisteners.model.Probe;
import edu.umd.fcmd.sensorlisteners.model.bluetooth.BluetoothConnectionProbe;
import edu.umd.fcmd.sensorlisteners.model.bluetooth.BluetoothStateProbe;
import edu.umd.fcmd.sensorlisteners.model.bluetooth.BluetoothStaticAttributesProbe;
import edu.umd.fcmd.sensorlisteners.service.ProbeManager;

/**
 * Created by MMueller on 12/2/2016.
 * <p>
 * BluetoothListener listening to certain bluetooth events.
 */

public class BluetoothListener implements Listener {
    private final String TAG = getClass().getSimpleName();

    private static final String CONNECTED = "connected";
    private static final String CONNECTING = "connecting";
    private static final String DISCONNECTED = "disconnected";
    private static final String CACHE_CLOSING = "cacheClosing";
    private static final String NEW_CONNECTION_STATE = "new ConnectionState: ";

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final ProbeManager<Probe> probeManager;
    private final PermissionsManager permissionsManager;
    private final BluetoothInformationReceiverFactory bluetoothInformationReceiverFactory;
    private BluetoothInformationReceiver receiver;

    private boolean runningState;

    @Inject
    public BluetoothListener(Context context,
                             ProbeManager<Probe> probeManager,
                             BluetoothAdapter bluetoothAdapter,
                             PermissionsManager permissionsManager,
                             BluetoothInformationReceiverFactory bluetoothInformationReceiverFactory) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.probeManager = probeManager;
        this.permissionsManager = permissionsManager;
        this.bluetoothInformationReceiverFactory = bluetoothInformationReceiverFactory;
    }

    @Override
    public void onUpdate(Probe state) {
        probeManager.save(state);
    }

    @Override
    public void startListening() {
        if (!runningState && (bluetoothAdapter != null)) {
            if (isPermittedByUser()) {
                receiver = bluetoothInformationReceiverFactory.create(this);
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
                intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                intentFilter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
                intentFilter.addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                intentFilter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                context.registerReceiver(receiver, intentFilter);

                createInitialProbes();
                runningState = true;
            } else {
                permissionsManager.requestPermissionFromNotification();
                Log.i(TAG, "Bluetooth listener NOT listening");
                runningState = false;
            }
        }
    }

    @Override
    public void stopListening() {
        if (runningState) {
            if (receiver != null) {
                context.unregisterReceiver(receiver);
            }
            receiver = null;
        }
        runningState = false;
    }

    @Override
    public boolean isPermittedByUser() {
        //non dangerous permission
        if (permissionsManager.isBluetoothPermitted()) {
            Log.i(TAG, "Bluetooth access permitted");
            return true;
        } else {
            Log.e(TAG, "Bluetooth access NOT permitted");
            return false;
        }
    }

    /**
     * Creates the initial probes.
     */
    private void createInitialProbes() {
        //Bluetooth State Probe
        BluetoothStateProbe bluetoothStateProbe = new BluetoothStateProbe();
        bluetoothStateProbe.setDate(System.currentTimeMillis());
        bluetoothStateProbe.setState(getState());
        onUpdate(bluetoothStateProbe);

        //Bluetooth Static Attributes Probe
        BluetoothStaticAttributesProbe bluetoothStaticAttributesProbe = new BluetoothStaticAttributesProbe();
        bluetoothStaticAttributesProbe.setDate(System.currentTimeMillis());
        bluetoothStaticAttributesProbe.setAddress(bluetoothAdapter.getAddress());
        bluetoothStaticAttributesProbe.setName(bluetoothAdapter.getName());
        onUpdate(bluetoothStaticAttributesProbe);

        //Possible connected Bluetooth Devices
        Set<BluetoothDevice> boundDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : boundDevices) {
            BluetoothConnectionProbe bluetoothConnectionProbe = new BluetoothConnectionProbe();
            bluetoothConnectionProbe.setDate(System.currentTimeMillis());
            int bondState = bluetoothDevice.getBondState();
            String state;
            if (bondState == BluetoothDevice.BOND_BONDING) {
                state = "BONDING";
            } else if (bondState == BluetoothDevice.BOND_BONDED) {
                state = "BONDED";
            } else {
                state = "NONE";
            }
            bluetoothConnectionProbe.setState(state);
            if (bluetoothDevice.getAddress() != null) {
                bluetoothConnectionProbe.setForeignAddress(bluetoothDevice.getAddress());
            }
            bluetoothConnectionProbe.setForeignName(bluetoothDevice.getName());
            onUpdate(bluetoothConnectionProbe);
        }

    }

    BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * Gets the state from the BluetoothAdapter.
     *
     * @return the state of the BluetoothAdapter.
     */
    public int getState() {
        if (isPermittedByUser()) return bluetoothAdapter.getState();
//        }else permissionsManager.onPermissionDenied(Manifest.permission.BLUETOOTH);
        return 0;
    }
}
