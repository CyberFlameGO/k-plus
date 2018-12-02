package sheasmith.me.betterkamar.pages.notices;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import sheasmith.me.betterkamar.KamarPlusApplication;
import sheasmith.me.betterkamar.R;
import sheasmith.me.betterkamar.dataModels.LoginObject;
import sheasmith.me.betterkamar.dataModels.NoticesObject;
import sheasmith.me.betterkamar.internalModels.ApiResponse;
import sheasmith.me.betterkamar.internalModels.Exceptions;
import sheasmith.me.betterkamar.internalModels.PortalObject;
import sheasmith.me.betterkamar.util.ApiManager;
import sheasmith.me.betterkamar.util.OnSwipeTouchListener;

import static android.content.Context.MODE_PRIVATE;
import static android.support.v4.util.Preconditions.checkArgument;

public class NoticesFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private NoticesAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ProgressBar mLoader;
    private PortalObject mPortal;

    private ArrayList<String> groups;
    private HashSet<String> mDisabled;
    private HashSet<String> mTempEnabled;

    private Date lastDate;

    private HashMap<Date, NoticesObject> notices = new HashMap<>();
    private Tracker mTracker;
    private TextView mCurrentDate;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public static NoticesFragment newInstance() {
        return new NoticesFragment();
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("notices", notices);
        outState.putSerializable("groups", groups);
        outState.putSerializable("disabled", mDisabled);
        outState.putSerializable("lastDate", lastDate);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getActivity().setTheme(R.style.NoActionBarShadow);
        super.onActivityCreated(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setElevation(0);
        getActivity().setTitle("Notices");

        if (mAdapter == null ||  mAdapter.getItemCount() == 0)
            doRequest(mPortal, new Date(System.currentTimeMillis()), false);

        KamarPlusApplication application = (KamarPlusApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("Notices");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(0, 1, 0, "Filter Notices").setIcon(R.drawable.ic_filter).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1)
            createFilterDiag(mPortal);

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final PortalObject portal = (PortalObject) getActivity().getIntent().getSerializableExtra("portal");
        mPortal = portal;
        ApiManager.setVariables(portal, getContext());

        if (savedInstanceState != null) {
            notices = (HashMap<Date, NoticesObject>) savedInstanceState.getSerializable("notices");
            groups = (ArrayList<String>) savedInstanceState.getSerializable("groups");
            mDisabled = (HashSet<String>) savedInstanceState.getSerializable("disabled");
            lastDate = (Date) savedInstanceState.getSerializable("lastDate");
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notices, container, false);
        mRecyclerView = view.findViewById(R.id.notices);
        setHasOptionsMenu(true);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(false);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                Calendar c = Calendar.getInstance();
                c.setTime(lastDate);
                c.add(Calendar.DAY_OF_YEAR, -1);
                Date newDate = c.getTime();
                mLoader.setVisibility(View.VISIBLE);
                doRequest(mPortal, newDate, false);
            }

            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                Calendar c = Calendar.getInstance();
                c.setTime(lastDate);
                c.add(Calendar.DAY_OF_YEAR, 1);
                Date newDate = c.getTime();
                mLoader.setVisibility(View.VISIBLE);
                doRequest(mPortal, newDate, false);
            }
        });

        mLoader = view.findViewById(R.id.loader);
        mCurrentDate = view.findViewById(R.id.current_date);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doRequest(mPortal, lastDate, true);
            }
        });

        view.findViewById(R.id.previous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar c = Calendar.getInstance();
                c.setTime(lastDate);
                c.add(Calendar.DAY_OF_YEAR, -1);
                Date newDate = c.getTime();
                mLoader.setVisibility(View.VISIBLE);
                doRequest(mPortal, newDate, false);
            }
        });

        view.findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar c = Calendar.getInstance();
                c.setTime(lastDate);
                c.add(Calendar.DAY_OF_YEAR, 1);
                Date newDate = c.getTime();
                mLoader.setVisibility(View.VISIBLE);
                doRequest(mPortal, newDate, false);
            }
        });

        SharedPreferences preferences = getActivity().getPreferences(MODE_PRIVATE);
        mDisabled = (HashSet<String>) preferences.getStringSet(mPortal.schoolFile.replace(".jpg", "_notices_disabled"), new HashSet<String>());

        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        if (lastDate != null) {
            NoticesObject value = notices.get(lastDate);

            mAdapter = new NoticesAdapter(value.NoticesResults.MeetingNotices.Meeting, value.NoticesResults.GeneralNotices.General, getContext(), mDisabled);
            mRecyclerView.setAdapter(mAdapter);
            mLoader.setVisibility(View.GONE);
        }

        return view;
    }

    private void doRequest(final PortalObject portal, final Date date, final boolean ignoreCache) {
        lastDate = date;
        SimpleDateFormat titleFormat = new SimpleDateFormat("EEEE, d'[p]' MMMM yyyy");
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int day = c.get(Calendar.DAY_OF_MONTH);
        String title = titleFormat.format(date).replace("[p]", getDayOfMonthSuffix(day));
        mCurrentDate.setText(title);
        if (!notices.containsKey(date) || ignoreCache)
            ApiManager.getNotices(new ApiResponse<NoticesObject>() {
                @Override
                public void success(final NoticesObject value) {
                    if (getActivity() == null)
                        return;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter = new NoticesAdapter(value.NoticesResults.MeetingNotices.Meeting, value.NoticesResults.GeneralNotices.General, getContext(), mDisabled);
                            mRecyclerView.setAdapter(mAdapter);
                            mLoader.setVisibility(View.GONE);
                            mRecyclerView.setVisibility(View.VISIBLE);
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });

                    notices.put(date, value);

                    groups = new ArrayList<>();
                    for (NoticesObject.General notice : value.NoticesResults.GeneralNotices.General) {
                        if (!groups.contains(notice.Level))
                            groups.add(notice.Level);
                    }

                    for (NoticesObject.Meeting notice : value.NoticesResults.MeetingNotices.Meeting) {
                        if (!groups.contains(notice.Level))
                            groups.add(notice.Level);
                    }

                }

                @Override
                public void error(Exception e) {
                    if (e instanceof Exceptions.ExpiredToken) {
                        ApiManager.login(portal.username, portal.password, new ApiResponse<LoginObject>() {
                            @Override
                            public void success(LoginObject value) {
                                doRequest(portal, date, ignoreCache);
                            }

                            @Override
                            public void error(Exception e) {
                                e.printStackTrace();
                            }
                        });
                        return;
                    }

                    else if (e instanceof IOException) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                    new AlertDialog.Builder(getContext())
                                            .setTitle("No Internet")
                                            .setMessage("You do not appear to be connected to the internet. Please check your connection and try again.")
                                            .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    doRequest(portal, date, ignoreCache);
                                                }
                                            })
                                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    getActivity().finish();
                                                }
                                            })
                                            .create()
                                            .show();
                                }
                            });
                        }
                    }
                    e.printStackTrace();
                }
            }, date, ignoreCache);
        else {
            NoticesObject value = notices.get(date);

            mAdapter = new NoticesAdapter(value.NoticesResults.MeetingNotices.Meeting, value.NoticesResults.GeneralNotices.General, getContext(), mDisabled);
            mRecyclerView.setAdapter(mAdapter);
            mLoader.setVisibility(View.GONE);

            groups = new ArrayList<>();
            for (NoticesObject.General notice : value.NoticesResults.GeneralNotices.General) {
                if (!groups.contains(notice.Level))
                    groups.add(notice.Level);
            }

            for (NoticesObject.Meeting notice : value.NoticesResults.MeetingNotices.Meeting) {
                if (!groups.contains(notice.Level))
                    groups.add(notice.Level);
            }
        }
    }



    private void createFilterDiag(final PortalObject portal) {
        final AlertDialog.Builder diag = new AlertDialog.Builder(getContext())
                .setTitle("Include Notices From");

        if (groups == null) {
            diag.setMessage("The notices are still loading. Please wait");
        }
        else {
            final SharedPreferences preferences = getActivity().getPreferences(MODE_PRIVATE);
            Set<String> disabled = preferences.getStringSet(portal.schoolFile.replace(".jpg", "_notices_disabled"), new HashSet<String>());
            boolean[] disabledBoolean = new boolean[groups.size()];
            for (String group : groups) {
                disabledBoolean[groups.indexOf(group)] = !disabled.contains(group);
            }
            mTempEnabled = mDisabled;
            diag.setMultiChoiceItems(groups.toArray(new CharSequence[groups.size()]), disabledBoolean, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i, boolean isChecked) {
                    if (!isChecked)
                        mTempEnabled.add(groups.get(i));
                    else
                        mTempEnabled.remove(groups.get(i));
                }
            });

            diag.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mDisabled = mTempEnabled;
                    preferences.edit().putStringSet(portal.schoolFile.replace(".jpg", "_notices_disabled"), mDisabled).apply();
                    mAdapter = new NoticesAdapter(mAdapter.meetingNotices, mAdapter.generalNotices, getContext(), mDisabled);
                    mRecyclerView.setAdapter(mAdapter);
                    mAdapter.notifyDataSetChanged();
                }
            });
        }

        diag.show();
    }

    String getDayOfMonthSuffix(final int n) {
        checkArgument(n >= 1 && n <= 31, "illegal day of month: " + n);
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }
}
