package com.jokuskay.rss_reader.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.Toast;
import com.jokuskay.rss_reader.R;
import com.jokuskay.rss_reader.adapters.RssAdapter;
import com.jokuskay.rss_reader.dialogs.AddRssDialog;
import com.jokuskay.rss_reader.models.Rss;
import com.jokuskay.rss_reader.services.GetPostsService;

import java.util.List;

public class RssListActivity extends ActionBarActivity implements
        AddRssDialog.OnAddRssDialogListener,
        RssAdapter.OnRssListActionListener {

    private static final String TAG = "RssListActivity";

    private RecyclerView.Adapter mAdapter;

    private SwipeRefreshLayout mRefresh;
    private Button mAddRssBtn;

    private List<Rss> mRssList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_rss_list);

        mAddRssBtn = (Button) findViewById(R.id.floating_button);
        mRefresh = (SwipeRefreshLayout) findViewById(R.id.rss_refresh);
        RecyclerView listView = (RecyclerView) findViewById(R.id.rss_list);

        listView.setHasFixedSize(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(mLayoutManager);
        listView.setItemAnimator(new DefaultItemAnimator());

        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRssList = Rss.getAll(RssListActivity.this);
                mAdapter.notifyDataSetChanged();
                mRefresh.setRefreshing(false);
            }
        });

        mAddRssBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AddRssDialog().show(getSupportFragmentManager(), TAG);
            }
        });

        mRssList = Rss.getAll(this);
        mAdapter = new RssAdapter(this, mRssList);

        listView.setAdapter(mAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(getPackageName()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    public void onAddRssDialogOpen() {
        mAddRssBtn.setVisibility(View.GONE);
    }

    @Override
    public void onAddRssDialogClose() {
        mAddRssBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAddRssDialogAction(String url) {
        onAddRssDialogClose();

        if (URLUtil.isValidUrl(url)) {
            mRefresh.setRefreshing(true);

            Intent service = new Intent(RssListActivity.this, GetPostsService.class);
            service.putExtra(Rss.Columns.url.name(), url);
            startService(service);

        } else {
            Toast.makeText(this, "URL is not valid", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(this, PostListActivity.class);
        intent.putExtra("rss", mRssList.get(position));
        startActivity(intent);
    }

    @Override
    public void onItemLongClick(int position) {
        mRssList.get(position).remove(this);
        mRssList.remove(position);
        mAdapter.notifyItemRemoved(position);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mRefresh.setRefreshing(false);

            Rss rss = (Rss) intent.getExtras().getSerializable("rss");
            if (rss != null) {
                mRssList.add(rss);
                mAdapter.notifyItemInserted(mRssList.size() - 1);
            } else {
                Toast.makeText(RssListActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }

        }
    };

}
