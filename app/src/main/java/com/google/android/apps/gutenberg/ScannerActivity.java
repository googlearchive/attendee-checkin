/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.gutenberg;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.apps.gutenberg.animation.FastOutSlowInInterpolator;
import com.google.android.apps.gutenberg.model.Checkin;
import com.google.android.apps.gutenberg.model.CheckinHolder;
import com.google.android.apps.gutenberg.widget.AppCompatTextView;
import com.google.android.apps.gutenberg.widget.DrawerViewPager;
import com.google.android.apps.gutenberg.widget.RecyclerViewFragment;
import com.google.android.apps.gutenberg.widget.RecyclerViewSlidingUpPanelLayout;
import com.google.android.apps.gutenberg.widget.TabLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class ScannerActivity extends BaseActivity implements ScannerFragment.Listener {

    private static final int VIEW_PAGER_PAGE_MARGIN = 16;
    private static final float TIMELINE_TRANSLATION_X = -32.f;
    private static final String FRAGMENT_ABOUT = "fragment_about";

    private RecyclerViewSlidingUpPanelLayout mPanelLayout;
    private DrawerViewPager mViewPager;
    private TabLayout mTabLayout;
    private AppCompatTextView mTabs[];

    private int mColorTabSelected;
    private int mColorTabUnselected;
    private int mColorTabIndicator;
    private int mToolbarElevation;

    private ImageLoader mImageLoader;
    private Animator mLastAnimator;
    private ScannerPagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        // Resources
        final Resources resources = getResources();
        mColorTabSelected = resources.getColor(R.color.tab_selected);
        mColorTabUnselected = resources.getColor(R.color.tab_unselected);
        mColorTabIndicator = resources.getColor(R.color.primary_dark);
        mToolbarElevation = resources.getDimensionPixelSize(R.dimen.toolbar_elevation);
        GutenbergApplication app = GutenbergApplication.from(this);
        mImageLoader = new ImageLoader(app.getRequestQueue(), app.getBitmapCache());
        // Set up the Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Set up the Fragments
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.scanner, ScannerFragment.newInstance())
                    .replace(R.id.event_selector, EventSelectionFragment.newInstance())
                    .commit();
        }
        // Set up the ViewPager
        mPanelLayout = (RecyclerViewSlidingUpPanelLayout) findViewById(R.id.layout);
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.getViewTreeObserver().addOnGlobalLayoutListener(mTabLayoutListener);
        mViewPager = (DrawerViewPager) findViewById(R.id.pager);
        mAdapter = new ScannerPagerAdapter(getSupportFragmentManager(), this);
        mViewPager.setAdapter(mAdapter);
        mTabLayout.addTabsFromPagerAdapter(mAdapter);
        int tabCount = mAdapter.getCount();
        mTabs = new AppCompatTextView[tabCount];
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < tabCount; i++) {
            mTabs[i] = (AppCompatTextView) inflater.inflate(R.layout.tab, mTabLayout, false);
            mTabs[i].setText(mAdapter.getPageTitle(i));
            mTabLayout.getTabAt(i).setCustomView(mTabs[i]);
        }
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
                mTabs[tab.getPosition()].setTextColor(mColorTabSelected);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                mTabs[tab.getPosition()].setTextColor(mColorTabUnselected);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        mPanelLayout.setPanelSlideListener(mPanelSlideListener);
        mViewPager.setPageMargin((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                VIEW_PAGER_PAGE_MARGIN, resources.getDisplayMetrics()));
        mViewPager.setPageMarginDrawable(resources.getDrawable(R.drawable.page_margin));
        final ViewPager.OnPageChangeListener onPageChangeListener
                = mTabLayout.createOnPageChangeListener();
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                onPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                onPageChangeListener.onPageSelected(position);
                mPanelLayout.setRecyclerView(mAdapter.getItem(position).getRecyclerView());
            }
        });
        mAdapter.getItem(mViewPager.getCurrentItem()).setOnRecyclerViewReadyListener(
                new RecyclerViewFragment.OnRecyclerViewReadyListener() {
                    @Override
                    public void onRecyclerViewReady(RecyclerView rv) {
                        mPanelLayout.setRecyclerView(rv);
                    }
                });
        mViewPager.setOnClickListenerWhenClosed(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_scanner, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_switch_account:
                selectAccount(true);
                return true;
            case R.id.action_about:
                AboutFragment.newInstance().show(getSupportFragmentManager(), FRAGMENT_ABOUT);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.COLLAPSED) {
            mPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            mViewPager.setCurrentItem(0, true);
        } else {
            super.onBackPressed();
        }
    }

    private void showCheckinAnimation(Checkin checkin) {
        if (mLastAnimator != null) {
            mLastAnimator.cancel();
        }
        final FrameLayout cover = (FrameLayout) findViewById(R.id.item_cover);
        cover.setVisibility(View.VISIBLE);
        final FrameLayout layer = (FrameLayout) findViewById(R.id.animation_layer);
        final CheckinHolder holder = new CheckinHolder(getLayoutInflater(), layer);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_VERTICAL;
        holder.setWillAnimate(true);
        holder.bind(checkin, mImageLoader);
        holder.itemView.setBackgroundColor(Color.rgb(0xf0, 0xf0, 0xf0));
        float elevation = getResources().getDimension(R.dimen.popup_elevation);
        ViewCompat.setTranslationZ(holder.itemView, elevation);
        holder.setLines(false, false);
        layer.addView(holder.itemView, lp);
        // Interpolator for animators
        FastOutSlowInInterpolator interpolator = new FastOutSlowInInterpolator();
        // Pop-up
        Animator popUpAnim = AnimatorInflater.loadAnimator(this, R.animator.pop_up);
        popUpAnim.setTarget(holder.itemView);
        popUpAnim.setInterpolator(interpolator);
        popUpAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                holder.animateCheckin();
            }
        });
        // Wait
        ObjectAnimator waitAnim = new ObjectAnimator();
        waitAnim.setTarget(holder.itemView);
        waitAnim.setPropertyName("translationY");
        waitAnim.setFloatValues(0.f, 0.f);
        waitAnim.setDuration(2000);
        // Slide-down
        ObjectAnimator slideDownAnim = new ObjectAnimator();
        slideDownAnim.setTarget(holder.itemView);
        slideDownAnim.setPropertyName("translationY");
        slideDownAnim.setFloatValues(0.f, calcSlideDistance());
        slideDownAnim.setInterpolator(interpolator);
        // Landing anim
        ObjectAnimator landingAnim = new ObjectAnimator();
        landingAnim.setTarget(holder.itemView);
        landingAnim.setPropertyName("translationZ");
        landingAnim.setFloatValues(elevation, 0.f);
        landingAnim.setInterpolator(interpolator);
        landingAnim.setDuration(500);
        // Play the animators
        AnimatorSet set = new AnimatorSet();
        set.setInterpolator(interpolator);
        set.playSequentially(
                popUpAnim,
                waitAnim,
                slideDownAnim,
                landingAnim
        );
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                clean();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                clean();
            }

            private void clean() {
                mLastAnimator = null;
                layer.removeAllViews();
                cover.setVisibility(View.INVISIBLE);
            }
        });
        mLastAnimator = set;
        set.start();
    }

    private int calcSlideDistance() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int height = metrics.heightPixels;
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, value, true);
        float itemHeight = value.getDimension(metrics);
        float paddingTop = getResources().getDimension(R.dimen.list_vertical_padding);
        return (int) ((height - itemHeight + paddingTop) / 2);
    }

    private ViewTreeObserver.OnGlobalLayoutListener mTabLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            SlidingUpPanelLayout.PanelState state = mPanelLayout.getPanelState();
            if (state == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                for (int i = 1; i < mTabs.length; i++) {
                    mTabs[i].setAlpha(0.f);
                }
                mTabs[0].setTextColor(mColorTabSelected);
                mTabs[0].setTranslationX(TIMELINE_TRANSLATION_X);
                ViewCompat.setTranslationZ(mTabLayout, 0);
                mTabLayout.setEnabled(false);
                mTabLayout.setTabIndicatorColor(Color.TRANSPARENT);
            } else if (state == SlidingUpPanelLayout.PanelState.EXPANDED) {
                for (int i = 1; i < mTabs.length; i++) {
                    mTabs[i].setAlpha(1.f);
                }
                mTabs[0].setTranslationX(0.f);
                ViewCompat.setTranslationZ(mTabLayout, mToolbarElevation);
                mTabLayout.setEnabled(true);
                mTabLayout.setTabIndicatorColor(mColorTabIndicator);
            }
            mTabLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
    };

    private SlidingUpPanelLayout.PanelSlideListener mPanelSlideListener
            = new SlidingUpPanelLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View view, float v) {
            // v: 0 (collapsed), 1 (expanded)
            for (int i = 1; i < mTabs.length; i++) {
                mTabs[i].setAlpha(v);
            }
            mTabs[0].setTranslationX(TIMELINE_TRANSLATION_X * (1 - v));
            ViewCompat.setTranslationZ(mTabLayout, mToolbarElevation * v);
            mTabLayout.setTabIndicatorColor(Color.argb((int) (0xff * v),
                    Color.red(mColorTabIndicator),
                    Color.green(mColorTabIndicator),
                    Color.blue(mColorTabIndicator)));
            if (mLastAnimator != null) {
                mLastAnimator.cancel();
                mLastAnimator = null;
            }
        }

        @Override
        public void onPanelCollapsed(View view) {
            mTabs[0].setTranslationX(TIMELINE_TRANSLATION_X);
            mViewPager.setPagingEnabled(false);
            mViewPager.setCurrentItem(0, true);
            mTabLayout.setEnabled(false);
            mTabLayout.setTabIndicatorColor(Color.TRANSPARENT);
            if (mLastAnimator != null) {
                mLastAnimator.cancel();
                mLastAnimator = null;
            }
            mAdapter.scrollToTop();
        }

        @Override
        public void onPanelExpanded(View view) {
            mTabs[0].setTranslationX(0.f);
            mViewPager.setPagingEnabled(true);
            mTabLayout.setEnabled(true);
            mTabLayout.setTabIndicatorColor(mColorTabIndicator);
        }

        @Override
        public void onPanelAnchored(View view) {
        }

        @Override
        public void onPanelHidden(View view) {
        }
    };

    @Override
    public void onNewCheckin(Checkin checkin) {
        showCheckinAnimation(checkin);
    }

    private static class ScannerPagerAdapter extends FragmentPagerAdapter {

        private static final int COUNT = 3;

        private final String[] mPageTitles = new String[COUNT];
        private final RecyclerViewFragment[] mFragments = new RecyclerViewFragment[COUNT];

        public ScannerPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            mPageTitles[0] = context.getString(R.string.pager_title_timeline);
            mPageTitles[1] = context.getString(R.string.pager_title_attending);
            mPageTitles[2] = context.getString(R.string.pager_title_all);
        }

        @Override
        public RecyclerViewFragment getItem(int position) {
            if (mFragments[position] == null) {
                switch (position) {
                    case 0:
                        mFragments[position] = TimelineFragment.newInstance();
                        break;
                    case 1:
                        mFragments[position] = AttendeeListFragment.newInstance(true);
                        break;
                    case 2:
                        mFragments[position] = AttendeeListFragment.newInstance(false);
                        break;
                }
            }
            return mFragments[position];
        }

        @Override
        public int getCount() {
            return COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPageTitles[position];
        }

        public void scrollToTop() {
            TimelineFragment fragment = (TimelineFragment) mFragments[0];
            if (fragment != null) {
                fragment.scrollToTop();
            }
        }

    }
}
