package net.olejon.spotcommander;

/*

Copyright 2015 Ole Jon Bjørkum

This file is part of SpotCommander.

SpotCommander is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SpotCommander is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SpotCommander.  If not, see <http://www.gnu.org/licenses/>.

*/

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONObject;

import java.util.ArrayList;

public class DonateActivity extends ActionBarActivity
{
    private final MyTools mTools = new MyTools(this);

    private IInAppBillingService mIInAppBillingService;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Allow landscape?
        if(!mTools.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Layout
        setContentView(R.layout.activity_donate);

        // Toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.donate_toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // In-app billing
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    // Activity result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == 1)
        {
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            if(resultCode == RESULT_OK)
            {
                try
                {
                    JSONObject jo = new JSONObject(purchaseData);
                    String purchaseToken = jo.getString("purchaseToken");

                    consumeDonation(purchaseToken);

                    mTools.showToast(getString(R.string.donate_thank_you), 1);

                    finish();
                }
                catch(Exception e)
                {
                    mTools.showToast(getString(R.string.donate_something_went_wrong), 1);

                    Log.e("DonateActivity", Log.getStackTraceString(e));
                }
            }
        }
    }

    // Destroy activity
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mServiceConnection != null) unbindService(mServiceConnection);
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_donate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
            {
                NavUtils.navigateUpFromSameTask(this);
                return true;
            }
            case R.id.donate_menu_reset:
            {
                resetDonations();
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // Donations
    private void makeDonation(String product)
    {
        try
        {
            Bundle buyIntentBundle = mIInAppBillingService.getBuyIntent(3, getPackageName(), product, "inapp", "");
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

            startIntentSenderForResult(pendingIntent.getIntentSender(), 1, new Intent(), 0, 0, 0);
        }
        catch(Exception e)
        {
            mTools.showToast(getString(R.string.donate_something_went_wrong), 1);

            Log.e("DonateActivity", Log.getStackTraceString(e));
        }
    }

    private void consumeDonation(String purchaseToken)
    {
        try
        {
            mIInAppBillingService.consumePurchase(3, getPackageName(), purchaseToken);
        }
        catch(Exception e)
        {
            mTools.showToast(getString(R.string.donate_something_went_wrong), 1);

            Log.e("DonateActivity", Log.getStackTraceString(e));
        }
    }

    private void resetDonations()
    {
        try
        {
            Bundle ownedItems = mIInAppBillingService.getPurchases(3, getPackageName(), "inapp", null);

            int response = ownedItems.getInt("RESPONSE_CODE");

            if(response == 0)
            {
                ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");

                for(String purchaseData : purchaseDataList)
                {
                    JSONObject jsonObject = new JSONObject(purchaseData);

                    String purchaseToken = jsonObject.getString("purchaseToken");

                    consumeDonation(purchaseToken);
                }

                mTools.showToast(getString(R.string.donate_reset_success), 0);
            }
            else
            {
                mTools.showToast(getString(R.string.donate_something_went_wrong), 1);
            }
        }
        catch(Exception e)
        {
            mTools.showToast(getString(R.string.donate_something_went_wrong), 1);

            Log.e("DonateActivity", Log.getStackTraceString(e));
        }
    }

    // Get products
    private class GetProductsTask extends AsyncTask<Void, Void, Bundle>
    {
        @Override
        protected void onPostExecute(Bundle skuDetails)
        {
            if(skuDetails == null)
            {
                mTools.showToast(getString(R.string.donate_something_went_wrong), 1);
            }
            else
            {
                try
                {
                    int responseCode = skuDetails.getInt("RESPONSE_CODE");

                    if(responseCode == 0)
                    {
                        Button makeSmallDonationButton = (Button) findViewById(R.id.donate_make_small_donation);
                        Button makeMediumDonationButton = (Button) findViewById(R.id.donate_make_medium_donation);
                        Button makeBigDonationButton = (Button) findViewById(R.id.donate_make_big_donation);

                        ArrayList<String> responseArrayList = skuDetails.getStringArrayList("DETAILS_LIST");

                        for(String details : responseArrayList)
                        {
                            JSONObject detailsJsonObject = new JSONObject(details);

                            String sku = detailsJsonObject.getString("productId");
                            String price = detailsJsonObject.getString("price");

                            switch(sku)
                            {
                                case "small_donation":
                                {
                                    makeSmallDonationButton.setText(getString(R.string.donate_donate)+" "+price);

                                    makeSmallDonationButton.setOnClickListener(new View.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(View view)
                                        {
                                            makeDonation("small_donation");
                                        }
                                    });

                                    break;
                                }
                                case "medium_donation":
                                {
                                    makeMediumDonationButton.setText(getString(R.string.donate_donate)+" "+price);

                                    makeMediumDonationButton.setOnClickListener(new View.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(View view)
                                        {
                                            makeDonation("medium_donation");
                                        }
                                    });

                                    break;
                                }
                                case "big_donation":
                                {
                                    makeBigDonationButton.setText(getString(R.string.donate_donate)+" "+price);

                                    makeBigDonationButton.setOnClickListener(new View.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(View view)
                                        {
                                            makeDonation("big_donation");
                                        }
                                    });

                                    break;
                                }
                            }
                        }
                    }
                }
                catch(Exception e)
                {
                    mTools.showToast(getString(R.string.donate_something_went_wrong), 1);

                    Log.e("DonateActivity", Log.getStackTraceString(e));
                }
            }
        }

        @Override
        protected Bundle doInBackground(Void... voids)
        {
            ArrayList<String> skuList = new ArrayList<>();

            skuList.add("small_donation");
            skuList.add("medium_donation");
            skuList.add("big_donation");

            Bundle querySkus = new Bundle();
            querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

            Bundle skuDetails;

            try
            {
                skuDetails = mIInAppBillingService.getSkuDetails(3, getPackageName(), "inapp", querySkus);
            }
            catch(Exception e)
            {
                Log.e("DonateActivity", Log.getStackTraceString(e));

                skuDetails = null;
            }

            return skuDetails;
        }
    }

    // Service
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mIInAppBillingService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mIInAppBillingService = IInAppBillingService.Stub.asInterface(service);

            GetProductsTask getProductsTask = new GetProductsTask();
            getProductsTask.execute();
        }
    };
}