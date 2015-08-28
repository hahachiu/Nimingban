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

package com.hippo.nimingban.client;

import android.content.Context;
import android.os.AsyncTask;

import com.hippo.httpclient.HttpClient;
import com.hippo.httpclient.HttpRequest;
import com.hippo.nimingban.NMBApplication;
import com.hippo.nimingban.client.ac.ACEngine;
import com.hippo.nimingban.network.NMBHttpRequest;
import com.hippo.yorozuya.PriorityThreadFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NMBClient {

    public static final String TAG = NMBClient.class.getSimpleName();

    public static final int RANDOM = 0;
    public static final int AC = 1;
    public static final int KUKUKU = 2;
    public static final int SITE_MIN = AC;
    public static final int SITE_MAX = KUKUKU;

    public static final int METHOD_GET_POST_LIST = 1;

    public static final int METHOD_GET_POST = 2;

    public static final int METHOD_GET_REFERENCE = 3;

    private final ThreadPoolExecutor mRequestThreadPool;
    private final HttpClient mHttpClient;

    public NMBClient(Context context) {
        int poolSize = 3;
        BlockingQueue<Runnable> requestWorkQueue = new LinkedBlockingQueue<>();
        ThreadFactory threadFactory = new PriorityThreadFactory(TAG,
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mRequestThreadPool = new ThreadPoolExecutor(poolSize, poolSize,
                1L, TimeUnit.SECONDS, requestWorkQueue, threadFactory);
        mHttpClient = NMBApplication.getNMBHttpClient(context);
    }

    public void execute(NMBRequest request) {
        if (!request.isCanceled()) {
            Task task = new Task(request.method, request.site, request.callback);
            task.executeOnExecutor(mRequestThreadPool, request.args);
            request.task = task;
        } else {
            request.callback.onCancelled();
        }
    }

    class Task extends AsyncTask<Object, Void, Object> {

        private int mMethod;
        private int mSite;
        private Callback mCallback;
        private HttpRequest mHttpRequest;

        private boolean mStop;

        public Task(int method, int site, Callback callback) {
            mMethod = method;
            mSite = site;
            mCallback = callback;
            mHttpRequest = new NMBHttpRequest(mSite);
        }

        public void stop() {
            if (!mStop) {
                mStop = true;

                Status status = getStatus();
                if (status == Status.PENDING) {
                    cancel(false);

                    // If it is pending, onPostExecute will not be called,
                    // need to call mListener.onCanceled here
                    mCallback.onCancelled();

                    // Clear
                    mHttpRequest = null;
                    mCallback = null;
                } else if (status == Status.RUNNING) {
                    if (mHttpRequest != null) {
                        mHttpRequest.cancel();
                    }
                }
            }
        }

        private Object getPostList(Object... params) throws Exception {
            switch (mSite) {
                case AC:
                    return ACEngine.getPostList(mHttpClient, mHttpRequest, (String) params[0]);
                default:
                    return new IllegalStateException("Can't detect site " + mSite);
            }
        }

        private Object getPost(Object... params) throws Exception {
            switch (mSite) {
                case AC:
                    return ACEngine.getPost(mHttpClient, mHttpRequest, (String) params[0]);
                default:
                    return new IllegalStateException("Can't detect site " + mSite);
            }
        }

        private Object getReference(Object... params) throws Exception {
            switch (mSite) {
                case AC:
                    return ACEngine.getReference(mHttpClient, mHttpRequest, (String) params[0]);
                default:
                    return new IllegalStateException("Can't detect site " + mSite);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Object doInBackground(Object... params) {
            try {
                switch (mMethod) {
                    case METHOD_GET_POST_LIST:
                        return getPostList(params);
                    case METHOD_GET_POST:
                        return getPost(params);
                    case METHOD_GET_REFERENCE:
                        return getReference(params);
                    default:
                        return new IllegalStateException("Can't detect method " + mMethod);
                }
            } catch (Exception e) {
                return e;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof CancelledException) {
                mCallback.onCancelled();
            } else if (result instanceof Exception) {
                mCallback.onFailure((Exception) result);
            } else {
                mCallback.onSuccess(result);
            }

            // Clear
            mHttpRequest = null;
            mCallback = null;
        }
    }

    public interface Callback<E> {

        void onSuccess(E result);

        void onFailure(Exception e);

        void onCancelled();
    }
}