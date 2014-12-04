package net.olejon.spotcommander;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
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
    private final MyMethod mMethod = new MyMethod(this);

    private IInAppBillingService mService;
    private GetProductsTask getProductsTask;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Allow landscape?
        if(!mMethod.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Layout
        setContentView(R.layout.activity_donate);

        // Toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);

        // In-app billing
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    // Destroy activity
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mService != null) unbindService(mServiceConn);

        if(getProductsTask != null && getProductsTask.getStatus() == AsyncTask.Status.RUNNING) getProductsTask.cancel(true);
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

                    mMethod.showToast(getString(R.string.donate_thank_you), 1);

                    finish();
                }
                catch(Exception e)
                {
                    mMethod.showToast(getString(R.string.donate_something_went_wrong), 1);

                    Log.e("onActivityResult", Log.getStackTraceString(e));
                }
            }
        }
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.donate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
        else if(item.getItemId() == R.id.action_reset)
        {
            resetDonations();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Donations
    private void makeDonation(String product)
    {
        try
        {
            Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), product, "inapp", "");
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

            startIntentSenderForResult(pendingIntent.getIntentSender(), 1, new Intent(), 0, 0, 0);
        }
        catch(Exception e)
        {
            mMethod.showToast(getString(R.string.donate_something_went_wrong), 1);

            Log.e("makeDonation", Log.getStackTraceString(e));
        }
    }

    private void consumeDonation(String purchaseToken)
    {
        try
        {
            mService.consumePurchase(3, getPackageName(), purchaseToken);
        }
        catch(Exception e)
        {
            mMethod.showToast(getString(R.string.donate_something_went_wrong), 1);

            Log.e("consumeDonation", Log.getStackTraceString(e));
        }
    }

    private void resetDonations()
    {
        try
        {
            Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);

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

                mMethod.showToast(getString(R.string.donate_reset_successful), 0);
            }
            else
            {
                mMethod.showToast(getString(R.string.donate_something_went_wrong), 1);
            }
        }
        catch(Exception e)
        {
            mMethod.showToast(getString(R.string.donate_something_went_wrong), 1);

            Log.e("resetDonations", Log.getStackTraceString(e));
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
                mMethod.showToast(getString(R.string.donate_something_went_wrong), 1);
            }
            else
            {
                try
                {
                    int response = skuDetails.getInt("RESPONSE_CODE");

                    if(response == 0)
                    {
                        Button makeSmallDonationButton = (Button) findViewById(R.id.make_small_donation_button);
                        Button makeMediumDonationButton = (Button) findViewById(R.id.make_medium_donation_button);
                        Button makeBigDonationButton = (Button) findViewById(R.id.make_big_donation_button);

                        ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");

                        for(String thisResponse : responseList)
                        {
                            JSONObject object = new JSONObject(thisResponse);

                            String sku = object.getString("productId");
                            String price = object.getString("price");

                            if(sku.equals("small_donation"))
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
                            }
                            else if(sku.equals("medium_donation"))
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
                            }
                            else if(sku.equals("big_donation"))
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
                            }
                        }
                    }
                }
                catch(Exception e)
                {
                    mMethod.showToast(getString(R.string.donate_something_went_wrong), 1);

                    Log.e("GetProductsTask onPostExecute", Log.getStackTraceString(e));
                }
            }
        }

        @Override
        protected Bundle doInBackground(Void... voids)
        {
            ArrayList<String> skuList = new ArrayList<String>();

            skuList.add("small_donation");
            skuList.add("medium_donation");
            skuList.add("big_donation");

            Bundle querySkus = new Bundle();
            querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

            Bundle skuDetails;

            try
            {
                skuDetails = mService.getSkuDetails(3, getPackageName(), "inapp", querySkus);
            }
            catch(Exception e)
            {
                Log.e("GetProductsTask doInBackground", Log.getStackTraceString(e));

                skuDetails = null;
            }

            return skuDetails;
        }
    }

    // Service
    private final ServiceConnection mServiceConn = new ServiceConnection()
    {
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mService = IInAppBillingService.Stub.asInterface(service);

            getProductsTask = new GetProductsTask();
            getProductsTask.execute();
        }
    };
}