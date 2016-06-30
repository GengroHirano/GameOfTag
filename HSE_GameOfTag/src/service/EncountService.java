package service;

import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import aidl.EncountServiceAidl;
import aidl.EncountServiceCallback;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.hse.hse_gameoftag.R;

public class EncountService extends Service {

	public interface EnemySerciveDelegate{
		public void onEncount() ;
	}

	public static final String DELAY_TIME = "DELAY"; //定期処理開始待ち時間を取得するキー
	public static final String ROUTINE_TIME = "ROUTINE"; //定期処理発行間隔を取得するキー

	private Resources mResources ; 
	private Timer mTimer ;
	private WeakReference<EncountServiceCallback> mCallback ; //エンカウントサービスのコールバック

	@Override
	public void onCreate() {
		super.onCreate();
		mResources = getResources() ;
	}

	@Override
	public IBinder onBind(Intent intent) {
		mTimer = new Timer(true) ;
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				boolean encount = lottery() ; //抽選開始
				if (!encount || mCallback.get() == null) {
					return ;
				}
				Log.v("tag", "that encount!!") ;
				//出現領域の抽選
				Random random = new Random() ;
				int range = random.nextInt(mResources.getInteger(R.integer.cage_level)) + 1 ;
				range *= 100 ;
				try {
					mCallback.get().onEncount(range) ;
				} catch (RemoteException e) {
					e.printStackTrace() ;
				}
			}
		},
		intent.getIntExtra(DELAY_TIME, 5000),
		intent.getIntExtra(ROUTINE_TIME, 5000)) ;
		return mEncountService;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v("UnBind", "MyService UnBinded") ;
		if (mTimer != null) {
			mTimer.cancel() ;
		}
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public boolean lottery(){
		int value = mResources.getInteger(R.integer.encount_probability) ;
		Random randam = new Random() ;
		int result = randam.nextInt(value) + 1 ;
		Log.v("result", ""+result) ;
		if (result <= mResources.getInteger(R.integer.encount)) {
			return true ;
		}
		return false ;
	}

	/**
	 * コールバックのセットaidl経由
	 */
	private final EncountServiceAidl.Stub mEncountService = new EncountServiceAidl.Stub() {

		@Override
		public void setCallback(EncountServiceCallback callback) throws RemoteException {
			mCallback = new WeakReference<EncountServiceCallback>(callback) ;
		}
	};


}
