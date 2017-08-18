package jp.juggler.lastimageviewer.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class MyFrameLayout extends FrameLayout{
	
	public MyFrameLayout( @NonNull Context context ){
		super( context );
	}
	
	public MyFrameLayout( @NonNull Context context, @Nullable AttributeSet attrs ){
		super( context, attrs );
	}
	
	public MyFrameLayout( @NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr ){
		super( context, attrs, defStyleAttr );
	}
	
	@TargetApi( 21 )
	public MyFrameLayout( @NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes ){
		super( context, attrs, defStyleAttr, defStyleRes );
	}
	
	public interface SizeChangedCallback{
		void onSizeChanged(int w,int h);
	}

	private SizeChangedCallback size_callback;

	public void setSizeChangedCallback(SizeChangedCallback size_callback){
		this.size_callback = size_callback;
	}
	
	@Override protected void onSizeChanged( int w, int h, int oldw, int oldh ){
		super.onSizeChanged( w, h, oldw, oldh );
		if( size_callback != null ) size_callback.onSizeChanged( w,h );
	}
}
