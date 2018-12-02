package sheasmith.me.betterkamar.pages.timetable;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.format.TitleFormatter;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import sheasmith.me.betterkamar.KamarPlusApplication;
import sheasmith.me.betterkamar.R;
import sheasmith.me.betterkamar.dataModels.AbsenceObject;
import sheasmith.me.betterkamar.dataModels.AttendanceObject;
import sheasmith.me.betterkamar.dataModels.CalendarObject;
import sheasmith.me.betterkamar.dataModels.EventsObject;
import sheasmith.me.betterkamar.dataModels.GlobalObject;
import sheasmith.me.betterkamar.dataModels.LoginObject;
import sheasmith.me.betterkamar.dataModels.TimetableObject;
import sheasmith.me.betterkamar.internalModels.ApiResponse;
import sheasmith.me.betterkamar.internalModels.Exceptions;
import sheasmith.me.betterkamar.internalModels.PortalObject;
import sheasmith.me.betterkamar.util.ApiManager;
import sheasmith.me.betterkamar.util.OnSwipeTouchListener;

public class TimetableFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private TimetableAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ProgressBar mLoader;
    private TextView noEvents;
    private TextView status;
    private MaterialCalendarView mCalendarView;

    private ArrayList<GlobalObject.PeriodDefinition> periodDefinitions;
    private ArrayList<AttendanceObject.Week> attendanceResults;
    private ArrayList<EventsObject.Event> events;
    private ArrayList<TimetableObject.Week> timetable;
    private ArrayList<CalendarObject.Day> days;
    private AbsenceObject absenceStats;
    private Date lastDate;
    private PortalObject mPortal;
    private Tracker mTracker;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setElevation(10);
        getActivity().setTitle("Timetable");
        if (mAdapter == null ||  mAdapter.getItemCount() == 0)
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doRequest(mPortal, false);
                }
            }).start();
        getActivity().invalidateOptionsMenu();

        KamarPlusApplication application = (KamarPlusApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName("Timetable");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public static TimetableFragment newInstance() {
        return new TimetableFragment();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("periodDefinitions", periodDefinitions);
        outState.putSerializable("attendanceResults", attendanceResults);
        outState.putSerializable("events", events);
        outState.putSerializable("timetable", timetable);
        if (mCalendarView.getSelectedDate() != null)
        outState.putSerializable("selectedDate", new Date(mCalendarView.getSelectedDate().getDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        outState.putSerializable("days", days);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            periodDefinitions = (ArrayList<GlobalObject.PeriodDefinition>) savedInstanceState.getSerializable("periodDefinitions");
            attendanceResults = (ArrayList<AttendanceObject.Week>) savedInstanceState.getSerializable("attendanceResults");
            events = (ArrayList<EventsObject.Event>) savedInstanceState.getSerializable("events");
            timetable = (ArrayList<TimetableObject.Week>) savedInstanceState.getSerializable("timetable");
            days = (ArrayList<CalendarObject.Day>) savedInstanceState.getSerializable("days");

            if (savedInstanceState.containsKey("selectedDate"))
            lastDate = (Date) savedInstanceState.getSerializable("selectedDate");
        }

        mPortal = (PortalObject) getActivity().getIntent().getSerializableExtra("portal");
        ApiManager.setVariables(mPortal, getContext());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Attendance Statistics").setPositiveButton("close", null);
            if (absenceStats == null) {
                builder.setMessage("Please wait for the timetable to load");
            }
            else {
                AbsenceObject.Student student = absenceStats.StudentAbsenceStatsResults.Student;
                builder.setMessage(String.format("Unjustified: %s%%\nJustified: %s%%\nOverseas: %s%%\nTotal Absences: %s%%\nTotal Present: %s%%", student.PctgeU, student.PctgeJ, student.PctgeO, student.PctgeT, student.PctgeP));
            }
            builder.create().show();
        }
        else if (item.getItemId() == 2) {
            LayoutInflater inflater = getLayoutInflater();
            View contentView = inflater.inflate(R.layout.timetable_info, null);

            new AlertDialog.Builder(getContext())
                    .setTitle("Attendance Codes (based on MoE rules)")
                    .setView(contentView)
                    .setPositiveButton("Close", null)
                    .create()
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(0, 1, 0, "Attendance Stats").setIcon(R.drawable.ic_chart).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 2, 0, "Attendance Info").setIcon(R.drawable.ic_info).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timetable, container, false);
        mLoader = view.findViewById(R.id.progressBar);
        noEvents = view.findViewById(R.id.noEvents);
        status = view.findViewById(R.id.schoolStatus);
        setHasOptionsMenu(true);

        mRecyclerView = view.findViewById(R.id.events);
        mRecyclerView.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeRight() {
                mCalendarView.setCurrentDate(mCalendarView.getSelectedDate().getDate().minusDays(1));
                mCalendarView.setSelectedDate(mCalendarView.getSelectedDate().getDate().minusDays(1));
                final Date date = new Date(mCalendarView.getSelectedDate().getDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
                updateList(date);
            }

            @Override
            public void onSwipeLeft() {
                mCalendarView.setCurrentDate(mCalendarView.getSelectedDate().getDate().plusDays(1));
                mCalendarView.setSelectedDate(mCalendarView.getSelectedDate().getDate().plusDays(1));
                final Date date = new Date(mCalendarView.getSelectedDate().getDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
                updateList(date);
            }
        });

        noEvents.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeRight() {
                mCalendarView.setCurrentDate(mCalendarView.getSelectedDate().getDate().minusDays(1));
                mCalendarView.setSelectedDate(mCalendarView.getSelectedDate().getDate().minusDays(1));
                final Date date = new Date(mCalendarView.getSelectedDate().getDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
                updateList(date);
            }

            @Override
            public void onSwipeLeft() {
                mCalendarView.setCurrentDate(mCalendarView.getSelectedDate().getDate().plusDays(1));
                mCalendarView.setSelectedDate(mCalendarView.getSelectedDate().getDate().plusDays(1));
                final Date date = new Date(mCalendarView.getSelectedDate().getDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
                updateList(date);
            }
        });

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(false);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        Date now = new Date(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);

        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        Date min = cal.getTime();

        cal.set(Calendar.MONTH, 11);
        cal.set(Calendar.DAY_OF_MONTH, 31);

        Date max = cal.getTime();

        mCalendarView = view.findViewById(R.id.calendarView);
//        mCalendarView.setCurrentDate(new Date(System.currentTimeMillis()));
        mCalendarView.state().edit().setMaximumDate(Instant.ofEpochMilli(max.getTime()).atZone(ZoneId.systemDefault()).toLocalDate()).setMinimumDate(Instant.ofEpochMilli(min.getTime()).atZone(ZoneId.systemDefault()).toLocalDate()).commit();

        mCalendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay calDate, boolean selected) {
                final Date date = new Date(calDate.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
                updateList(date);
            }
        });

        if (lastDate != null) {
            mCalendarView.setSelectedDate(Instant.ofEpochMilli(lastDate.getTime()).atZone(ZoneId.systemDefault()).toLocalDate());
            updateList(lastDate);
        }

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doRequest(mPortal, true);
            }
        });

        return view;
    }


    private void doRequest(final PortalObject portal, final boolean ignoreCache) {
        final boolean[] finished = new boolean[]{false, false, false, false, false, false};

        ApiManager.getGlobals(new ApiResponse<GlobalObject>() {
            @Override
            public void success(GlobalObject value) {
                if (getActivity() == null)
                    return;
                periodDefinitions = value.GlobalsResults.PeriodDefinitions;
                finished[0] = true;
                hideLoader(finished);
            }

            @Override
            public void error(Exception e) {
                e.printStackTrace();
                finished[0] = true;
                handleError(portal, e, ignoreCache);
            }

        }, ignoreCache);

        ApiManager.getAttendance(new ApiResponse<AttendanceObject>() {
            @Override
            public void success(AttendanceObject value) {
                if (getActivity() == null)
                    return;
                attendanceResults = value.StudentAttendanceResults.Weeks;
                finished[1] = true;
                hideLoader(finished);
            }

            @Override
            public void error(Exception e) {
                e.printStackTrace();
                finished[1] = true;
                handleError(portal, e, ignoreCache);
            }
        }, ignoreCache);

        ApiManager.getEvents(new ApiResponse<EventsObject>() {
            @Override
            public void success(EventsObject value) {
                if (getActivity() == null)
                    return;
                events = value.EventsResults.Events;
                finished[2] = true;
                hideLoader(finished);
            }

            @Override
            public void error(Exception e) {
                e.printStackTrace();
                finished[2] = true;
                handleError(portal, e, ignoreCache);
            }
        }, new Date(System.currentTimeMillis()).getYear(), ignoreCache);

        ApiManager.getTimetable(new ApiResponse<TimetableObject>() {
            @Override
            public void success(TimetableObject value) {
                if (getActivity() == null)
                    return;
                timetable = value.StudentTimetableResults.Student.Timetable;
                finished[3] = true;
                hideLoader(finished);
            }

            @Override
            public void error(Exception e) {
                e.printStackTrace();
                finished[3] = true;

                handleError(portal, e, ignoreCache);
            }
        }, ignoreCache);

        ApiManager.getCalendar(new ApiResponse<CalendarObject>() {
            @Override
            public void success(CalendarObject value) {
                if (getActivity() == null)
                    return;
                days = value.CalendarResults.Days;
                finished[4] = true;
                hideLoader(finished);
            }

            @Override
            public void error(Exception e) {
                e.printStackTrace();
                finished[4] = true;

                handleError(portal, e, ignoreCache);
            }
        }, ignoreCache);

        ApiManager.getAbsenceStats(new ApiResponse<AbsenceObject>() {
            @Override
            public void success(AbsenceObject value) {
                if (getActivity() == null)
                    return;
                absenceStats = value;
                finished[5] = true;
                hideLoader(finished);
            }

            @Override
            public void error(Exception e) {
                e.printStackTrace();
                finished[5] = true;

                handleError(portal, e, ignoreCache);
            }
        }, ignoreCache);
    }

    private void handleError(final PortalObject portal, Exception e, final boolean ignoreCache) {
        if (e instanceof Exceptions.ExpiredToken) {
            ApiManager.login(portal.username, portal.password, new ApiResponse<LoginObject>() {
                @Override
                public void success(LoginObject value) {
                    doRequest(portal, ignoreCache);
                }

                @Override
                public void error(Exception e) {
                    e.printStackTrace();
                }
            });
            return;
        } else if (e instanceof IOException) {
            if (getActivity() == null)
                return;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(getContext())
                            .setTitle("No Internet")
                            .setMessage("You do not appear to be connected to the internet. Please check your connection and try again.")
                            .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    doRequest(portal, ignoreCache);
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

    private void hideLoader(boolean[] finished) {
        for (boolean f : finished) {
            if (!f)
                return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoader.setVisibility(View.GONE);
                mSwipeRefreshLayout.setRefreshing(false);
                // So we get the weeks to show up
                mCalendarView.setCurrentDate(mCalendarView.getCurrentDate().getDate().minusWeeks(1));
                mCalendarView.setCurrentDate(mCalendarView.getCurrentDate().getDate().plusWeeks(1));
                mCalendarView.setSelectedDate(Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDate());
                updateList(new Date(System.currentTimeMillis()));
            }
        });
    }

    private void updateList(final Date date) {
        Calendar current = Calendar.getInstance();
        current.setTime(date);

        List<TimetableObject.Class> periods = new ArrayList<>();
        Calendar monday = Calendar.getInstance();
        monday.setTime(date);
        monday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        int weekNumber = 0;
        CalendarObject.Day thisDay = null;
        final Calendar temp = Calendar.getInstance();

        AttendanceObject.Week attendanceWeek = null;

        for (CalendarObject.Day day : days) {
            try {
                Date start = format.parse(day.Date);
                temp.setTime(start);
                if (current.get(Calendar.DAY_OF_YEAR) == temp.get(Calendar.DAY_OF_YEAR) && !day.WeekYear.equals("") && !day.Status.equals("Holidays")) {
                    weekNumber = Integer.parseInt(day.WeekYear);
                    for (AttendanceObject.Week aw : attendanceResults) {
                        if (Integer.parseInt(aw.index) == weekNumber) {
                            attendanceWeek = aw;
                            break;
                        }
                    }
                    thisDay = day;
                    break;
                }
                else if (current.get(Calendar.DAY_OF_YEAR) == temp.get(Calendar.DAY_OF_YEAR)) {
                    thisDay = day;
                    break;
                }

            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        if (thisDay != null) {
            final CalendarObject.Day finalThisDay = thisDay;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    if (finalThisDay.Week.equals("") || finalThisDay.DayTT.equals("")) {
                        status.setText(String.format("School Status: %s", finalThisDay.Status));
//                    }
//                    else {
//                        status.setText(String.format("School Status: %s. Term %s Week %s", finalThisDay.Status, finalThisDay.Term, finalThisDay.Week));
//                    }
                    mCalendarView.setTitleFormatter(new TitleFormatter() {
                        @Override
                        public CharSequence format(CalendarDay calendarDay) {
                            SimpleDateFormat form = new SimpleDateFormat("MMMM, yyyy");
                            CalendarObject.Day weekDay = null;
                            int dayOfYear = mCalendarView.getCurrentDate().getDate().getDayOfYear();
                            for (CalendarObject.Day day : days) {
                                try {
                                    Date start = format.parse(day.Date);
                                    temp.setTime(start);
                                    if (dayOfYear == temp.get(Calendar.DAY_OF_YEAR)) {
                                        weekDay = day;
                                    }

                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            if (weekDay.Status.equals("Holidays"))
                                return form.format(new Date(calendarDay.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()));
                            else
                                return form.format(new Date(calendarDay.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())) + "\nTerm " + weekDay.TermA + " Week " + weekDay.WeekA;
                        }
                    });
                    mCalendarView.invalidate();
                }
            });
        }

        if (weekNumber == 0) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.setVisibility(View.GONE);
                    noEvents.setVisibility(View.VISIBLE);
                }
            });
            return;
        }
        else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.setVisibility(View.VISIBLE);
                    noEvents.setVisibility(View.GONE);
                }
            });
        }

        AttendanceObject.Day attendanceDay = null;

        weekLoop:
        for (TimetableObject.Week week : timetable) {
            if (week.WeekNumber == weekNumber) {
                for (Integer day : week.Classes.keySet()) {
                    Calendar cal = Calendar.getInstance();
                    cal.setFirstDayOfWeek(Calendar.SUNDAY);
                    cal.setTime(date);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                    cal.add(Calendar.DAY_OF_WEEK, day);

                    if (cal.get(Calendar.DAY_OF_YEAR) == current.get(Calendar.DAY_OF_YEAR)) {
                        periods = week.Classes.get(day);
                        if (attendanceWeek != null) {
                            for (AttendanceObject.Day ad : attendanceWeek.Days) {
                                if (ad.index.equals(day.toString())) {
                                    attendanceDay = ad;
                                    break;
                                }
                            }
                        }
                        break weekLoop;
                    }
                }
            }
        }

        for (TimetableObject.Class c : periods) {
            if (attendanceDay != null) {
                try {
                    c.attendance = attendanceDay.content.charAt(periods.indexOf(c));
                }
                catch (IndexOutOfBoundsException e) {
                    c.attendance = '.';
                }
            }
            else
                break;
        }

        final List<EventsObject.Event> dayEvents = new ArrayList<>();

        for (EventsObject.Event event : events) {
            try {
                Date start = format.parse(event.Start);
                Date end = format.parse(event.Finish);

                Calendar c = Calendar.getInstance();
                c.setTime(start);
                int startDay = c.get(Calendar.DAY_OF_YEAR);

                c.setTime(end);
                int endDay = c.get(Calendar.DAY_OF_YEAR);

                c.setTime(date);
                int currentDay = c.get(Calendar.DAY_OF_YEAR);

                if (startDay >= currentDay && endDay <= currentDay) {
                    dayEvents.add(event);
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        if (periods == null) {
            throw new RuntimeException();
        }

        if (dayEvents.size() == 0 && periods.size() == 0) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.setVisibility(View.GONE);
                    noEvents.setVisibility(View.VISIBLE);
                }
            });
            return;
        }

        final List<TimetableObject.Class> finalPeriods = periods;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter = new TimetableAdapter(dayEvents, finalPeriods, periodDefinitions, getContext());
                mRecyclerView.setAdapter(mAdapter);
//                                                mLoader.setVisibility(View.GONE);
            }
        });
    }
}
