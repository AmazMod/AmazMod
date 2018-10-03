package com.amazmod.service.support;

import android.content.Context;
import android.support.wearable.view.GridViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class HorizontalGridViewPager extends GridViewPager {

    private final GestureDetector mGestureDetector;

    public HorizontalGridViewPager(Context context, AttributeSet attrs ) {
        super( context, attrs );
        mGestureDetector = new GestureDetector( context, new HScrollDetector() );
    }

    @Override
    public boolean onInterceptTouchEvent( MotionEvent ev ) {
        // If we have more horizontal than vertical scrolling, intercept the event,
        // otherwise let the child handle it
        return super.onInterceptTouchEvent( ev ) && mGestureDetector.onTouchEvent( ev );
    }

    class HScrollDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
            // Returns true if scrolling horizontally
            return ( Math.abs( distanceX ) > Math.abs( distanceY ) );
        }
    }
}