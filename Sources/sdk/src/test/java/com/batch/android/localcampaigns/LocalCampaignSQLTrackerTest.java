package com.batch.android.localcampaigns;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LocalCampaignSQLTrackerTest
{
    private Context appContext;
    private Field dbHelperField;
    private Field databaseField;

    @Before
    public void setUp() throws NoSuchFieldException
    {
        appContext = ApplicationProvider.getApplicationContext();

        dbHelperField = LocalCampaignsSQLTracker.class.getDeclaredField("dbHelper");
        databaseField = LocalCampaignsSQLTracker.class.getDeclaredField("database");

        dbHelperField.setAccessible(true);
        databaseField.setAccessible(true);
    }

    // Check if DBHelper was created in open()
    @Test
    public void testOpen() throws IllegalAccessException
    {
        LocalCampaignsSQLTracker tracker = new LocalCampaignsSQLTracker();

        Assert.assertNull(dbHelperField.get(tracker));
        tracker.open(appContext);
        Assert.assertNotNull(dbHelperField.get(tracker));
    }

    // Check if Database was closed in close()
    @Test
    public void testClose() throws IllegalAccessException, ViewTrackerUnavailableException
    {
        LocalCampaignsSQLTracker tracker = new LocalCampaignsSQLTracker();
        tracker.open(appContext);

        // This is going to init the database field
        tracker.getViewEvent("fake_event");

        SQLiteDatabase database = (SQLiteDatabase) databaseField.get(tracker);
        Assert.assertNotNull(database);

        tracker.close();

        Assert.assertNull(databaseField.get(tracker));
        Assert.assertFalse(database.isOpen());
    }

    @Test
    public void testTrackEventForCampaignIDAndCount() throws ViewTrackerUnavailableException
    {
        // Clear database
        appContext.deleteDatabase(LocalCampaignTrackDbHelper.DATABASE_NAME);

        LocalCampaignsSQLTracker tracker = new LocalCampaignsSQLTracker();
        tracker.open(appContext);

        final String FAKE_CAMPAIGN_ID_1 = "MyCampaign1";
        final String FAKE_CAMPAIGN_ID_2 = "MyCampaign2";

        // Never tracked
        Assert.assertEquals(0, tracker.getViewEvent(FAKE_CAMPAIGN_ID_1).count);

        // Track one time
        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);

        Assert.assertEquals(1, tracker.getViewEvent(FAKE_CAMPAIGN_ID_1).count);

        // Track three times
        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);
        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);
        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);

        Assert.assertEquals(4, tracker.getViewEvent(FAKE_CAMPAIGN_ID_1).count);

        // Track another campaign
        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_2);

        Assert.assertEquals(4, tracker.getViewEvent(FAKE_CAMPAIGN_ID_1).count);
        Assert.assertEquals(1, tracker.getViewEvent(FAKE_CAMPAIGN_ID_2).count);

        tracker.close();
    }

    @Test
    public void testCampaignLastOccurence() throws ViewTrackerUnavailableException
    {
        // Clear database
        appContext.deleteDatabase(LocalCampaignTrackDbHelper.DATABASE_NAME);

        LocalCampaignsSQLTracker tracker = new LocalCampaignsSQLTracker();
        tracker.open(appContext);

        final String FAKE_CAMPAIGN_ID_1 = "MyCampaign1";
        final String FAKE_CAMPAIGN_ID_2 = "MyCampaign2";

        // Never tracked
        Assert.assertEquals(0, tracker.campaignLastOccurrence(FAKE_CAMPAIGN_ID_1));

        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);
        long firstTrackTime = tracker.campaignLastOccurrence(FAKE_CAMPAIGN_ID_1);

        // Tracked, time != 0
        Assert.assertNotSame(0, firstTrackTime);

        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);

        // Tracked another time (time > to previous time)
        long secondTrackTime = tracker.campaignLastOccurrence(FAKE_CAMPAIGN_ID_1);

        System.out.println("times :: " + firstTrackTime + "    " + secondTrackTime);
        Assert.assertTrue(firstTrackTime < secondTrackTime);

        tracker.close();
    }

    @Test
    // Tests that the db being not opened results in a specific exception
    public void testUnavailabilityException()
    {
        LocalCampaignsSQLTracker tracker = new LocalCampaignsSQLTracker();
        try {
            tracker.trackViewEvent("foo");
        } catch (ViewTrackerUnavailableException expected) {
            return;
        }
        Assert.fail("A ViewTrackerUnavailableException should have been thrown");
    }
}
