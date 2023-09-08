package com.denisg.spotiinfo;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class Animations {


    public static void FadeInOutViews(final View viewToFadeIn, final View... viewsToHide) {

        Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(500);
        fadeOut.setFillAfter(true);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                for (View view : viewsToHide) {
                    view.setVisibility(View.GONE);
                    view.setElevation(-10f);
                    EnableDisableViews((ViewGroup) view,false);
                }

                Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(500);
                fadeIn.setFillAfter(true);

                viewToFadeIn.setVisibility(View.VISIBLE);
                EnableDisableViews((ViewGroup) viewToFadeIn,true);
                viewToFadeIn.setElevation(1f);


                viewToFadeIn.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Animation repeated (if needed)
            }
        });


        for (View view : viewsToHide) {
            if (view.getVisibility() == View.VISIBLE)
                view.startAnimation(fadeOut);
        }
    }

    public static void EnableDisableViews(ViewGroup viewGroup, boolean type) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup)
                EnableDisableViews((ViewGroup) child, type);
            else
                child.setEnabled(type);
        }
    }

    public static void fadeInAndBounce(final View view) {
        view.setVisibility(View.INVISIBLE);

        view.animate()
                .alpha(1)
                .setDuration(1000)
                .withStartAction(new Runnable() {
                    @Override
                    public void run() {
                        view.setVisibility(View.VISIBLE);
                    }
                })
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        view.animate()
                                .translationYBy(-100)
                                .setDuration(500)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        view.animate()
                                                .translationYBy(100)
                                                .setDuration(500)
                                                .setInterpolator(new BounceInterpolator());
                                    }
                                });
                    }
                });
    }
}