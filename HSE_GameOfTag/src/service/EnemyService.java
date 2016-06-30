package service;

import java.util.List;
import java.util.Random;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.hse.hse_gameoftag.GoogleMapFragment;
import com.hse.hse_gameoftag.R;

public class EnemyService extends IntentService {
	
	public EnemyService(){
		super("GEO_FANCE_SERVICE");
	}
	
	public EnemyService(String name) {
		super("GEO_FANCE_SERVICE");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.v("service", "発火") ;
		List<Geofence> list = LocationClient.getTriggeringGeofences(intent) ;
		if (list == null) {
			return ;
		}
		for (Geofence geofence : list) {
			//本来ならgeofenceのidによって振り分けを行う
			//こんな感じで
			Log.v("geofenceID", geofence.getRequestId()) ;
			int type = LocationClient.getGeofenceTransition(intent) ;
			if (type == Geofence.GEOFENCE_TRANSITION_ENTER) {
				Log.v("Event", "領域内") ;
				if (enemyAction()) {
					Intent recive = new Intent(GoogleMapFragment.ENEMY_RECEIVE) ;
					recive.putExtra(GoogleMapFragment.ENEMY_RESULT, GoogleMapFragment.ENEMY_ACTION) ;
					recive.putExtra(GoogleMapFragment.RISK_LEVEL, getRiskLevel()) ;
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(recive) ;
				}
			} else if(type == Geofence.GEOFENCE_TRANSITION_EXIT){
				Log.v("Event", "領域外") ;
				Intent recive = new Intent(GoogleMapFragment.ENEMY_RECEIVE) ;
				recive.putExtra(GoogleMapFragment.ENEMY_RESULT, GoogleMapFragment.ESCAPE) ;
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(recive) ;
			}
		}
		Log.v("count", ""+list.size()) ;
	}
	
	private boolean enemyAction(){
		int value = getResources().getInteger(R.integer.enemy_action_probability) ;
		Random randam = new Random() ;
		int result = randam.nextInt(value) + 1 ;
		Log.v("result", ""+result) ;
		if (result <= getResources().getInteger(R.integer.enemy_action)) {
			return true ;
		}
		return false ;
	}
	
	private int getRiskLevel(){
		int value = getResources().getInteger(R.integer.max_risk) / 3;
		Random randam = new Random() ;
		int result = randam.nextInt(value) + 1 ;
		return result ;
	}

}
