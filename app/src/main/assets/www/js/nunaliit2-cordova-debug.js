/*
Copyright (c) 2010, Geomatics and Cartographic Research Centre, 
Carleton University, Canada
All rights reserved.

Released under New BSD License.
Details at:
   https://svn.gcrc.carleton.ca/nunaliit2/trunk/sdk/license.txt
*/

"use strict";
var nunaliit2CoreScript;
(function(){
var scriptLocation = null;
var pattern = new RegExp('(^|(.*?\/))nunaliit2-cordova-debug.js$');
var scripts = document.getElementsByTagName('script');
for( var loop=0; loop<scripts.length; ++loop ) {
	var src = scripts[loop].getAttribute('src');
	if (src) {
		var match = src.match(pattern);
		if( match ) {
			scriptLocation = match[1];
			break;
		}
	}
};
if( null === scriptLocation ) {
	alert('Unable to find library tag (nunaliit2-cordova-debug.js)');
};
if( typeof nunaliit2CoreScript === 'undefined' ){
	nunaliit2CoreScript = 'nunaliit2-cordova-debug.js';
	if( typeof window !== 'undefined' ){
		window.nunaliit2CoreScript = nunaliit2CoreScript;
	};
};
var jsfiles = [
'n2.cordovaPlugin.js'
];
var allScriptTags = new Array();
for( var i=0; i<jsfiles.length; ++i ) {
	allScriptTags.push('<script src="');
	allScriptTags.push(scriptLocation);
	allScriptTags.push(jsfiles[i]);
	allScriptTags.push('"></script>');
};
document.write(allScriptTags.join(''));
})();
