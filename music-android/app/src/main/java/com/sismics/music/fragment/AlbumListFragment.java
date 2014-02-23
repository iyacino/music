package com.sismics.music.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.androidquery.AQuery;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.sismics.music.R;
import com.sismics.music.event.MyMusicMenuVisibilityChanged;
import com.sismics.music.event.OpenAlbumEvent;
import com.sismics.music.model.Album;
import com.sismics.music.resource.AlbumResource;
import com.sismics.music.adapter.AlbumAdapter;
import com.sismics.music.util.CacheUtil;
import com.sismics.music.util.PreferenceUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * Albums list fragments.
 *
 * @author bgamard
 */
public class AlbumListFragment extends Fragment {

    private AQuery aq;

    /**
     * Returns a new instance of this fragment.
     */
    public static AlbumListFragment newInstance() {
        AlbumListFragment fragment = new AlbumListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.my_music, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                refreshAlbumList();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the view
        View view = inflater.inflate(R.layout.fragment_album_list, container, false);
        aq = new AQuery(view);

        refreshAlbumList();

        // Clear the search input
        aq.id(R.id.clearSearch).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aq.id(R.id.search).text("");
            }
        });

        // Filter the albums when the search input changes
        aq.id(R.id.search).getEditText()
                .addTextChangedListener(new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        AlbumAdapter adapter = (AlbumAdapter) aq.id(R.id.listAlbum).getListView().getAdapter();
                        adapter.getFilter().filter(s);
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

        // Open the album details on click
        aq.id(R.id.listAlbum).itemClicked(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AlbumAdapter adapter = (AlbumAdapter) aq.id(R.id.listAlbum).getListView().getAdapter();
                EventBus.getDefault().post(new OpenAlbumEvent(new Album(adapter.getItem(position))));
            }
        });

        EventBus.getDefault().register(this);
        return view;
    }

    /**
     * Refresh album list.
     */
    private void refreshAlbumList() {
        // Get cached albums
        final Set<String> cachedAlbumSet = CacheUtil.getCachedAlbumSet();

        // Grab the data from the cache first
        JSONObject cache = PreferenceUtil.getCachedJson(getActivity(), PreferenceUtil.Pref.CACHED_ALBUMS_LIST_JSON);
        if (cache != null) {
            aq.id(R.id.listAlbum).adapter(new AlbumAdapter(getActivity(), cache.optJSONArray("albums"), cachedAlbumSet));
        }

        // Download the album list from server
        AlbumResource.list(getActivity(), new JsonHttpResponseHandler() {
            public void onSuccess(final JSONObject json) {
                if (getActivity() == null) {
                    // The activity is dead, and this fragment has been detached
                    return;
                }

                // Cache the albums list
                ListView listView = aq.id(R.id.listAlbum).getListView();
                JSONArray albums = json.optJSONArray("albums");
                PreferenceUtil.setCachedJson(getActivity(), PreferenceUtil.Pref.CACHED_ALBUMS_LIST_JSON, json);

                // Clear the search input
                aq.id(R.id.search).text("");

                // Publish the new albums to the adapter
                AlbumAdapter adapter = (AlbumAdapter) listView.getAdapter();
                if (adapter != null) {
                    adapter.setAlbums(albums);
                } else {
                    listView.setAdapter(new AlbumAdapter(getActivity(), albums, cachedAlbumSet));
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    /**
     * My music menu visibility fragment has changed.
     * @param event Event
     */
    public void onEvent(MyMusicMenuVisibilityChanged event) {
        setMenuVisibility(event.isMenuVisible());
    }
}