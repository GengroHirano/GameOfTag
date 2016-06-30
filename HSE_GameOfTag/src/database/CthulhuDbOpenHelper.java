package database;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class CthulhuDbOpenHelper extends SQLiteOpenHelper {

	private final static String DB = "Cthulhu.db" ;
	private final static int DB_VERSION = 1 ; 
	private Context mContext ;
	
	public CthulhuDbOpenHelper(Context context) {
		super(context, DB, null, DB_VERSION);
		mContext = context ;
	}

	//assetに格納したデータベースをコピーするための空のデータベースを作成
	public void createEmptyDataBase() throws IOException {
		boolean checkDB = checkDataBaseExists() ;
		if ( !checkDB ) { //存在してなかったら
			this.getReadableDatabase() ; //空のデータベースを作成
			try{
				//assetに格納したデータベースをコピーする
				copyDataBaseFromAsset() ;
			}
			catch (Exception e) {
				e.printStackTrace() ;
				throw new Error("Error copying database") ;
			}
		}
	}

	//再コピーを防止するために、既にデータベースが存在するかを判断(存在してたらtrue)
	private boolean checkDataBaseExists() {
		SQLiteDatabase checkDB = null ;
		try{
			String db_PATH = mContext.getDatabasePath(DB).getPath() ;
			checkDB = SQLiteDatabase.openDatabase(db_PATH, null, SQLiteDatabase.OPEN_READONLY) ;
		}
		catch (Exception e) {
			Log.v("データベース", "まだ存在せず") ;
		}
		if (checkDB != null) {
			checkDB.close() ;
		}
		return checkDB != null ? true : false ;
	}

	//assetに格納したデータベースをデフォルトのデータベースバスに作成した空のデータベースへコピーする
	private void copyDataBaseFromAsset() throws IOException{
		//asset内のデータベースファイルにアクセス
		InputStream in = mContext.getAssets().open(DB) ;
		//デフォルトのデータベースパスに作成した空のデータベース
		String dbName = mContext.getDatabasePath(DB).getPath() ;
		OutputStream out = new FileOutputStream(dbName) ;

		//コピー
		byte[] buffer = new byte[1024] ;
		int size ;
		while( (size = in.read(buffer)) > 0 ){
			out.write(buffer, 0, size) ;
		}
		out.flush() ;
		out.close() ;
		in.close() ;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

}
