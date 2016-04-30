/*

jQuery Base64 plugin 0.0.3
Source: http://github.com/yckart/jquery.base64.js

Copyright 2016 Yannick Albert
License: http://opensource.org/licenses/MIT

*/

(function(e){function a(e,t,n,r,i,s){e=String(e);var o=0,u=0,a=e.length,f="",l=0;while(u<a){var c=e.charCodeAt(u);c=c<256?n[c]:-1;o=(o<<i)+c;l+=i;while(l>=s){l-=s;var h=o>>l;f+=r.charAt(h);o^=h<<l}++u}if(!t&&l>0)f+=r.charAt(o<<s-l);return f}var t="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/",n="",r=[256],i=[256],s=0;var o={encode:function(e){var t=e.replace(/[\u0080-\u07ff]/g,function(e){var t=e.charCodeAt(0);return String.fromCharCode(192|t>>6,128|t&63)}).replace(/[\u0800-\uffff]/g,function(e){var t=e.charCodeAt(0);return String.fromCharCode(224|t>>12,128|t>>6&63,128|t&63)});return t},decode:function(e){var t=e.replace(/[\u00e0-\u00ef][\u0080-\u00bf][\u0080-\u00bf]/g,function(e){var t=(e.charCodeAt(0)&15)<<12|(e.charCodeAt(1)&63)<<6|e.charCodeAt(2)&63;return String.fromCharCode(t)}).replace(/[\u00c0-\u00df][\u0080-\u00bf]/g,function(e){var t=(e.charCodeAt(0)&31)<<6|e.charCodeAt(1)&63;return String.fromCharCode(t)});return t}};while(s<256){var u=String.fromCharCode(s);n+=u;i[s]=s;r[s]=t.indexOf(u);++s}var f=e.base64=function(e,t,n){return t?f[e](t,n):e?null:this};f.btoa=f.encode=function(e,n){e=f.raw===false||f.utf8encode||n?o.encode(e):e;e=a(e,false,i,t,8,6);return e+"====".slice(e.length%4||4)};f.atob=f.decode=function(e,t){e=e.replace(/[^A-Za-z0-9\+\/\=]/g,"");e=String(e).split("=");var i=e.length;do{--i;e[i]=a(e[i],true,r,n,6,8)}while(i>0);e=e.join("");return f.raw===false||f.utf8decode||t?o.decode(e):e}})(jQuery)