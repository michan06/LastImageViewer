package jp.juggler.lastimageviewer.util;

import android.content.ContentValues;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import android.util.Log;

public class LogCategory {
	
	private final ContentValues cv = new ContentValues();
	private final String category;
	private final String TAG = "LastImageViewer";
	
	public LogCategory( @NonNull String category ){
		this.category = category;
	}
	
	@SuppressWarnings("unused")
	public void e( @NonNull String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		synchronized( cv ){
			Log.e( TAG, category + ": " + fmt );
		}
	}
	
	@SuppressWarnings("unused")
	public void w( @NonNull String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		synchronized( cv ){
			Log.w( TAG, category + ": " + fmt );
		}
	}
	
	@SuppressWarnings("unused")
	public void i( @NonNull String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		synchronized( cv ){
			Log.i( TAG, category + ": " + fmt );
		}
	}
	
	@SuppressWarnings("unused")
	public void v( @NonNull String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		synchronized( cv ){
			Log.v( TAG, category + ": " + fmt );
		}
	}
	
	@SuppressWarnings("unused")
	public void d( @NonNull String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		synchronized( cv ){
			Log.d( TAG, category + ": " + fmt );
		}
	}
	
	@SuppressWarnings("unused")
	public void e( @NonNull Resources res, int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		synchronized( cv ){
			Log.e( TAG, category + ": " + fmt );
		}
	}
	
	@SuppressWarnings("unused")
	public void w( @NonNull Resources res, int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		synchronized( cv ){
			Log.w( TAG, category + ": " + fmt );
		}
	}
	
	@SuppressWarnings("unused")
	public void i( @NonNull Resources res, int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		synchronized( cv ){
			Log.i( TAG, category + ": " + fmt );
		}
	}
	
	@SuppressWarnings("unused")
	public void v( @NonNull Resources res, int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		synchronized( cv ){
			Log.v( TAG, category + ": " + fmt );
		}
	}
	
	@SuppressWarnings("unused")
	public void d( @NonNull Resources res, int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		synchronized( cv ){
			Log.d( TAG, category + ": " + fmt );
		}
	}
	
	@SuppressWarnings("unused")
	public void e( @NonNull Throwable ex, @NonNull String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		synchronized( cv ){
			Log.e( TAG, category + ": " + fmt + String.format( ":%s %s", ex.getClass().getSimpleName(), ex.getMessage() ) );
		}
	}
	
	@SuppressWarnings("unused")
	public void e( @NonNull Throwable ex, @NonNull Resources res, int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		synchronized( cv ){
			Log.e( TAG, category + ": " + fmt + String.format( ":%s %s", ex.getClass().getSimpleName(), ex.getMessage() ) );
		}
	}
	
	/**
	 * Caption  for labeling causative exception stack traces
	 */
	private static final String CAUSE_CAPTION = "Caused by: ";
	
	/**
	 * Caption for labeling suppressed exception stack traces
	 */
	private static final String SUPPRESSED_CAPTION = "Suppressed: ";
	
	@SuppressWarnings("unused")
	public void trace( @NonNull Throwable ex ){
		//// ex.printStackTrace();
		
		// Guard against malicious overrides of Throwable.equals by
		// using a Set with identity equality semantics.
		Set< Throwable > dejaVu = Collections.newSetFromMap( new IdentityHashMap< Throwable, Boolean >() );
		dejaVu.add( ex );
		
		// Print our stack trace
		e( ex.toString() );
		
		StackTraceElement[] trace = ex.getStackTrace();
		for( StackTraceElement traceElement : trace ){
			e( "\tat " + traceElement );
		}
		
		if( Build.VERSION.SDK_INT >= 19 ){
			// Print suppressed exceptions, if any
			for( Throwable se : ex.getSuppressed() )
				printEnclosedStackTrace( se, trace, SUPPRESSED_CAPTION, "\t", dejaVu );
		}
		
		// Print cause, if any
		Throwable ourCause = ex.getCause();
		if( ourCause != null )
			printEnclosedStackTrace( ourCause, trace, CAUSE_CAPTION, "", dejaVu );
	}
	
	/**
	 * Print our stack trace as an enclosed exception for the specified
	 * stack trace.
	 */
	private void printEnclosedStackTrace(
		@NonNull Throwable ex
		, @NonNull StackTraceElement[] enclosingTrace
		, String caption
		, String prefix
		, Set< Throwable > dejaVu
	){
		if( dejaVu.contains( ex ) ){
			e( "\t[CIRCULAR REFERENCE:" + ex + "]" );
		}else{
			dejaVu.add( ex );
			// Compute number of frames in common between this and enclosing trace
			StackTraceElement[] trace = ex.getStackTrace();
			int m = trace.length - 1;
			int n = enclosingTrace.length - 1;
			while( m >= 0 && n >= 0 && trace[ m ].equals( enclosingTrace[ n ] ) ){
				m--;
				n--;
			}
			int framesInCommon = trace.length - 1 - m;
			
			// Print our stack trace
			e( prefix + caption + this );
			for( int i = 0 ; i <= m ; i++ )
				e( prefix + "\tat " + trace[ i ] );
			if( framesInCommon != 0 )
				e( prefix + "\t... " + framesInCommon + " more" );
			
			if( Build.VERSION.SDK_INT >= 19 ){
				// Print suppressed exceptions, if any
				for( Throwable ex2 : ex.getSuppressed() )
					printEnclosedStackTrace( ex2, trace, SUPPRESSED_CAPTION, prefix + "\t", dejaVu );
			}
			
			// Print cause, if any
			Throwable ourCause = ex.getCause();
			if( ourCause != null )
				printEnclosedStackTrace( ourCause, trace, CAUSE_CAPTION, prefix, dejaVu );
		}
	}
	
}