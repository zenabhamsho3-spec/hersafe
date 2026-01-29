package com.example.hersafe.ui.onboarding;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.hersafe.R;

public class OnboardingAdapter extends FragmentStateAdapter {

    private final Context context;

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.context = fragmentActivity;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String title;
        String desc;
        int imgRes;

        switch (position) {
            case 0:
                title = context.getString(R.string.welcome_title);
                desc = context.getString(R.string.welcome_description);
                imgRes = R.drawable.women;
                break;
            case 1:
                title = context.getString(R.string.emergency_title);
                desc = context.getString(R.string.emergency_desc);
                imgRes = R.drawable.img1;
                break;
            case 2:
                title = context.getString(R.string.spy_camera_title);
                desc = context.getString(R.string.spy_camera_desc);
                imgRes = R.drawable.img2;
                break;
            case 3:
                title = context.getString(R.string.share_loc_title);
                desc = context.getString(R.string.share_loc_desc);
                imgRes = R.drawable.img4;
                break;
            case 4:
                // Dummy page for swipe-to-login transition
                title = "";
                desc = "";
                imgRes = 0;
                break;
            default:
                // Fallback
                title = "";
                desc = "";
                imgRes = 0;
        }

        return OnboardingFragment.newInstance(title, desc, imgRes, position);
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}