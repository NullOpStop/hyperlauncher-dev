package net.kdt.pojavlaunch.customcontrols.handleview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;

public class DeleteButton extends MaterialButton implements ActionButtonInterface {
    public DeleteButton(Context context) {super(context); init();}
    public DeleteButton(Context context, @Nullable AttributeSet attrs) {super(context, attrs); init();}

    public void init() {
        setOnClickListener(this);
        setIcon(null);
        setText(R.string.global_delete);
        setPadding(0, 0, 0, 0);
        setInsetBottom(0);
        setInsetTop(0);
    }

    private ControlInterface mCurrentlySelectedButton = null;

    @Override
    public boolean shouldBeVisible() {
        return mCurrentlySelectedButton != null;
    }

    @Override
    public void setFollowedView(ControlInterface view) {
        mCurrentlySelectedButton = view;
    }

    @Override
    public void onClick() {
        if(mCurrentlySelectedButton == null) return;

        mCurrentlySelectedButton.removeButton();
    }
}
