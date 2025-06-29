package com.example.eventcountdownwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import androidx.core.content.ContextCompat;

import com.example.eventcountdownwidget.utils.ColorUtil;

import java.util.ArrayList;
import java.util.List;

// Removed unused imports


/**
 * Service that provides the RemoteViewsFactory for the event list widget's ListView.
 */
public class EventListWidgetService extends RemoteViewsService {
    private static final String TAG = "EventListWidgetService";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new EventListRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    /**
     * Factory responsible for creating and populating RemoteViews for each list item.
     */
    class EventListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private final Context mContext;
        private final int mAppWidgetId;
        private List<SimpleEventListWidgetProvider.CalendarEventItem> mEventList = new ArrayList<>();
        private SimpleEventListWidgetProvider.WidgetConfig mWidgetConfig;

        public EventListRemoteViewsFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // onCreate, onDataSetChanged, onDestroy, loadWidgetConfig, loadEvents (Keep as is)
        @Override public void onCreate() { loadWidgetConfig(); loadEvents(); }
        @Override public void onDataSetChanged() {loadWidgetConfig(); loadEvents(); }
        @Override public void onDestroy() { mEventList.clear(); }
        private void loadWidgetConfig() { mWidgetConfig = SimpleEventListWidgetProvider.WidgetConfigManager.loadConfig(mContext, mAppWidgetId); if (mWidgetConfig == null) { mWidgetConfig = new SimpleEventListWidgetProvider.WidgetConfig(mAppWidgetId, 0, -1, 10); } }
        private void loadEvents() { if (mWidgetConfig != null) { mEventList = SimpleEventListWidgetProvider.CalendarRepository.loadEvents(mContext, mWidgetConfig.getMaxEvents()); } else { mEventList.clear(); } }

        @Override
        public int getCount() { return mEventList.size(); }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= mEventList.size() || mEventList.get(position) == null) {
                return new RemoteViews(mContext.getPackageName(), R.layout.event_list_widget_item); // Return empty
            }

            try {
                SimpleEventListWidgetProvider.CalendarEventItem event = mEventList.get(position);
                RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.event_list_widget_item);

                // Populate views
                rv.setTextViewText(R.id.event_item_title, event.getTitle());
                String subtitle = SimpleEventListWidgetProvider.CountdownFormatter.formatCountdown(event);
                rv.setTextViewText(R.id.event_item_subtitle, subtitle);

                // Apply theme
                if (mWidgetConfig != null) { applyItemTheme(rv, mWidgetConfig); }
                else { rv.setTextColor(R.id.event_item_title, Color.WHITE); rv.setTextColor(R.id.event_item_subtitle, Color.LTGRAY); }

                return rv;
            } catch (Exception e) {
                RemoteViews errorRv = new RemoteViews(mContext.getPackageName(), R.layout.event_list_widget_item);
                errorRv.setTextViewText(R.id.event_item_title, "Error");
                errorRv.setTextViewText(R.id.event_item_subtitle, "Cannot load");
                errorRv.setTextColor(R.id.event_item_title, Color.RED);
                return errorRv;
            }
        }

        // applyItemTheme (Keep as is)
        private void applyItemTheme(RemoteViews rv, SimpleEventListWidgetProvider.WidgetConfig config) {
            int titleColor; int subtitleColor;
            if (config.getThemeStyle() == 1) { titleColor=ContextCompat.getColor(mContext, R.color.widget_text_on_light); subtitleColor=ContextCompat.getColor(mContext, R.color.lightTextSecondary); }
            else if (config.getThemeStyle() == 2) { titleColor=ContextCompat.getColor(mContext, R.color.widget_text_on_dark); subtitleColor=ContextCompat.getColor(mContext, R.color.darkTextSecondary); }
            else { int baseBg = (config.getWidgetColor() != -1) ? config.getWidgetColor() : Color.parseColor("#303030"); boolean dark = ColorUtil.isDarkColor(baseBg); titleColor = dark ? Color.WHITE : Color.BLACK; subtitleColor = dark ? Color.argb(200, 255, 255, 255) : Color.DKGRAY; }
            rv.setTextColor(R.id.event_item_title, titleColor); rv.setTextColor(R.id.event_item_subtitle, subtitleColor);
        }

        // getLoadingView, getViewTypeCount, getItemId, hasStableIds (Keep as is)
        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public long getItemId(int pos) { return (pos>=0 && pos<mEventList.size() && mEventList.get(pos)!=null) ? mEventList.get(pos).getId() : pos; }
        @Override public boolean hasStableIds() { return true; }

    } // End of Factory
} // End of Service