package com.bee.passenger.airbnbmapexample.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.bee.passenger.R;
import com.bee.passenger.airbnbmapexample.constants.NetworkConstants;
import com.bee.passenger.airbnbmapexample.controller.BaseInterface;
import com.bee.passenger.airbnbmapexample.model.AddressModel;
import com.bee.passenger.airbnbmapexample.ui.adapter.RecyclerViewLiteModeAdapter;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewLiteModeMapActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private RecyclerViewLiteModeAdapter mAdapter;
    private List<AddressModel> addresses = new ArrayList<AddressModel>();
    private AddressModel mAddressModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view_lite_mode_map);
        getSupportActionBar().setTitle("Map View Lite Mode");

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_list);
        mRecyclerView.setHasFixedSize(false);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new RecyclerViewLiteModeAdapter(addresses, this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.requestLayout();


        mAddressModel=new AddressModel(this, new BaseInterface() {
            @Override
            public void handleNetworkCall(Object object, int requestCode) {
                if (requestCode == NetworkConstants.ADDRESS_REQUEST) {
                    if (object instanceof ArrayList) {
                        addresses = new ArrayList<>();
                        addresses = (ArrayList) object;
                        mAdapter = new RecyclerViewLiteModeAdapter(addresses, RecyclerViewLiteModeMapActivity.this);
                        mRecyclerView.setAdapter(mAdapter);
                        mAdapter.notifyDataSetChanged();
                        mRecyclerView.requestLayout();
                    }
                    else{
                        Toast.makeText(RecyclerViewLiteModeMapActivity.this, (String)object, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        mAddressModel.fetchAddressFromServer();
    }
}
