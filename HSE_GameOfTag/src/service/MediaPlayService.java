package service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.hse.hse_gameoftag.R;

public class MediaPlayService extends Service {

	private MediaPlayer mPlayer ;
	private MediaPlayBinder mBinder = new MediaPlayBinder();

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v("UnBind", "MyService UnBinded") ;
		stopBgm() ;
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		stopBgm() ;
		super.onDestroy();
	}

	public class MediaPlayBinder extends Binder{

		public MediaPlayService getService(){
			return MediaPlayService.this ;
		}

	}

	public void switchBgm(int riskLevel){
		switch (riskLevel - 1) {
		case 1:
			stopBgm() ;
			startBgm(R.raw.noise_level1) ;
			break;
		case 2:
			stopBgm() ;
			startBgm(R.raw.noise_level2) ;
			break;
		case 3:
			stopBgm() ;
			startBgm(R.raw.noise_level3) ;
			break;
		case 4:
			stopBgm() ;
			startBgm(R.raw.noise_level4) ;
			break;
		case 5:
			stopBgm() ;
			startBgm(R.raw.noise_level5) ;
			break;
		default:
			stopBgm() ;
			break ;
		}
	}

	public void stopBgm(){
		if (mPlayer == null) {
			return ;
		}
		if (mPlayer.isPlaying()) {
			mPlayer.stop() ;
		}
		mPlayer.release() ;
		mPlayer = null ;
	}

	private void startBgm(int bgmResId){
		mPlayer = MediaPlayer.create(getApplicationContext(), bgmResId) ;
		mPlayer.setLooping(true) ;
		mPlayer.start() ;
	}
}
