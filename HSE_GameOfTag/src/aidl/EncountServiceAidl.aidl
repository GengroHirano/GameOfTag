package aidl ;
 
import aidl.EncountServiceCallback ;
 
/**
 * エンカウントサービスのコールバックをセットするためのインターフェース
 */
interface EncountServiceAidl{
    /**
     * コールバックをセットする
     */
    void setCallback(EncountServiceCallback callback) ;
}