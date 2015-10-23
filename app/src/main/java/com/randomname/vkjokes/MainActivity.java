package com.randomname.vkjokes;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.randomname.vkjokes.Fragments.CommentsFragment;
import com.randomname.vkjokes.Fragments.FullscreenPhotoFragmentHost;
import com.randomname.vkjokes.Fragments.PublicListFragment;
import com.randomname.vkjokes.Fragments.VkLoginAlert;
import com.randomname.vkjokes.Models.WallPostModel;
import com.randomname.vkjokes.Util.StringUtils;
import com.vk.sdk.VKSdk;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements PublicListFragment.PublicListFragmentCallback {

    private final static String FULLSCREEN_FRAGMENT_TAG = "full_screen_tag";
    private final static String COMMENTS_FRAGMENT_TAG = "comments_fragment_tag";
    private final static String MENU_ICON_STATE = "menu_icon_state";
    private final static String TOOLBAR_COLOR_STATE = "toolbar_color_state";
    private final static String STATUS_COLOR_STATE = "window_color_state";
    private final static String TOOLBAR_TITLE_STATE = "toolbar_title_state";

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    private MaterialMenuDrawable materialMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initToolbar();
        FragmentManager fm = getSupportFragmentManager();

        if (fm.getFragments() == null) {
            Fragment fragment = fm.findFragmentByTag("PublicListFragment");
            if (fragment == null) {
                FragmentTransaction ft = fm.beginTransaction();
                fragment = new PublicListFragment();
                ft.replace(R.id.main_frame, fragment, "PublicListFragment");
                ft.commit();
            }
        }

        if (savedInstanceState != null) {

            String stringIconState = savedInstanceState.getString(MENU_ICON_STATE);

            materialMenu.setIconState(MaterialMenuDrawable.IconState.valueOf(stringIconState));

            toolbar.setBackgroundColor(savedInstanceState.getInt(TOOLBAR_COLOR_STATE, Color.parseColor("#2196F3")));
            getSupportActionBar().setTitle(savedInstanceState.getCharSequence(TOOLBAR_TITLE_STATE));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(savedInstanceState.getInt(STATUS_COLOR_STATE, Color.parseColor("#1E88E5")));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        MaterialMenuDrawable.IconState iconState = materialMenu.getIconState();
        outState.putString(MENU_ICON_STATE, iconState.name());

        int color = Color.TRANSPARENT;
        Drawable background = toolbar.getBackground();
        if (background instanceof ColorDrawable) {
            color = ((ColorDrawable) background).getColor();
        }

        outState.putInt(TOOLBAR_COLOR_STATE, color);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outState.putInt(STATUS_COLOR_STATE, getWindow().getStatusBarColor());
        }

        outState.putCharSequence(TOOLBAR_TITLE_STATE, getSupportActionBar().getTitle());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        closeFullscreen(false);
        super.onBackPressed();
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        materialMenu = new MaterialMenuDrawable(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFullscreen(true);
            }
        });

        toolbar.setNavigationIcon(materialMenu);
    }

    private void closeFullscreen(boolean toClose) {

        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        Fragment frag = fragments.get(fragments.size() - 1);

        if (frag == null) {
            return;
        }

        String tag = frag.getTag();

        switch (tag) {
            case FULLSCREEN_FRAGMENT_TAG:
                closePhotoFragment(toClose, frag);
                break;
            case COMMENTS_FRAGMENT_TAG:
                closeCommentFragment(toClose, frag);
                break;
            default:
        }
    }

    private void closePhotoFragment(boolean toClose, Fragment frag) {
        if (frag != null) {
            animateToolbar(
                    Color.parseColor("#000000"),
                    Color.parseColor("#2196F3"),
                    MaterialMenuDrawable.AnimationState.BURGER_ARROW,
                    1
            );
            animateStatusBar(Color.parseColor("#000000"),
                    Color.parseColor("#1E88E5")
            );

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.stay_still, R.anim.slide_out_right);
            ft.hide(frag);
            ft.commit();

            if (toClose) {
                getSupportFragmentManager().popBackStack();
            }

            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    private void closeCommentFragment(boolean toClose, Fragment frag) {
        if (frag != null) {

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.stay_still, R.anim.slide_out_top);
            ft.hide(frag);
            ft.commit();

            if (toClose) {
                getSupportFragmentManager().popBackStack();
            }

            getSupportActionBar().setTitle(R.string.app_name);
            materialMenu.animateIconState(MaterialMenuDrawable.IconState.BURGER, false);
        }
    }

    private void animateToolbar(int colorFrom, int colorTo, final MaterialMenuDrawable.AnimationState state, final int offset) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);

        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                toolbar.setBackgroundColor((Integer) animator.getAnimatedValue());
                materialMenu.setTransformationOffset(
                        state,
                        animator.getAnimatedFraction() + offset
                );

                int alphaValue = 255;

                if (offset == 0) {
                    alphaValue = Math.round(255 * (1.0f - animator.getAnimatedFraction()));
                } else {
                    alphaValue = Math.round(255 * animator.getAnimatedFraction());
                }

                if (alphaValue > 100) {
                    toolbar.getBackground().setAlpha(alphaValue);
                } else {
                    toolbar.getBackground().setAlpha(100);
                }
            }
        });

        colorAnimation.setDuration(350);
        colorAnimation.setStartDelay(0);
        colorAnimation.start();
    }

    private void animateStatusBar(int colorFrom, int colorTo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);

            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    getWindow().setStatusBarColor((Integer) animator.getAnimatedValue());
                }
            });

            colorAnimation.setDuration(350);
            colorAnimation.setStartDelay(0);
            colorAnimation.start();
        }
    }

    private void openFullScreenFragment(ArrayList<String> wallPhotos, int position) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(FULLSCREEN_FRAGMENT_TAG);
        if (fragment == null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_right, R.anim.stay_still);

            Bundle data = new Bundle();
            data.putStringArrayList(FullscreenPhotoFragmentHost.PHOTOS_ARRAY_KEY, wallPhotos);
            data.putInt(FullscreenPhotoFragmentHost.POSITION_KEY, position);

            fragment = FullscreenPhotoFragmentHost.getInstance(data);
            ft.add(R.id.main_frame, fragment, FULLSCREEN_FRAGMENT_TAG);
            ft.addToBackStack(FULLSCREEN_FRAGMENT_TAG);
            ft.commit();
            getSupportFragmentManager().executePendingTransactions();
            animateToolbar(
                    R.color.primary,
                    Color.parseColor("#000000"),
                    MaterialMenuDrawable.AnimationState.BURGER_ARROW,
                    0
            );
            animateStatusBar(
                    R.color.primary_dark,
                    Color.parseColor("#000000")
            );
        }
    }

    @Override
    public void onButtonClick(ArrayList<String> wallPhotos, int position) {
        openFullScreenFragment(wallPhotos, position);
    }

    @Override
    public void onCommentsClick(WallPostModel wallPostModel) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(COMMENTS_FRAGMENT_TAG);
        if (fragment == null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.stay_still);

            Bundle data = new Bundle();
            data.putParcelable(CommentsFragment.WALL_POST_MODEL_KEY, wallPostModel);

            fragment = CommentsFragment.getInstance(data);
            ft.add(R.id.main_frame, fragment, COMMENTS_FRAGMENT_TAG);
            ft.addToBackStack(COMMENTS_FRAGMENT_TAG);
            ft.commit();
            getSupportFragmentManager().executePendingTransactions();

            materialMenu.animateIconState(MaterialMenuDrawable.IconState.CHECK, false);
        }
    }

    @Override
    public void showVkAlert() {
        VkLoginAlert alert = VkLoginAlert.newInstance();
        alert.show(getSupportFragmentManager(), "alertTag");
    }
}
