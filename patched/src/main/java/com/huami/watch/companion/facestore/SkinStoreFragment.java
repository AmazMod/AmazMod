package com.huami.watch.companion.facestore;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;

import com.edotasx.amazfit.Constants;
import com.huami.midong.webview.jsbridge.JsBridgeWebView;
import com.huami.midong.webview.jsbridge.JsCallBackFunction;
import com.huami.midong.webview.nativejsapi.IJsBridgeCallback;
import com.huami.midong.webview.nativejsapi.JsBridgeNativeAPI;
import com.huami.midong.webview.nativejsapi.jsshare.JSShareCreator;
import com.huami.midong.webview.utils.Debug;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexAdd;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 12/04/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class SkinStoreFragment extends android.support.v4.app.Fragment implements IJsBridgeCallback {
    @DexIgnore
    private JsBridgeNativeAPI d;
    @DexIgnore
    private JsBridgeWebView b;


    @DexIgnore
    @Override
    public void doShare(JSShareCreator.Content content) {

    }

    @DexWrap
    @Override
    public void loadPage(String s) {
        if (TextUtils.isEmpty(s)) {
            Log.d(Constants.TAG, "Url is empty!! ");
            return;
        }
        Log.d(Constants.TAG, " url address : " + s);
        this.d.registerAllNativeApi();
        WebSettings webSettings = this.b.getSettings();
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        this.b.loadUrl(Constants.WATCHFACES_STORE_URL, new CustomJsCallback(b));
        //this.b.registerHandler();
    }

    @DexWrap
    private void a(String string2) {
        Log.d(Constants.TAG, string2);

        if ((string2 != null) && (string2.indexOf("onPageFinished") > -1)) {
            String javascript = "((function(){if(window.AmazMod)console.log(\"AmazMod bridge yet initialized!\");else{window.AmazMod=!0;try{HM_JsBridge.init(!0)}catch(t){}var t=\"https://sawfb.fabiobarbon.click/\";$(\".media\").find(\".download_button\").html('<i class=\"fa fa-mobile\"></i> Send to device').click(function(e){e.preventDefault();var o=$(this).closest(\".media\"),i=t+$(this).attr(\"href\").substr(1),a=t+o.find(\"img\").attr(\"src\"),s=i.split(\"/\");n(r(i,(s[s.length-2]||\"\").replace(/ /gi,\"_\"),a))}),$(\"div.col-sm-7>a[type=button]>i.fa-download\").closest(\"a\").html('<i class=\"fa fa-mobile\"></i> Send to device').click(function(e){e.preventDefault();var o=t+$(this).closest(\".row\").find(\"img\").attr(\"src\"),i=t+$(this).attr(\"href\").substr(1),a=i.split(\"/\");n(r(i,(a[a.length-2]||\"\").replace(/ /gi,\"_\"),o))}),console.log(\"AmazMod bridged initialied\");var e={init:function(t){t&&(this.construct(),window.location.href=\"#openModalLoading\")},destroy:function(){$(\"#openModalLoading\").remove()},construct:function(){this.appendHtml(document.body,'<div id=\"openModalLoading\" class=\"modalDialog\"><div><div class=\"loading-spinner\"></div></div></div>'),this.appendCss()},appendHtml:function(t,e){var r=document.createElement(\"div\");for(r.innerHTML=e;r.children.length>0;)t.appendChild(r.children[0])},appendCss:function(){var t=\".modalDialog {position: fixed;font-family: Arial, Helvetica, sans-serif;top: 0;right: 0;bottom: 0;left: 0; background: rgba(0, 0, 0, 0.8);z-index: 99999;opacity:0; -webkit-transition: opacity 400ms ease-in; -moz-transition: opacity 400ms ease-in;transition: opacity 400ms ease-in; pointer-events: none;}  .modalDialog:target {opacity:1;pointer-events: auto;}  .modalDialog > div {width: 100%;position: relative;margin: 20% auto;}@-webkit-keyframes rotate-forever { 0% { -webkit-transform: rotate(0deg); -moz-transform: rotate(0deg); -ms-transform: rotate(0deg); -o-transform: rotate(0deg); transform: rotate(0deg); } 100% { -webkit-transform: rotate(360deg); -moz-transform: rotate(360deg); -ms-transform: rotate(360deg); -o-transform: rotate(360deg); transform: rotate(360deg); } } @-moz-keyframes rotate-forever { 0% { -webkit-transform: rotate(0deg); -moz-transform: rotate(0deg); -ms-transform: rotate(0deg); -o-transform: rotate(0deg); transform: rotate(0deg); } 100% { -webkit-transform: rotate(360deg); -moz-transform: rotate(360deg); -ms-transform: rotate(360deg); -o-transform: rotate(360deg); transform: rotate(360deg); } } @keyframes rotate-forever { 0% { -webkit-transform: rotate(0deg); -moz-transform: rotate(0deg); -ms-transform: rotate(0deg); -o-transform: rotate(0deg); transform: rotate(0deg); } 100% { -webkit-transform: rotate(360deg); -moz-transform: rotate(360deg); -ms-transform: rotate(360deg); -o-transform: rotate(360deg); transform: rotate(360deg); } } .loading-spinner { -webkit-animation-duration: 0.75s; -moz-animation-duration: 0.75s; animation-duration: 0.75s; -webkit-animation-iteration-count: infinite; -moz-animation-iteration-count: infinite; animation-iteration-count: infinite; -webkit-animation-name: rotate-forever; -moz-animation-name: rotate-forever; animation-name: rotate-forever; -webkit-animation-timing-function: linear; -moz-animation-timing-function: linear; animation-timing-function: linear; height: 30px; width: 30px; border: 8px solid #ffffff; border-right-color: transparent; border-radius: 50%; display: inline-block; }.loading-spinner { position: absolute; top: 50%; right: 0; bottom: 0; left: 50%; margin: -15px 0 -15px;}\",e=document.head||document.getElementsByTagName(\"head\")[0],r=document.createElement(\"style\");r.type=\"text/css\",r.styleSheet?r.styleSheet.cssText=t:r.appendChild(document.createTextNode(t)),e.appendChild(r)}};!function(t){if(\"object\"==typeof exports)module.exports=t();else if(\"function\"==typeof define&&define.amd)define(t);else{var e;try{e=window}catch(t){e=self}e.SparkMD5=t()}}(function(t){\"use strict\";var e=[\"0\",\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\",\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"];function r(t,e){var r=t[0],n=t[1],o=t[2],i=t[3];n=((n+=((o=((o+=((i=((i+=((r=((r+=(n&o|~n&i)+e[0]-680876936|0)<<7|r>>>25)+n|0)&n|~r&o)+e[1]-389564586|0)<<12|i>>>20)+r|0)&r|~i&n)+e[2]+606105819|0)<<17|o>>>15)+i|0)&i|~o&r)+e[3]-1044525330|0)<<22|n>>>10)+o|0,n=((n+=((o=((o+=((i=((i+=((r=((r+=(n&o|~n&i)+e[4]-176418897|0)<<7|r>>>25)+n|0)&n|~r&o)+e[5]+1200080426|0)<<12|i>>>20)+r|0)&r|~i&n)+e[6]-1473231341|0)<<17|o>>>15)+i|0)&i|~o&r)+e[7]-45705983|0)<<22|n>>>10)+o|0,n=((n+=((o=((o+=((i=((i+=((r=((r+=(n&o|~n&i)+e[8]+1770035416|0)<<7|r>>>25)+n|0)&n|~r&o)+e[9]-1958414417|0)<<12|i>>>20)+r|0)&r|~i&n)+e[10]-42063|0)<<17|o>>>15)+i|0)&i|~o&r)+e[11]-1990404162|0)<<22|n>>>10)+o|0,n=((n+=((o=((o+=((i=((i+=((r=((r+=(n&o|~n&i)+e[12]+1804603682|0)<<7|r>>>25)+n|0)&n|~r&o)+e[13]-40341101|0)<<12|i>>>20)+r|0)&r|~i&n)+e[14]-1502002290|0)<<17|o>>>15)+i|0)&i|~o&r)+e[15]+1236535329|0)<<22|n>>>10)+o|0,n=((n+=((o=((o+=((i=((i+=((r=((r+=(n&i|o&~i)+e[1]-165796510|0)<<5|r>>>27)+n|0)&o|n&~o)+e[6]-1069501632|0)<<9|i>>>23)+r|0)&n|r&~n)+e[11]+643717713|0)<<14|o>>>18)+i|0)&r|i&~r)+e[0]-373897302|0)<<20|n>>>12)+o|0,n=((n+=((o=((o+=((i=((i+=((r=((r+=(n&i|o&~i)+e[5]-701558691|0)<<5|r>>>27)+n|0)&o|n&~o)+e[10]+38016083|0)<<9|i>>>23)+r|0)&n|r&~n)+e[15]-660478335|0)<<14|o>>>18)+i|0)&r|i&~r)+e[4]-405537848|0)<<20|n>>>12)+o|0,n=((n+=((o=((o+=((i=((i+=((r=((r+=(n&i|o&~i)+e[9]+568446438|0)<<5|r>>>27)+n|0)&o|n&~o)+e[14]-1019803690|0)<<9|i>>>23)+r|0)&n|r&~n)+e[3]-187363961|0)<<14|o>>>18)+i|0)&r|i&~r)+e[8]+1163531501|0)<<20|n>>>12)+o|0,n=((n+=((o=((o+=((i=((i+=((r=((r+=(n&i|o&~i)+e[13]-1444681467|0)<<5|r>>>27)+n|0)&o|n&~o)+e[2]-51403784|0)<<9|i>>>23)+r|0)&n|r&~n)+e[7]+1735328473|0)<<14|o>>>18)+i|0)&r|i&~r)+e[12]-1926607734|0)<<20|n>>>12)+o|0,n=((n+=((o=((o+=((i=((i+=((r=((r+=(n^o^i)+e[5]-378558|0)<<4|r>>>28)+n|0)^n^o)+e[8]-2022574463|0)<<11|i>>>21)+r|0)^r^n)+e[11]+1839030562|0)<<16|o>>>16)+i|0)^i^r)+e[14]-35309556|0)<<23|n>>>9)+o|0,n=((n+=((o=((o+=((i=((i+=((r=((r+=(n^o^i)+e[1]-1530992060|0)<<4|r>>>28)+n|0)^n^o)+e[4]+1272893353|0)<<11|i>>>21)+r|0)^r^n)+e[7]-155497632|0)<<16|o>>>16)+i|0)^i^r)+e[10]-1094730640|0)<<23|n>>>9)+o|0,n=((n+=((o=((o+=((i=((i+=((r=((r+=(n^o^i)+e[13]+681279174|0)<<4|r>>>28)+n|0)^n^o)+e[0]-358537222|0)<<11|i>>>21)+r|0)^r^n)+e[3]-722521979|0)<<16|o>>>16)+i|0)^i^r)+e[6]+76029189|0)<<23|n>>>9)+o|0,n=((n+=((o=((o+=((i=((i+=((r=((r+=(n^o^i)+e[9]-640364487|0)<<4|r>>>28)+n|0)^n^o)+e[12]-421815835|0)<<11|i>>>21)+r|0)^r^n)+e[15]+530742520|0)<<16|o>>>16)+i|0)^i^r)+e[2]-995338651|0)<<23|n>>>9)+o|0,n=((n+=((i=((i+=(n^((r=((r+=(o^(n|~i))+e[0]-198630844|0)<<6|r>>>26)+n|0)|~o))+e[7]+1126891415|0)<<10|i>>>22)+r|0)^((o=((o+=(r^(i|~n))+e[14]-1416354905|0)<<15|o>>>17)+i|0)|~r))+e[5]-57434055|0)<<21|n>>>11)+o|0,n=((n+=((i=((i+=(n^((r=((r+=(o^(n|~i))+e[12]+1700485571|0)<<6|r>>>26)+n|0)|~o))+e[3]-1894986606|0)<<10|i>>>22)+r|0)^((o=((o+=(r^(i|~n))+e[10]-1051523|0)<<15|o>>>17)+i|0)|~r))+e[1]-2054922799|0)<<21|n>>>11)+o|0,n=((n+=((i=((i+=(n^((r=((r+=(o^(n|~i))+e[8]+1873313359|0)<<6|r>>>26)+n|0)|~o))+e[15]-30611744|0)<<10|i>>>22)+r|0)^((o=((o+=(r^(i|~n))+e[6]-1560198380|0)<<15|o>>>17)+i|0)|~r))+e[13]+1309151649|0)<<21|n>>>11)+o|0,n=((n+=((i=((i+=(n^((r=((r+=(o^(n|~i))+e[4]-145523070|0)<<6|r>>>26)+n|0)|~o))+e[11]-1120210379|0)<<10|i>>>22)+r|0)^((o=((o+=(r^(i|~n))+e[2]+718787259|0)<<15|o>>>17)+i|0)|~r))+e[9]-343485551|0)<<21|n>>>11)+o|0,t[0]=r+t[0]|0,t[1]=n+t[1]|0,t[2]=o+t[2]|0,t[3]=i+t[3]|0}function n(t){var e,r=[];for(e=0;e<64;e+=4)r[e>>2]=t.charCodeAt(e)+(t.charCodeAt(e+1)<<8)+(t.charCodeAt(e+2)<<16)+(t.charCodeAt(e+3)<<24);return r}function o(t){var e,r=[];for(e=0;e<64;e+=4)r[e>>2]=t[e]+(t[e+1]<<8)+(t[e+2]<<16)+(t[e+3]<<24);return r}function i(t){var e,o,i,a,s,f,h=t.length,u=[1732584193,-271733879,-1732584194,271733878];for(e=64;e<=h;e+=64)r(u,n(t.substring(e-64,e)));for(o=(t=t.substring(e-64)).length,i=[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],e=0;e<o;e+=1)i[e>>2]|=t.charCodeAt(e)<<(e%4<<3);if(i[e>>2]|=128<<(e%4<<3),e>55)for(r(u,i),e=0;e<16;e+=1)i[e]=0;return a=(a=8*h).toString(16).match(/(.*?)(.{0,8})$/),s=parseInt(a[2],16),f=parseInt(a[1],16)||0,i[14]=s,i[15]=f,r(u,i),u}function a(t){var r,n=\"\";for(r=0;r<4;r+=1)n+=e[t>>8*r+4&15]+e[t>>8*r&15];return n}function s(t){var e;for(e=0;e<t.length;e+=1)t[e]=a(t[e]);return t.join(\"\")}function f(t){return/[\\u0080-\\uFFFF]/.test(t)&&(t=unescape(encodeURIComponent(t))),t}function h(t){var e,r=[],n=t.length;for(e=0;e<n-1;e+=2)r.push(parseInt(t.substr(e,2),16));return String.fromCharCode.apply(String,r)}function u(){this.reset()}return\"5d41402abc4b2a76b9719d911017c592\"!==s(i(\"hello\"))&&function(t,e){var r=(65535&t)+(65535&e);return(t>>16)+(e>>16)+(r>>16)<<16|65535&r},\"undefined\"==typeof ArrayBuffer||ArrayBuffer.prototype.slice||function(){function e(t,e){return(t=0|t||0)<0?Math.max(t+e,0):Math.min(t,e)}ArrayBuffer.prototype.slice=function(r,n){var o,i,a,s,f=this.byteLength,h=e(r,f),u=f;return n!==t&&(u=e(n,f)),h>u?new ArrayBuffer(0):(o=u-h,i=new ArrayBuffer(o),a=new Uint8Array(i),s=new Uint8Array(this,h,o),a.set(s),i)}}(),u.prototype.append=function(t){return this.appendBinary(f(t)),this},u.prototype.appendBinary=function(t){this._buff+=t,this._length+=t.length;var e,o=this._buff.length;for(e=64;e<=o;e+=64)r(this._hash,n(this._buff.substring(e-64,e)));return this._buff=this._buff.substring(e-64),this},u.prototype.end=function(t){var e,r,n=this._buff,o=n.length,i=[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0];for(e=0;e<o;e+=1)i[e>>2]|=n.charCodeAt(e)<<(e%4<<3);return this._finish(i,o),r=s(this._hash),t&&(r=h(r)),this.reset(),r},u.prototype.reset=function(){return this._buff=\"\",this._length=0,this._hash=[1732584193,-271733879,-1732584194,271733878],this},u.prototype.getState=function(){return{buff:this._buff,length:this._length,hash:this._hash}},u.prototype.setState=function(t){return this._buff=t.buff,this._length=t.length,this._hash=t.hash,this},u.prototype.destroy=function(){delete this._hash,delete this._buff,delete this._length},u.prototype._finish=function(t,e){var n,o,i,a=e;if(t[a>>2]|=128<<(a%4<<3),a>55)for(r(this._hash,t),a=0;a<16;a+=1)t[a]=0;n=(n=8*this._length).toString(16).match(/(.*?)(.{0,8})$/),o=parseInt(n[2],16),i=parseInt(n[1],16)||0,t[14]=o,t[15]=i,r(this._hash,t)},u.hash=function(t,e){return u.hashBinary(f(t),e)},u.hashBinary=function(t,e){var r=s(i(t));return e?h(r):r},u.ArrayBuffer=function(){this.reset()},u.ArrayBuffer.prototype.append=function(t){var e,n,i,a,s,f=(n=this._buff.buffer,i=t,a=!0,(s=new Uint8Array(n.byteLength+i.byteLength)).set(new Uint8Array(n)),s.set(new Uint8Array(i),n.byteLength),a?s:s.buffer),h=f.length;for(this._length+=t.byteLength,e=64;e<=h;e+=64)r(this._hash,o(f.subarray(e-64,e)));return this._buff=e-64<h?new Uint8Array(f.buffer.slice(e-64)):new Uint8Array(0),this},u.ArrayBuffer.prototype.end=function(t){var e,r,n=this._buff,o=n.length,i=[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0];for(e=0;e<o;e+=1)i[e>>2]|=n[e]<<(e%4<<3);return this._finish(i,o),r=s(this._hash),t&&(r=h(r)),this.reset(),r},u.ArrayBuffer.prototype.reset=function(){return this._buff=new Uint8Array(0),this._length=0,this._hash=[1732584193,-271733879,-1732584194,271733878],this},u.ArrayBuffer.prototype.getState=function(){var t,e=u.prototype.getState.call(this);return e.buff=(t=e.buff,String.fromCharCode.apply(null,new Uint8Array(t))),e},u.ArrayBuffer.prototype.setState=function(t){return t.buff=function(t,e){var r,n=t.length,o=new ArrayBuffer(n),i=new Uint8Array(o);for(r=0;r<n;r+=1)i[r]=t.charCodeAt(r);return e?i:o}(t.buff,!0),u.prototype.setState.call(this,t)},u.ArrayBuffer.prototype.destroy=u.prototype.destroy,u.ArrayBuffer.prototype._finish=u.prototype._finish,u.ArrayBuffer.hash=function(t,e){var n=s(function(t){var e,n,i,a,s,f,h=t.length,u=[1732584193,-271733879,-1732584194,271733878];for(e=64;e<=h;e+=64)r(u,o(t.subarray(e-64,e)));for(n=(t=e-64<h?t.subarray(e-64):new Uint8Array(0)).length,i=[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],e=0;e<n;e+=1)i[e>>2]|=t[e]<<(e%4<<3);if(i[e>>2]|=128<<(e%4<<3),e>55)for(r(u,i),e=0;e<16;e+=1)i[e]=0;return a=(a=8*h).toString(16).match(/(.*?)(.{0,8})$/),s=parseInt(a[2],16),f=parseInt(a[1],16)||0,i[14]=s,i[15]=f,r(u,i),u}(new Uint8Array(t)));return e?h(n):n},u})}function r(t,e,r){return{device_type:\"amazfit\",name:e,skin_bin:t,skin_size:\"100KB\",downloads:9999,thumbnails:[r]}}function n(t){e.init(!0),fetch(t.skin_bin).then(function(t){return t.arrayBuffer()}).then(function(r){t.skin_size=Math.round(r.byteLength/1024)+\"KB\";var n=SparkMD5.ArrayBuffer.hash(r);t.skin_bin+=\"?\"+n+\".wzf\",e.destroy(),HM_JsBridge.invoke(\"syncWatchSkin\",t,function(t){console.dir(t)})}).catch(function(t){e.destroy(),alert(t)})}})())";
            b.loadUrl("javascript:" + javascript);
        }
    }

    @DexIgnore
    @Override
    public void onFinishActivity() {

    }

    @DexIgnore
    @Override
    public void onShowMoreMenu(JSShareCreator.Content content, boolean b, boolean b1, boolean b2, boolean b3, String s) {

    }

    @DexIgnore
    @Override
    public void setMenuMoreVisible(boolean b, View.OnClickListener onClickListener) {

    }

    @DexIgnore
    @Override
    public void setMenuShareVisible(JSShareCreator.Content content) {

    }

    @DexIgnore
    @Override
    public void setTitleBarBackgroundColor(int i) {

    }

    @DexIgnore
    @Override
    public void setTitleFgColor(int i) {

    }

    @DexIgnore
    @Override
    public void setTitleText(String s) {

    }

    @DexIgnore
    @Override
    public void setTitleVisibility(boolean b) {

    }

    @DexIgnore
    @Override
    public void syncWatchSkin(String s) {

    }
}
