package org.godotengine.godot;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;
import com.applovin.sdk.AppLovinPrivacySettings;

public class GodotApplovin extends Godot.SingletonBase
{
    private final String TAG = GodotApplovin.class.getName();
    private Activity activity = null; // The main activity of the game

    private HashMap<String, View> zombieBanners = new HashMap<>();
    private HashMap<String, FrameLayout.LayoutParams> bannerParams = new HashMap<>();
    private HashMap<String, Integer> callbacks = new HashMap<>();
    private HashMap<String, AppLovinAd> interstitials = new HashMap<>();
    private HashMap<String, AppLovinAdView> banners = new HashMap<>();
    private HashMap<String, AppLovinIncentivizedInterstitial> rewardeds = new HashMap<>();

    private boolean ProductionMode = true; // Store if is real or not

    private FrameLayout layout = null; // Store the layout
    private AppLovinSdk sdk = null;
    private AppLovinInterstitialAdDialog interstitialAd = null;
    private boolean _inited = false;
    private boolean gdprApplies;

    /* Init
     * ********************************************************************** */

    /**
     * Prepare for work with YandexAds
     * @param boolean ProductionMode Tell if the enviroment is for real or test
     * @param int gdscript instance id
     */
    public void init(final String sdkKey, boolean ProductionMode) {

        this.ProductionMode = ProductionMode;
        //if(!ProductionMode) sdk.getSettings().setVerboseLogging( true );
        layout = (FrameLayout)activity.getWindow().getDecorView().getRootView();

        Log.i(TAG, "Applovin sdk key: " + sdkKey);
        sdk = AppLovinSdk.getInstance(sdkKey, new AppLovinSdkSettings(), activity);
        sdk.initializeSdk(new AppLovinSdk.SdkInitializationListener() {
                @Override
                public void onSdkInitialized(final AppLovinSdkConfiguration configuration)
                {
                    _inited = true;
                    Log.i(TAG, "Applovin initialized");
                    if ( configuration.getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES ) {
                        // Show user consent dialog
                        Log.i(TAG, "GDPR applies");
                        gdprApplies = true;
                    } else if ( configuration.getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.DOES_NOT_APPLY ) {
                        // No need to show consent dialog, proceed with initialization
                        Log.i(TAG, "GDPR doesn't applies");
                        gdprApplies = false;
                    } else {
                        // Consent dialog state is unknown. Proceed with initialization, but check if the consent
                        // dialog should be shown on the next application initialization
                        Log.i(TAG, "GDPR status unknown");
                        gdprApplies = false;
                    }
                }
            });

        // Create instance of interstitial ad
        interstitialAd = AppLovinInterstitialAd.create( sdk, activity );

        // Optional: Set listeners
        interstitialAd.setAdDisplayListener(new AppLovinAdDisplayListener() {
                @Override
                public void adDisplayed(AppLovinAd appLovinAd) {
                    // An interstitial ad was displayed.
                }
                @Override
                public void adHidden(AppLovinAd appLovinAd) {
                    // An interstitial ad was hidden.
                    int callback_id = callbacks.get(appLovinAd.getZoneId());
                    GodotLib.calldeferred(callback_id, "_on_interstitial_close", new Object[] { appLovinAd.getZoneId() });
                }
            });
        interstitialAd.setAdClickListener(new AppLovinAdClickListener() {
                @Override
                public void adClicked(final AppLovinAd ad) {
                }
            });
        interstitialAd.setAdVideoPlaybackListener( new  AppLovinAdVideoPlaybackListener() {
                @Override
                public void videoPlaybackBegan(final AppLovinAd ad) {
                }
                @Override
                public void videoPlaybackEnded(final AppLovinAd ad, final double percentViewed, final boolean fullyWatched) {
                }
            });
        // Set listeners to an instance of your AppLovinAdView
        //adView.setAdLoadListener( <objectImplementingAppLovinAdLoadListener> );
        //adView.setAdDisplayListener( <objectImplementingAppLovinAdDisplayListener> );
        //adView.setAdClickListener( <objectImplementingAppLovinAdClickListener> );
    }

    public boolean isInited() {
        return _inited;
    }

    public void setUserId(final String uid) {
        sdk.setUserIdentifier( uid );
    }

    public boolean isGdprApplies() {
        return gdprApplies;
    }
    
    public void debugMediation() {
        sdk.showMediationDebugger();
    }

    public void setGdprConsent(final boolean consent) {
        AppLovinPrivacySettings.setHasUserConsent( consent, activity );
    }

    public void setAgeRestricted(final boolean ageRestricted) {
        AppLovinPrivacySettings.setIsAgeRestrictedUser( ageRestricted, activity );
    }

    public void setCCPAApplied(final boolean ccpaApplied) {
        AppLovinPrivacySettings.setDoNotSell( ccpaApplied, activity );
    }


    /* Rewarded Video
     * ********************************************************************** */


    /**
     * Load a Rewarded Video
     * @param String id AdMod Rewarded video ID
     */
    public void loadRewardedVideo(final String id, final int callback_id) {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    try {
                        callbacks.put(id, callback_id);

                        // Create instance of incentivized interstitial ad
                        AppLovinIncentivizedInterstitial incentivizedInterstitial = AppLovinIncentivizedInterstitial.create(id, sdk);
                        rewardeds.put(id, incentivizedInterstitial);
                        incentivizedInterstitial.preload( new AppLovinAdLoadListener() {
                                @Override
                                public void adReceived(AppLovinAd appLovinAd) {
                                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_loaded", new Object[] { id });
                                }
                                @Override
                                public void failedToReceiveAd(int errorCode) {
                                    // Look at AppLovinErrorCodes.java for list of error codes
                                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_failed_to_load", new Object[] { id, ""+errorCode });
                                }
                            } );

                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                        e.printStackTrace();
                    }
                }
            });
    }

    /**
     * Show a Rewarded Video
     */
    public void showRewardedVideo(final String id) {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(rewardeds.containsKey(id)) {

                        AppLovinIncentivizedInterstitial incentivizedInterstitial = rewardeds.get(id);

                        if ( incentivizedInterstitial.isAdReadyToDisplay() ) {
                            AppLovinAdDisplayListener displayListener = new AppLovinAdDisplayListener(){
                                    @Override
                                    public void adDisplayed(AppLovinAd appLovinAd) {
                                        // An interstitial ad was displayed.
                                    }
                                    @Override
                                    public void adHidden(AppLovinAd appLovinAd) {
                                        // An interstitial ad was hidden.
                                        int callback_id = callbacks.get(id);
                                        GodotLib.calldeferred(callback_id, "_on_rewarded", new Object[] { id, "Reward", 1 });
                                        GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_closed", new Object[] { id });
                                    }
                                };
                            incentivizedInterstitial.show(activity, 
                                                          null, //<AppLovinAdRewardListener>, 
                                                          null, //<AppLovinAdVideoPlaybackListener>, 
                                                          displayListener, 
                                                          null //<AppLovinAdClickListener> 
                                                          );
                        } else {
                            Log.w(TAG, "showRewardedVideo - rewarded not loaded");
                        }
                    }
                }
            });
    }

    /* Banner
     * ********************************************************************** */

    private AppLovinAdView initBanner(final String id, final boolean isOnTop, final int callback_id)
    {

        // Create an ad view with a specific zone to load ads for
        AppLovinAdView adView = new AppLovinAdView(sdk, AppLovinAdSize.BANNER, id, activity );

        // Optional: Set listeners
        adView.setAdLoadListener( new AppLovinAdLoadListener() {
                @Override
                public void adReceived(AppLovinAd appLovinAd) {
                    Log.w(TAG, "Banner: adReceived");
                    GodotLib.calldeferred(callback_id, "_on_banner_loaded", new Object[]{ id });
                }
                @Override
                public void failedToReceiveAd(int errorCode) {
                    Log.w(TAG, "Banner: failedToReceiveAd");
                    // Look at AppLovinErrorCodes.java for list of error codes
                    GodotLib.calldeferred(callback_id, "_on_banner_failed_to_load", new Object[]{ id, ""+errorCode });
                }
            });
        //adView.setAdDisplayListener( … );
        //adView.setAdClickListener( … );
        //adView.setAdViewEventListener( … );

        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                                                FrameLayout.LayoutParams.MATCH_PARENT,
                                                FrameLayout.LayoutParams.WRAP_CONTENT
                                                );
        if(isOnTop) adParams.gravity = Gravity.TOP;
        else adParams.gravity = Gravity.BOTTOM;
        adView.setBackgroundColor(/* Color.WHITE */Color.TRANSPARENT);
        bannerParams.put(id, adParams);
        return adView;
    }

    private void placeBannerOnScreen(final String id, final AppLovinAdView banner)
    {
        FrameLayout.LayoutParams adParams = bannerParams.get(id);
        layout.addView(banner, adParams);
    }

    /**
     * Load a banner
     * @param String id AdMod Banner ID
     * @param boolean isOnTop To made the banner top or bottom
     */
    public void loadBanner(final String id, final boolean isOnTop, final int callback_id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(!banners.containsKey(id)) {
                        AppLovinAdView adView = initBanner(id, isOnTop, callback_id);
                        adView.loadNextAd();
                        banners.put(id, adView);
                    } else {
                        AppLovinAdView adView = banners.get(id);
                        adView.loadNextAd();
                    }
                }
            });
    }

    /**
     * Show the banner
     */
    public void showBanner(final String id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(banners.containsKey(id)) {
                        AppLovinAdView b = banners.get(id);
                        if(b.getParent() == null) {
                            placeBannerOnScreen(id, b);
                        }
                        b.setVisibility(View.VISIBLE);
                        b.resume();
                        for (String key : banners.keySet()) {
                            if(!key.equals(id)) {
                                AppLovinAdView b2 = banners.get(key);
                                b2.setVisibility(View.GONE);
                                b2.pause();
                            }
                        }
                        Log.d(TAG, "Show Banner");
                    } else {
                        Log.w(TAG, "Banner not found: "+id);
                    }
                }
            });
    }

    public void removeBanner(final String id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(banners.containsKey(id)) {
                        AppLovinAdView b = banners.get(id);
                        banners.remove(id);
                        layout.removeView(b); // Remove the banner
                        Log.d(TAG, "Remove Banner");
                    } else {
                        Log.w(TAG, "Banner not found: "+id);
                    }
                }
            });
    }

    /**
     * Hide the banner
     */
    public void hideBanner(final String id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(banners.containsKey(id)) {
                        AppLovinAdView b = banners.get(id);
                        b.setVisibility(View.GONE);
                        b.pause();
                        Log.d(TAG, "Hide Banner");
                    } else {
                        Log.w(TAG, "Banner not found: "+id);
                    }
                }
            });
    }

    /**
     * Get the banner width
     * @return int Banner width
     */
    public int getBannerWidth(final String id)
    {
        if(banners.containsKey(id)) {
            AppLovinAdView b = banners.get(id);
            if(b != null) {
                Resources r = activity.getResources();
                return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, b.getSize().getWidth(), r.getDisplayMetrics());
            } else
                return 0;
        } else {
            return 0;
        }
    }

    /**
     * Get the banner height
     * @return int Banner height
     */
    public int getBannerHeight(final String id)
    {
        if(banners.containsKey(id)) {
            AppLovinAdView b = banners.get(id);
            if(b != null) {
                Resources r = activity.getResources();
                return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, b.getSize().getHeight(), r.getDisplayMetrics());
            } else
                return 0;
        } else {
            return 0;
        }
    }

    public String makeZombieBanner(final String id)
    {
        if (banners.containsKey(id)) {
            AppLovinAdView b = banners.get(id);
            String zid = java.util.UUID.randomUUID().toString();
            banners.remove(id);
            zombieBanners.put(zid, b);
            Log.i(TAG, "makeZombieBanner: OK");
            return zid;
        } else {
            Log.w(TAG, "makeZombieBanner: Banner not found: "+id);
            return "";
        }
    }

    public void killZombieBanner(final String zid)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (zombieBanners.containsKey(zid)) {
                        View z = zombieBanners.get(zid);
                        zombieBanners.remove(zid);
                        layout.removeView(z); // Remove the zombie banner
                        Log.w(TAG, "killZombieBanner: OK");
                    } else {
                        Log.w(TAG, "killZombieBanner: Banner not found: "+zid);
                    }
                }
            });
    }

    /* Interstitial
     * ********************************************************************** */

    /**
     * Load a interstitial
     * @param String id AdMod Interstitial ID
     */
    public void loadInterstitial(final String id, final int callback_id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    // Load an ad for a given zone
                    callbacks.put(id, callback_id);
                    sdk.getAdService().loadNextAdForZoneId( id, new AppLovinAdLoadListener() {
                            @Override
                            public void adReceived(AppLovinAd ad) {
                                // Render the returned ad
                                interstitials.put(id, ad);
                                GodotLib.calldeferred(callback_id, "_on_interstitial_loaded", new Object[] { id });
                            }
                            @Override
                            public void failedToReceiveAd(int errorCode) {
                                Log.e(TAG, "Interstitial loading error: " + errorCode);
                                // Look at AppLovinErrorCodes.java for list of error codes
                                GodotLib.calldeferred(callback_id, "_on_interstitial_failed_to_load", new Object[] { id, ""+errorCode });
                            }
                        } );
                }
            });
    }

    /**
     * Show the interstitial
     */
    public void showInterstitial(final String id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(interstitials.containsKey(id)) {
                        AppLovinAd interstitial = interstitials.get(id);
                        interstitialAd.showAndRender(interstitial);
                    } else {
                        Log.w(TAG, "Interstitial not found: " + id);
                    }
                }
            });
    }

    /* Utils
     * ********************************************************************** */


    /* Definitions
     * ********************************************************************** */

    /**
     * Initilization Singleton
     * @param Activity The main activity
     */
    static public Godot.SingletonBase initialize(Activity activity)
    {
        return new GodotApplovin(activity);
    }

    /**
     * Constructor
     * @param Activity Main activity
     */
    public GodotApplovin(Activity p_activity) {
        registerClass("Applovin", new String[] {
                "init", "isInited", "setUserId", "debugMediation", "isGdprApplies", "setGdprConsent", "setAgeRestricted", "setCCPAApplied",
                // banner
                "loadBanner", "showBanner", "hideBanner", "removeBanner", "getBannerWidth", "getBannerHeight",
                "makeZombieBanner", "killZombieBanner",
                // Interstitial
                "loadInterstitial", "showInterstitial",
                // Rewarded video
                "loadRewardedVideo", "showRewardedVideo"
            });
        activity = p_activity;
    }
}
