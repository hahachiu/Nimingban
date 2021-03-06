/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.nimingban.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.nimingban.NMBApplication;
import com.hippo.nimingban.R;
import com.hippo.nimingban.client.NMBClient;
import com.hippo.nimingban.client.NMBRequest;
import com.hippo.nimingban.client.ac.data.ACSearchItem;
import com.hippo.nimingban.client.data.ACSite;
import com.hippo.nimingban.widget.ContentLayout;
import com.hippo.rippleold.RippleSalon;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.widget.recyclerview.MarginItemDecoration;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.ResourcesUtils;

import java.util.List;

public class SearchActivity extends AbsActivity implements EasyRecyclerView.OnItemClickListener {

    public static final String ACTION_SEARCH = "com.hippo.nimingban.ui.SearchActivity.action.SEARCH";

    public static final String KEY_KEYWORD = "keyword";

    private NMBClient mNMBClient;

    private SearchAdapter mSearchAdapter;
    private SearchHelper mSearchHelper;

    private NMBRequest mNMBRequest;

    private String mKeyword;

    @Override
    protected int getLightThemeResId() {
        return R.style.AppTheme;
    }

    @Override
    protected int getDarkThemeResId() {
        return R.style.AppTheme_Dark;
    }

    // false for error
    private boolean handlerIntent(Intent intent) {
        if (intent == null) {
            return false;
        }

        String action = intent.getAction();
        if (ACTION_SEARCH.equals(action)) {
            String keyword = intent.getStringExtra(KEY_KEYWORD);
            if (keyword != null) {
                mKeyword = keyword;
                setTitle(getString(R.string.search_title, keyword));
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!handlerIntent(getIntent())) {
            finish();
            return;
        }

        mNMBClient = NMBApplication.getNMBClient(this);

        setContentView(R.layout.activity_search);

        ContentLayout contentLayout = (ContentLayout) findViewById(R.id.content_layout);
        EasyRecyclerView recyclerView = contentLayout.getRecyclerView();

        mSearchHelper = new SearchHelper();
        mSearchHelper.setEmptyString(getString(R.string.not_found));
        contentLayout.setHelper(mSearchHelper);

        mSearchAdapter = new SearchAdapter();
        recyclerView.setAdapter(mSearchAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setSelector(RippleSalon.generateRippleDrawable(ResourcesUtils.getAttrBoolean(this, R.attr.dark)));
        recyclerView.setDrawSelectorOnTop(true);
        recyclerView.setOnItemClickListener(this);
        recyclerView.hasFixedSize();
        recyclerView.setClipToPadding(false);
        int halfInterval = LayoutUtils.dp2pix(this, 4);
        recyclerView.addItemDecoration(new MarginItemDecoration(halfInterval));
        recyclerView.setPadding(halfInterval, halfInterval, halfInterval, halfInterval);

        mSearchHelper.firstRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mNMBRequest != null) {
            mNMBRequest.cancel();
            mNMBRequest = null;
        }
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        Intent intent = new Intent(this, PostActivity.class);
        intent.setAction(PostActivity.ACTION_SITE_ID);
        intent.putExtra(PostActivity.KEY_SITE, ACSite.getInstance().getId());
        intent.putExtra(PostActivity.KEY_ID, mSearchHelper.getDataAt(position).id);
        startActivity(intent);
        return true;
    }

    private class SearchHolder extends RecyclerView.ViewHolder {

        public TextView leftText;
        private TextView content;

        public SearchHolder(View itemView) {
            super(itemView);

            leftText = (TextView) itemView.findViewById(R.id.left_text);
            content = (TextView) itemView.findViewById(R.id.content);
        }
    }

    private class SearchAdapter extends RecyclerView.Adapter<SearchHolder> {

        @Override
        public SearchHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SearchHolder(getLayoutInflater().inflate(R.layout.item_search, parent, false));
        }

        @Override
        public void onBindViewHolder(SearchHolder holder, int position) {
            ACSearchItem item = mSearchHelper.getDataAt(position);
            holder.leftText.setText("No." + item.id);
            holder.content.setText(item.context);
        }

        @Override
        public int getItemCount() {
            return mSearchHelper.size();
        }
    }

    private class SearchHelper extends ContentLayout.ContentHelper<ACSearchItem> {

        @Override
        protected void getPageData(int taskId, int type, int page) {
            if (mNMBRequest != null) {
                mNMBRequest.cancel();
                mNMBRequest = null;
            }

            NMBRequest request = new NMBRequest();
            mNMBRequest = request;
            request.setSite(ACSite.getInstance());
            request.setMethod(NMBClient.METHOD_SEARCH);
            request.setArgs(mKeyword, page);
            request.setCallback(new SearchListener(taskId, type, page));
            mNMBClient.execute(request);
        }

        @Override
        protected Context getContext() {
            return SearchActivity.this;
        }

        @Override
        protected void notifyDataSetChanged() {
            mSearchAdapter.notifyDataSetChanged();
        }

        @Override
        protected void notifyItemRangeRemoved(int positionStart, int itemCount) {
            mSearchAdapter.notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        protected void notifyItemRangeInserted(int positionStart, int itemCount) {
            mSearchAdapter.notifyItemRangeInserted(positionStart, itemCount);
        }
    }

    private class SearchListener implements NMBClient.Callback<List<ACSearchItem>> {

        private int mTaskId;
        private int mTaskType;
        private int mPage;

        public SearchListener(int taskId, int taskType, int page) {
            mTaskId = taskId;
            mTaskType = taskType;
            mPage = page;
        }

        @Override
        public void onSuccess(List<ACSearchItem> result) {
            // Clear
            mNMBRequest = null;

            if (result.isEmpty()) {
                mSearchHelper.onGetEmptyData(mTaskId);
                if (mTaskType == ContentLayout.ContentHelper.TYPE_NEXT_PAGE ||
                        mTaskType == ContentLayout.ContentHelper.TYPE_NEXT_PAGE_KEEP_POS) {
                    mSearchHelper.setPages(mPage);
                } else {
                    mSearchHelper.setPages(0);
                }
            } else {
                mSearchHelper.onGetPageData(mTaskId, result);
                mSearchHelper.setPages(Integer.MAX_VALUE);
            }
        }

        @Override
        public void onFailure(Exception e) {
            // Clear
            mNMBRequest = null;
            mSearchHelper.onGetExpection(mTaskId, e);
        }

        @Override
        public void onCancelled() {
            // Clear
            mNMBRequest = null;
        }
    }
}
