package com.allsop.gerard.wifidirect;

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.allsop.gerard.wifidirect.DeviceListFragment.DeviceActionListener;

import java.util.Calendar;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {
    private String TAG = "DeviceDetailFragment";
    private View mContentView = null;
    private WifiP2pInfo info;
    private ProgressDialog progressDialog = null;
    private static int port = 10051;
    private String clientMessage;
    private String serverMessage;
    Intent mServiceIntent;
    WifiP2pDevice device;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_detail, null);

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });
        mContentView.findViewById(R.id.client_send_msg).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendMessage();
                    }
                });
        mContentView.findViewById(R.id.server_send_msg).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendServerMessage("JSon object goes here");
                    }
                });
        return mContentView;
    }

    public void sendMessage() {
        Calendar rightNow = Calendar.getInstance();
        long offset = rightNow.get(Calendar.ZONE_OFFSET) +  rightNow.get(Calendar.DST_OFFSET);
        clientMessage = "Client time: "+Long.toString((rightNow.getTimeInMillis() + offset) %  (24 * 60 * 60 * 1000));
        Intent serviceIntent = new Intent(getActivity(), DataSocketManager.class);
        serviceIntent.setAction(DataSocketManager.ACTION_SEND_MESSAGE);
        serviceIntent.putExtra(DataSocketManager.EXTRAS_MESSAGE, clientMessage);
        serviceIntent.putExtra(DataSocketManager.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(DataSocketManager.EXTRAS_GROUP_OWNER_PORT, port);
        getActivity().startService(serviceIntent);
    }

    public void startSocketServerService(){
        Intent serviceIntent = new Intent(getActivity(), SocketServerService.class);
        serviceIntent.setAction(SocketServerService.ACTION_START_SERVER);
        getActivity().startService(serviceIntent);
    }

    public void sendServerMessage(String msg) {
        Calendar rightNow = Calendar.getInstance();
        long offset = rightNow.get(Calendar.ZONE_OFFSET) +  rightNow.get(Calendar.DST_OFFSET);
        serverMessage = "Server time: "+Long.toString((rightNow.getTimeInMillis() + offset) %  (24 * 60 * 60 * 1000));
        SocketServerService.setMessage(serverMessage);
        SocketServerService.nextTurn();
    }


    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);
        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));
        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            mContentView.findViewById(R.id.server_send_msg).setVisibility(View.VISIBLE);
            startSocketServerService();
        } else if (info.groupFormed) {
            mContentView.findViewById(R.id.client_send_msg).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
        }
    }

    public void showDetails(WifiP2pDevice device) {
        Log.d("DeviceDetailFragment", "showDetails()");
        //this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());
        Log.d("DeviceDetailFragment", "END showDetails(): " + device.toString());
    }

    public void resetViews() {
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.status_bar).setVisibility(View.GONE);
        mContentView.findViewById(R.id.client_send_msg).setVisibility(View.GONE);
        mContentView.findViewById(R.id.server_send_msg).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }
    private void log(String msg){
        Log.d(TAG, msg);
    }

}