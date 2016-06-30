package com.hse.hse_gameoftag;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import service.EncountService;
import service.EnemyService;
import service.MediaPlayService;
import service.MediaPlayService.MediaPlayBinder;
import aidl.EncountServiceAidl;
import aidl.EncountServiceCallback;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
/**
 * GoogleMapの描画とserviceのコールバックを受け取るFragment
 * 今回はすべての処理の起点をここに集約してみた。
 * ServiceのAidlを使ったコールバックのパターンと
 * Binderを拡張したコールバックのパターンも実装している。
 * 今回のようにローカルで完結するであろうserviceの場合は後者の
 * Binderを拡張したパターンを使うべし。・・・え？なんでaidlを使ったかって?
 * バックグラウンド制御のお題だったもんで調子に乗って実装しちゃいました☆
 * @author ootaakihiro
 *
 */
public class GoogleMapFragment extends Fragment implements GooglePlayServicesClient.ConnectionCallbacks, 
OnConnectionFailedListener, 
OnAddGeofencesResultListener{

	private final int ENCOUNT = 555 ;

	public static final String ENEMY_RECEIVE = "ENEMY_RECEIVE" ; //追跡者に居場所を通知する
	public static final String ENEMY_RESULT = "ENEMY_RESULT" ; //追跡者のアクションを取得する
	public static final String ENEMY_ACTION = "ENEMY_ACTION" ; //追跡者のアクション
	public static final String RISK_LEVEL = "RISK_LEVEL" ; //危険度
	public static final String ESCAPE = "ESCAPE" ; //逃走成功

	private GoogleMap mMap ;
	private EncountServiceAidl mEncount ;
	private LocationClient mLocationClient ;
	private LatLng mLocation ;
	private HSE_GameOfTag mApp ;
	private MediaPlayService mPlayService ;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLocationClient = new LocationClient(getActivity().getApplicationContext(), this, this);
		LocalBroadcastManager
		.getInstance(getActivity().getApplicationContext())
		.registerReceiver(mBroadcastReceiver, new IntentFilter(ENEMY_RECEIVE)) ;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_map, container, false) ;
		mMap = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap() ;
		mMap.setMyLocationEnabled(true) ;
		mApp = (HSE_GameOfTag)getActivity().getApplication() ;


		final int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity().getApplicationContext());
		if (result != ConnectionResult.SUCCESS) {
			Toast.makeText(getActivity().getApplicationContext(), 
					"googlePlayServiceが使えないからこのアプリは使えんよ (status=" + result + ")",
					Toast.LENGTH_LONG).show();
			getActivity().finish();
		}

		return root ;
	}

	@Override
	public void onResume() {
		super.onResume();

		Intent intent = new Intent(getActivity().getApplicationContext()
				, EncountService.class) ;
		intent.putExtra(EncountService.DELAY_TIME, 1000) ;
		intent.putExtra(EncountService.ROUTINE_TIME, 1000) ;
		getActivity().bindService(intent
				, mServiceConnection
				, Service.BIND_AUTO_CREATE) ;

		Intent mediaIntent = new Intent(getActivity().getApplicationContext()
				, MediaPlayService.class) ;
		getActivity().bindService(mediaIntent
				, mPlayerServiceConnection
				, Service.BIND_AUTO_CREATE) ;

		if(!mLocationClient.isConnected() || !mLocationClient.isConnected()){
			mLocationClient.connect() ;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unbindService(mServiceConnection) ;
		getActivity().unbindService(mPlayerServiceConnection) ;
		mLocationClient.removeLocationUpdates(mLocationListener) ;
		LocalBroadcastManager
		.getInstance(getActivity().getApplicationContext())
		.unregisterReceiver(mBroadcastReceiver) ;
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		Log.v("Filed", "位置情報サービス無効") ;
	}

	@Override
	public void onConnected(Bundle arg0) {
		Log.v("Connected", "位置情報サービス接続") ;
		requestUpdate() ;
	}

	@Override
	public void onDisconnected() {
		Log.v("Disconnect", "切断") ;
	}

	@Override
	public void onAddGeofencesResult(int arg0, String[] arg1) {
	}

	private void requestUpdate() {
		LocationRequest req = LocationRequest.create();
		req.setFastestInterval(500);
		req.setInterval(500);
		req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationClient.requestLocationUpdates(req, mLocationListener) ;
	}

	/**
	 * 位置情報の変化をキャッチするリスナ
	 */
	private LocationListener mLocationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location loc) {
			// 更新された位置情報を使う処理
			Log.v(""+loc.getLatitude(), ""+loc.getLongitude()) ;
			mLocation = new LatLng(loc.getLatitude(), loc.getLongitude()) ;
			CameraPosition cp = new CameraPosition.Builder().target(mLocation).zoom(15.0f).build() ;
			mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp)) ;
		}
	};

	/**
	 * ジオフェンス内のイベントを受け取るレシーバ
	 */
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getStringExtra(ENEMY_RESULT).equals(ESCAPE)) {
				onEscape() ;
			} else if (intent.getStringExtra(ENEMY_RESULT).equals(ENEMY_ACTION)) {
				onRiskLevelChanged(intent.getIntExtra(RISK_LEVEL, 0)) ;
			}
		}
	};

	/**
	 * 追跡者が行動した時に呼ばれる
	 * @param chengeLevel 危険度
	 */
	public void onRiskLevelChanged(int chengeLevel) {
		mApp.addRiskLevel(chengeLevel) ;
		Log.v("riskLevel", ""+mApp.getRiskLevel()) ;
		if (mApp.getRiskLevel() <= getResources().getInteger(R.integer.max_risk)) {
			if (mPlayService == null) {
				return ;
			}
			mPlayService.switchBgm(mApp.getRiskLevel()) ;
		} else {
			gameOver() ;
		}
	}

	/**
	 * 逃げ切った時に呼び出される
	 */
	public void onEscape() {
		mApp.setRiskLevel(getResources().getInteger(R.integer.min_risk)) ;
		if (mPlayService == null) {
			return ;
		}
		mPlayService.stopBgm() ;

	}

	public void gameOver(){
		Toast.makeText(getActivity().getApplicationContext(), 
				"お前の負けだ", 
				Toast.LENGTH_SHORT).show() ;
		getActivity().finish() ;
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		/*
		 * (非 Javadoc)
		 * 超やべぇサービスのエラーが発生した時に呼び出される。
		 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
		 */
		@Override
		public void onServiceDisconnected(ComponentName name) {
			getActivity().unbindService(mServiceConnection) ;
		}

		/*
		 * (非 Javadoc)
		 * サービスをbind接続した時に呼び出される。
		 * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
		 */
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.v("Connection", "MyServiceConnected") ;
			mEncount = EncountServiceAidl.Stub.asInterface(service) ;
			try {
				mEncount.setCallback(mEncountCallback) ;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	private ServiceConnection mPlayerServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			getActivity().unbindService(mPlayerServiceConnection) ;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MediaPlayBinder binder = (MediaPlayBinder)service ;
			mPlayService = binder.getService() ;
		}
	};

	/*
	 * エンカウントサービスのコールバック
	 */
	private EncountServiceCallback.Stub mEncountCallback = new EncountServiceCallback.Stub() {

		/*
		 * (非 Javadoc)
		 * エンカウントイベントが発生した時に呼び出される
		 * @see aidl.EncountServiceCallback#onEncount(int)
		 */
		@Override
		public void onEncount(int range) throws RemoteException {
			Log.v("tag", "let'Lock!") ;
			Message message = mEncountHandler.obtainMessage(ENCOUNT, range, 0, GoogleMapFragment.this) ;
			mEncountHandler.sendMessage(message) ;
		}

	};

	/*
	 * サービスで呼び出すハンドラ
	 * ぶっちゃけメモリリークが心配
	 */
	private static Handler mEncountHandler = new Handler(){

		@Override
		public void handleMessage(android.os.Message msg) {
			WeakReference<GoogleMapFragment> fragment =
					new WeakReference<GoogleMapFragment>((GoogleMapFragment)msg.obj) ;
			if (fragment.get() == null) {
				return ;
			}
			if (fragment.get().ENCOUNT == msg.what) {
				if (fragment.get().mLocation == null) {
					return ;
				}
				//ジオフェンス追加処理
				int range = msg.arg1 ;
				ArrayList<Geofence> fenceList = new ArrayList<Geofence>();
				Geofence geofence = new Geofence.Builder()
				.setRequestId("Fence-1")
				.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
				.setCircularRegion(fragment.get().mLocation.latitude,
						fragment.get().mLocation.longitude,
						range)
						.setExpirationDuration(Geofence.NEVER_EXPIRE)
						.build();
				fenceList.add(geofence);

				//ジオフェンス可視化処理
				fragment.get().mMap.clear() ;
				CircleOptions circleOptions = new CircleOptions() ;
				circleOptions.center(fragment.get().mLocation) ;
				circleOptions.radius(range) ;
				HSE_GameOfTag app = (HSE_GameOfTag)fragment.get().getActivity().getApplication() ;
				int boundary = fragment.get().getResources().getInteger(R.integer.max_risk) / 2 ;
				if (app.getRiskLevel() > boundary) {
					circleOptions.strokeColor(Color.BLUE) ;
				} else {
					circleOptions.strokeColor(Color.TRANSPARENT) ;
				}
				circleOptions.strokeWidth(2f) ;
				fragment.get().mMap.addCircle(circleOptions) ;

				//ペンディングインテントの作成
				final Intent intent = new Intent(fragment.get().getActivity().getApplicationContext(), EnemyService.class);
				PendingIntent pendingIntent = PendingIntent.getService(fragment.get().getActivity().getApplicationContext(), 
						0, 
						intent, 
						PendingIntent.FLAG_UPDATE_CURRENT);
				fragment.get().mLocationClient.addGeofences(fenceList, pendingIntent, fragment.get());
			}
		};

	} ;
}
