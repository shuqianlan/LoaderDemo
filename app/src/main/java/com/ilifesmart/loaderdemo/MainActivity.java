package com.ilifesmart.loaderdemo;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;

public class MainActivity extends AppCompatActivity {


    // 花了将近一个小时看源码，整个流程串完发现居然在Android 28中已被抛弃,
    // 拥抱MVVM+ViewModel+LiveData吧，骚年!
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager manager = getSupportFragmentManager();
        CursorLoaderListFragment fragment = new CursorLoaderListFragment();
        manager.beginTransaction().add(R.id.root, fragment).commit();
    }

    public static class CursorLoaderListFragment extends ListFragment implements SearchView.OnQueryTextListener, SearchView.OnCloseListener
    , LoaderManager.LoaderCallbacks<Cursor> {

        SimpleCursorAdapter mAdapter;
        SearchView mSearchView;
        String mCurFilter;

        static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.CONTACT_STATUS,
                ContactsContract.Contacts.CONTACT_PRESENCE,
                ContactsContract.Contacts.PHOTO_ID,
                ContactsContract.Contacts.LOOKUP_KEY,
        };

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            setEmptyText("No phone Number");
            setHasOptionsMenu(true);
//            SimpleAdapter // 给与静态数据调用.
            mAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_2, null, new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.CONTACT_STATUS
            }, new int[] {android.R.id.text1, android.R.id.text2}, 0);

            setListAdapter(mAdapter); // 关联Adapter

            // 显示进度条
            setListShown(false);

            // LoaderManager会监视数据改变，用户本身不需要再监视数据改变事件.
            // 触发onCreateLoader
            // start调用也在此. 界面onStarted以后.
            getActivity().getSupportLoaderManager().initLoader(0, null, this); //
        }

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
            // 仅调用一次，后期会re-used
            Uri baseUri;
            if (mCurFilter != null) {
                baseUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(mCurFilter));
            } else {
                baseUri = ContactsContract.Contacts.CONTENT_URI;
            }

            String select = "((" + ContactsContract.Contacts.DISPLAY_NAME + " NOTNULL) AND ("
                    + ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1) AND ("
                    + ContactsContract.Contacts.DISPLAY_NAME + " != ''))";

            return new CursorLoader(getActivity(), baseUri, CONTACTS_SUMMARY_PROJECTION, select, null, null);
        }

        // 返回结果，通过AsyncTask的doInbackground()触发onPostExecute(), 最终通过LoadManager的onComplete到达此处.
        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
            setListShown(true);
            mAdapter.swapCursor(cursor);
        }

        // 触发下次调用前会先cancelLoad，最终会调到此. 此处用来清理之前的Cursor信息.
        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }

        @Override
        public boolean onClose() {
            if (!TextUtils.isEmpty(mSearchView.getQuery())) {
                mSearchView.setQuery(null, true);
            }
            return true;
        }

        @Override
        public boolean onQueryTextSubmit(String s) {
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            String newFilter = !TextUtils.isEmpty(s) ?s : null;
            if (mCurFilter == null && newFilter == null) {
                return true;
            }

            if (mCurFilter != null && mCurFilter.equalsIgnoreCase(newFilter)) {
                return true;
            }

            mCurFilter = newFilter;
            getLoaderManager().restartLoader(0, null, this);
            return false;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            MenuItem item = menu.add("Search");
            item.setIcon(android.R.drawable.ic_menu_search);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            mSearchView = new MySearchView(getActivity());
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setOnCloseListener(this);
            mSearchView.setIconifiedByDefault(true);
            item.setActionView(mSearchView);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Log.d("FragmentCompleList", "onListItemClick: Item clicked " + id);
            super.onListItemClick(l, v, position, id);
        }
    }

    public static class MySearchView extends SearchView {

        public MySearchView(Context context) {
            super(context);
        }

        @Override
        public void onActionViewCollapsed() {
            setQuery("", false);
            super.onActionViewCollapsed();
        }
    }



}
