package aidl ;

/**
 * エンカウントサービスのコールバック
 */
interface EncountServiceCallback{
    /**
     * エンカウントした時に呼び出される
     * @param range 敵の行動範囲
     */
    void onEncount(int range) ;
}