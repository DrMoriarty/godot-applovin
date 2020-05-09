package org.godotengine.godot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import android.app.Activity;
import android.widget.FrameLayout;
import android.view.ViewGroup.LayoutParams;
import android.provider.Settings;
import android.graphics.Color;
import android.util.Log;
import java.util.Locale;
import android.view.Gravity;
import android.view.View;
import android.os.Bundle;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdLoadListener;

public class GodotApplovin extends Godot.SingletonBase
{
    private final String TAG = GodotApplovin.class.getName();
	private Activity activity = null; // The main activity of the game

    private HashMap<String, Integer> callbacks = new HashMap<>();
    private HashMap<String, AppLovinAd> interstitials = new HashMap<>();
    private HashMap<String, AppLovinAdView> banners = new HashMap<>();
    private HashMap<String, AppLovinIncentivizedInterstitial> rewardeds = new HashMap<>();

	private boolean ProductionMode = true; // Store if is real or not

	private FrameLayout layout = null; // Store the layout
	private FrameLayout.LayoutParams adParams = null; // Store the layout params
    private AppLovinSdk sdk = null;
    private AppLovinInterstitialAdDialog interstitialAd = null;

	/* Init
	 * ********************************************************************** */

	/**
	 * Prepare for work with YandexAds
	 * @param boolean ProductionMode Tell if the enviroment is for real or test
	 * @param int gdscript instance id
	 */
	public void init(boolean ProductionMode) {

		this.ProductionMode = ProductionMode;
        if(!ProductionMode) sdk.getSettings().setVerboseLogging( true );
        
        layout = (FrameLayout)activity.getWindow().getDecorView().getRootView();

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

	/* Rewarded Video
	 * ********************************************************************** */
    /*
	private RewardedAd initRewardedVideo(final String id, final int callback_id)
	{
        Log.w("godot", "Prepare rewarded video: "+id+" callback: "+Integer.toString(callback_id));
        RewardedAd rewarded = new RewardedAd(activity);
        rewarded.setBlockId(id);
        rewarded.setRewardedAdEventListener(new RewardedAdEventListener.SimpleRewardedAdEventListener()
            {
                @Override
                public void onAdLeftApplication() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdLeftApplication");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_left_application", new Object[] { id });
                }

                @Override
                public void onAdClosed() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdClosed");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_closed", new Object[] { id });
                }

                @Override
                public void onAdFailedToLoad(final AdRequestError error) {
                    Log.w("godot", "YandexAds: onRewardedVideoAdFailedToLoad. error: " + error.toString());
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_failed_to_load", new Object[] { id, error.toString() });
                }

                @Override
                public void onAdLoaded() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdLoaded");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_loaded", new Object[] { id });
                }

                @Override
                public void onAdOpened() {
                    Log.w("godot", "YandexAds: onRewardedVideoAdOpened");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_opened", new Object[] { id });
                }

                @Override
                public void onRewarded(final Reward reward) {
                    Log.w("godot", "YandexAds: " + String.format(" onRewarded! currency: %s amount: %d", reward.getType(), reward.getAmount()));
                    GodotLib.calldeferred(callback_id, "_on_rewarded", new Object[] { id, reward.getType(), reward.getAmount() });
                }


                  @Override
                  public void onRewardedVideoStarted() {
                  Log.w("godot", "YandexAds: onRewardedVideoStarted");
                  GodotLib.calldeferred(instance_id, "_on_rewarded_video_started", new Object[] { id });
                  }

                  @Override
                  public void onRewardedVideoCompleted() {
                  Log.w("godot", "YandexAds: onRewardedVideoCompleted");
                  GodotLib.calldeferred(instance_id, "_on_rewarded_video_completed", new Object[] { id });
                  }

            });
        rewarded.loadAd(getAdRequest());
        return rewarded;
	}
    */

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

    /*
    public boolean isRewardedVideoLoaded(final String id)
    {
        return rewardedVideoAd != null && rewardedVideoAd.isLoaded();
    }
    */

	/* Banner
	 * ********************************************************************** */

    private AppLovinAdView initBanner(final String id, final boolean isOnTop, final int callback_id)
    {

        // Create an ad view with a specific zone to load ads for
        AppLovinAdView adView = new AppLovinAdView( AppLovinAdSize.BANNER, id, activity );

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

        adParams = new FrameLayout.LayoutParams(
                                                FrameLayout.LayoutParams.MATCH_PARENT,
                                                FrameLayout.LayoutParams.WRAP_CONTENT
                                                );
        if(isOnTop) adParams.gravity = Gravity.TOP;
        else adParams.gravity = Gravity.BOTTOM;
        adView.setBackgroundColor(/* Color.WHITE */Color.TRANSPARENT);
        layout.addView(adView, adParams);
        return adView;
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
            if(b != null)
                return b.getSize().getWidth();
            else
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
            if(b != null)
                return b.getSize().getHeight();
            else
                return 0;
        } else {
            return 0;
        }
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

    /*
    public boolean isInterstitialLoaded(final String id)
    {
        return interstitialAd != null && interstitialAd.isLoaded();
    }
    */

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
			"init",
			"initWithContentRating",
			// banner
			"loadBanner", "showBanner", "hideBanner", "removeBanner", "getBannerWidth", "getBannerHeight", //"resize",
			// Interstitial
			"loadInterstitial", "showInterstitial", //"isInterstitialLoaded",
			// Rewarded video
			"loadRewardedVideo", "showRewardedVideo" //, "isRewardedVideoLoaded"
		});
		activity = p_activity;
        AppLovinSdk.initializeSdk(activity);
        sdk = AppLovinSdk.getInstance(activity);
	}
}
