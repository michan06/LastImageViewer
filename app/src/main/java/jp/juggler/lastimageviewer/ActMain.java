package jp.juggler.lastimageviewer;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import jp.juggler.lastimageviewer.util.LogCategory;
import jp.juggler.lastimageviewer.util.Utils;
import jp.juggler.lastimageviewer.view.MyFrameLayout;

public class ActMain extends AppCompatActivity {
	static final LogCategory log = new LogCategory( "ActMain" );
	
	static final Uri media_image_uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
	
	@Override
	protected void onCreate( Bundle savedInstanceState ){
		super.onCreate( savedInstanceState );
		getWindow().addFlags(
			WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_FULLSCREEN
		);
		
		if( Build.VERSION.SDK_INT >= 19 ){
			View decor = this.getWindow().getDecorView();
			decor.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			);
		}
		
		initUI();
	}
	
	@Override protected void onDestroy(){
		super.onDestroy();
		clearBitmap();
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		
		if( Build.VERSION.SDK_INT >= 19 ){
			View decor = this.getWindow().getDecorView();
			decor.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			);
		}
		
		startWatching();
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		getContentResolver().unregisterContentObserver( mContentObserver );
	}
	
	private static final int PERMISSION_REQUEST_CODE = 1;
	
	@Override public void onRequestPermissionsResult(
		int requestCode
		, @NonNull String permissions[]
		, @NonNull int[] grantResults
	){
		switch( requestCode ){
		case PERMISSION_REQUEST_CODE:
			// If request is cancelled, the result arrays are empty.
			if( grantResults.length > 0 &&
				grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED
				){
				startWatching();
			}else{
				showError(getString(R.string.permission_error ));
			}
			break;
		}
	}
	
	////////////////////////////////////////////////////////////////////
	
	MyFrameLayout flRoot;
	TextView tvError;
	ImageView ivImage;
	Handler handler;
	int root_w;
	int root_h;
	
	private void initUI(){
		this.handler = new Handler();
		
		setContentView( R.layout.act_main );
		flRoot = (MyFrameLayout) findViewById( R.id.flRoot );
		flRoot.setSizeChangedCallback( new MyFrameLayout.SizeChangedCallback() {
			@Override public void onSizeChanged( int w, int h ){
				if( w == 0 || h == 0 ) return;
				root_w = w;
				root_h = h;
				if( pending_image != null ){
					startLoading( pending_image );
				}
			}
		} );
		tvError = flRoot.findViewById( R.id.tvError );
		ivImage = flRoot.findViewById( R.id.ivImage );
	}
	
	void showError( String text ){
		ivImage.setVisibility( View.GONE );
		tvError.setVisibility( View.VISIBLE );
		tvError.setText( text );
	}
	
	private final ContentObserver mContentObserver = new ContentObserver( handler ) {
		@Override public void onChange( boolean selfChange ){
			ImageInfo info = getLatestImageUri();
			if( info.uri == null ){
				log.e( "ContentObserver.onChange: can't get image Uri. %s", info.error );
				return;
			}
			startLoading( info );
		}
	};
	
	private static class ImageInfo {
		Uri uri;
		String mime_type;
		String error;
		
		ImageInfo(){
		}
		
		ImageInfo( String error ){
			this.error = error;
		}
		
	}
	
	@NonNull ImageInfo getLatestImageUri(){
		try{
			Cursor cursor = getContentResolver().query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI
				, null
				, null
				, null
				, MediaStore.Images.ImageColumns.DATE_MODIFIED + " desc limit 1"
			);
			if( cursor == null ){
				return new ImageInfo( "image query failed." );
			}
			
			try{
				if( cursor.getCount() <= 0 ){
					return new ImageInfo( "image query result is empty." );
				}
				
				cursor.moveToNext();
				int idx_id = cursor.getColumnIndex( MediaStore.Images.ImageColumns._ID );
				int idx_mime_type = cursor.getColumnIndex( MediaStore.Images.ImageColumns.MIME_TYPE );
				
				ImageInfo info = new ImageInfo();
				info.uri = ContentUris.withAppendedId( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getInt( idx_id ) );
				info.mime_type = cursor.isNull( idx_mime_type ) ? null : cursor.getString( idx_mime_type );
				return info;
			}finally{
				cursor.close();
			}
		}catch( Throwable ex ){
			log.trace( ex );
			return new ImageInfo( String.format( "image query failed. %s %s", ex.getClass().getSimpleName(), ex.getMessage() ) );
		}
	}
	
	ImageInfo pending_image;
	
	Uri showing_uri;
	Bitmap showing_bitmap;
	
	void clearBitmap(){
		ivImage.setImageDrawable( null );
		if( showing_bitmap != null ){
			showing_bitmap.recycle();
		}
	}

	
	void startWatching(){
		
		int permissionCheck = ContextCompat.checkSelfPermission( this, Manifest.permission.READ_EXTERNAL_STORAGE );
		if( permissionCheck != PackageManager.PERMISSION_GRANTED ){
			if( Build.VERSION.SDK_INT >= 23 ){

				showError( "check app permission..." );

				// No explanation needed, we can request the permission.
				
				ActivityCompat.requestPermissions( this
					, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE }
					, PERMISSION_REQUEST_CODE
				);
				return;
			}

			showError(getString(R.string.permission_error ));
			return;
		}
		
		getContentResolver().registerContentObserver( media_image_uri, true, mContentObserver );
		ImageInfo info = getLatestImageUri();
		if( info.uri == null ){
			showError( info.error );
			return;
		}
		startLoading( info );
	}
	
	
	void startLoading( @NonNull final ImageInfo info ){
		if( root_w == 0 || root_h == 0 ){
			log.d( "startLoading: pending_image %s", info.uri );
			pending_image = info;
			return;
		}
		pending_image = null;
		
		if( info.uri.equals( showing_uri ) ){
			log.d( "startLoading: already showing uri %s", showing_uri );
			return;
		}
		
		showError("loading image...");
		showing_uri = info.uri;
		log.d("loading %s",info.uri);
		
		AsyncTask< Void, Void, Bitmap > task = new AsyncTask< Void, Void, Bitmap >() {
			
			@Override protected Bitmap doInBackground( Void... voids ){
				return Utils.createResizedBitmap( log, ActMain.this, info.uri, false, root_w, root_h );
			}
			
			@Override protected void onCancelled( Bitmap bitmap ){
				onPostExecute( bitmap );
			}
			
			@Override protected void onPostExecute( Bitmap bitmap ){
				if( bitmap == null ) return;
				
				clearBitmap();
				showing_bitmap = bitmap;
				ivImage.setImageBitmap( showing_bitmap );
				ivImage.setVisibility( View.VISIBLE );
				tvError.setVisibility( View.GONE );
			}
		};
		task.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR );
	}
}
