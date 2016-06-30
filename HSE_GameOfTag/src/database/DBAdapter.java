package database;

import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DBAdapter {

	private CthulhuDbOpenHelper dbHelper ;
	private SQLiteDatabase db ;

	public DBAdapter(Context context) {	
		dbHelper = new CthulhuDbOpenHelper(context) ;
		try {
			dbHelper.createEmptyDataBase() ;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DBAdapter open(){
		db = dbHelper.getWritableDatabase() ;
		return this ;
	}

	public void close(Cursor c){
		c.close() ;
	}
	
	public Cursor getData(){
		String sql = "SELECT * FROM cthulhu ;" ;
		Cursor c = db.rawQuery(sql, null) ;
		return c ;
	}
}
