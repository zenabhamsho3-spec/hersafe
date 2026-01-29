package com.example.hersafe.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.hersafe.R;
import com.example.hersafe.ui.auth.LoginActivity;

public class OnboardingFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_DESC = "desc";
    private static final String ARG_IMG = "img";
    private static final String ARG_INDEX = "index";

    private String title;
    private String description;
    private int imageResId;
    private int pageIndex;

    public OnboardingFragment() {
        // Required empty public constructor
    }

    public static OnboardingFragment newInstance(String title, String description, int imageResId, int pageIndex) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, description);
        args.putInt(ARG_IMG, imageResId);
        args.putInt(ARG_INDEX, pageIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            description = getArguments().getString(ARG_DESC);
            imageResId = getArguments().getInt(ARG_IMG);
            pageIndex = getArguments().getInt(ARG_INDEX);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_step, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvDesc = view.findViewById(R.id.tvDescription);
        ImageView imgIllustration = view.findViewById(R.id.imgIllustration);
        ImageButton btnNext = view.findViewById(R.id.btnNextArrow);
        ImageButton btnPrev = view.findViewById(R.id.btnPreviousArrow);

        // Set Data
        tvTitle.setText(title);
        tvDesc.setText(description);
        imgIllustration.setImageResource(imageResId);

        // Setup Dots
        setupDots(view, pageIndex);

        // Setup Buttons
        if (pageIndex == 0) {
            btnPrev.setVisibility(View.INVISIBLE);
        } else {
            btnPrev.setVisibility(View.VISIBLE);
        }

        btnNext.setOnClickListener(v -> {
            ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPager);
            if (viewPager != null) {
                if (pageIndex < 3) {
                    viewPager.setCurrentItem(pageIndex + 1, true);
                } else {
                    // Last Page -> Go to Login
                    finishOnboarding();
                }
            }
        });

        btnPrev.setOnClickListener(v -> {
            ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPager);
            if (viewPager != null && pageIndex > 0) {
                viewPager.setCurrentItem(pageIndex - 1, true);
            }
        });
    }

    private void setupDots(View view, int activeIndex) {
        ImageView[] dots = new ImageView[]{
                view.findViewById(R.id.dot1),
                view.findViewById(R.id.dot2),
                view.findViewById(R.id.dot3),
                view.findViewById(R.id.dot4)
        };

        for (int i = 0; i < dots.length; i++) {
            if (i == activeIndex) {
                dots[i].setImageResource(R.drawable.indicator_active);
                // We could set tint here if needed, but the drawable might handle it
                // dots[i].setColorFilter(ContextCompat.getColor(requireContext(), R.color.hersafe_title));
                // Based on layout analysis, active dot needs tinting sometimes.
                // Simplified: Just use indicator_active
                 dots[i].setColorFilter(ContextCompat.getColor(requireContext(), R.color.hersafe_title));
            } else {
                dots[i].setImageResource(R.drawable.indicator_inactive);
                dots[i].clearColorFilter();
            }
        }
    }

    private void finishOnboarding() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }
}