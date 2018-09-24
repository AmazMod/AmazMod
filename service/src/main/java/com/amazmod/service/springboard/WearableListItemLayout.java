
/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.amazmod.service.springboard;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazmod.service.R;

/**
 * This layout must be inflated with two child views: a CircledImageView with id @+id/circle
 * and a TextView with id @+id/name (see layout/list_item.xml). This layout represents an
 * item in a WearableListView and gets notified when it enters or leaves a central position
 * by the OnCenterProximityListenerInterface and increases or decreases the size of
 * CircledImageView accordingly.
 */
public class WearableListItemLayout extends LinearLayout
		implements WearableListView.OnCenterProximityListener {

	private CircledImageView mCircle;
	private TextView mName;
	private final int mUnselectedCircleColor, mSelectedCircleColor, mPressedCircleColor;
	private boolean mIsInCenter;

	private float mBigCircleRadius;
	private float mSmallCircleRadius;
	private ObjectAnimator mScalingDownAnimator;
	private ObjectAnimator mScalingUpAnimator;

	private static final float NO_ALPHA = 1.0f, PARTIAL_ALPHA = 0.85f;
	private static final float NO_SCALE = 1.0f, SCALE = 0.9f;
	private static final float NO_X_TRANSLATION = 0f, X_TRANSLATION = 20f;

	public WearableListItemLayout(Context context) {
		this(context, null);
	}

	public WearableListItemLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}


	public WearableListItemLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mUnselectedCircleColor = Color.parseColor("#c1c1c1");
		mSelectedCircleColor = Color.parseColor("#3185ff");
		mPressedCircleColor = Color.parseColor("#2955c5");
		mSmallCircleRadius = getResources().getDimensionPixelSize(R.dimen.small_circle_radius);
		mBigCircleRadius = getResources().getDimensionPixelSize(R.dimen.big_circle_radius);

		// when expanded, the circle may extend beyond the bounds of the view
		setClipChildren(false);
	}


	/**
	 * Finalize inflating a view from XML. This is called as the last phase of inflation,
	 * after all child views have been added. Even if the subclass overrides onFinishInflate,
	 * they should always be sure to call the super method, so that we get called.
	 */
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mCircle = (CircledImageView) findViewById(R.id.image);
		mName = (TextView) findViewById(R.id.text);

		mScalingUpAnimator = ObjectAnimator.ofFloat(mCircle, "circleRadius", mBigCircleRadius);
		mScalingUpAnimator.setDuration(150L);

		mScalingDownAnimator = ObjectAnimator.ofFloat(mCircle, "circleRadius", mSmallCircleRadius);
		mScalingDownAnimator.setDuration(150L);
	}

	/**
	 * Called when this view becomes central item of the WearableListView.
	 */
	@Override
	public void onCenterPosition(boolean animate) {
		if(animate) {
			mScalingDownAnimator.cancel();
			if (!mScalingUpAnimator.isRunning() && mCircle.getCircleRadius() != mBigCircleRadius) {
				mScalingUpAnimator.start();
				mCircle.animate().scaleX(NO_SCALE).scaleY(NO_SCALE).translationX(NO_X_TRANSLATION).alpha(NO_ALPHA);
				mName.animate().scaleX(NO_SCALE).scaleY(NO_SCALE).translationX(NO_X_TRANSLATION).alpha(NO_ALPHA);
			}
		} else {
			mCircle.setCircleRadius(mBigCircleRadius);
            mName.setAlpha(NO_ALPHA);
		}

		//mCircle.setCircleColor(mSelectedCircleColor);
		mIsInCenter = true;
	}

	/**
	 * Called when this view stops being the central item of the WearableListView.	 *
	 */
	@Override
	public void onNonCenterPosition(boolean animate) {
		if(animate) {
			mScalingUpAnimator.cancel();
			if(!mScalingDownAnimator.isRunning() && mCircle.getCircleRadius() != mSmallCircleRadius) {
				mScalingDownAnimator.start();
				mCircle.animate().scaleX(SCALE).scaleY(SCALE).translationX(X_TRANSLATION).alpha(PARTIAL_ALPHA);
				mName.animate().scaleX(SCALE).scaleY(SCALE).translationX(X_TRANSLATION).alpha(PARTIAL_ALPHA);

			}
		} else {
			mCircle.setCircleRadius(mSmallCircleRadius);
            mName.setAlpha(PARTIAL_ALPHA);
		}

		//mCircle.setCircleColor(mUnselectedCircleColor);
		mIsInCenter = false;
	}

	/*
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mCircle = (CircledImageView) findViewById(R.id.image);
	}

	@Override
	public void onCenterPosition(boolean animate) {
		if (animate) {
			animate().alpha(NO_ALPHA).translationX(NO_X_TRANSLATION).start();
		}
		//mCircle.setCircleColor(mSelectedCircleColor);
		mCircle.setCircleRadius(mBigCircleRadius);
	}

	@Override
	public void onNonCenterPosition(boolean animate) {
		if (animate) {
			animate().alpha(PARTIAL_ALPHA).translationX(X_TRANSLATION).start();
		}
		//mCircle.setCircleColor(mUnselectedCircleColor);
		mCircle.setCircleRadius(mSmallCircleRadius);
	}
	*/

	/**
	 *  Called when this view has been tapped by the user.
	 */
	@Override
	public void setPressed(boolean pressed) {
		super.setPressed(pressed);
		if(mIsInCenter && pressed) {
			//mCircle.setCircleRadius(mSmallCircleRadius);
			//mCircle.animate().scaleX(0.9f).scaleY(0.9f);
			//mCircle.setCircleColor(mPressedCircleColor);
		}
		if(mIsInCenter && !pressed) {
			//mCircle.setCircleRadius(mSmallCircleRadius);
			//mCircle.animate().scaleX(1f).scaleY(1f);
			//mCircle.setCircleColor(mSelectedCircleColor);
		}
	}

}
