/*
Copyright (c) 2016, Geomatics and Cartographic Research Centre, Carleton
University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.
 - Neither the name of the Geomatics and Cartographic Research Centre,
   Carleton University nor the names of its contributors may be used to
   endorse or promote products derived from this software without specific
   prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

;(function($,$n2){
"use strict";

// Localization
var _loc = function(str,args){ return $n2.loc(str,'nunaliit2-cordova',args); };

function httpJsonError(XMLHttpRequest, defaultStr) {
	// Need JSON
	if( !JSON || typeof(JSON.parse) !== 'function' ) {
		return $n2.error.fromString(defaultStr);
	};

	// Need a response text
	var text = XMLHttpRequest.responseText;
	if( !text ) return $n2.error.fromString(defaultStr);

	// Parse
	var error = JSON.parse(text);
	if( !error ) return $n2.error.fromString(defaultStr);

	var err = undefined;
	if( typeof error.reason === 'string' ) {
		err = $n2.error.fromString(error.reason);
	} else {
		err = $n2.error.fromString(defaultStr);
	};

	if( error.error ){
		var condition = 'couchDb_' + error.error;
		err.setCondition(condition);
	};

	return err;
};

// Fix name: no spaces, all lowercase
function fixUserName(userName) {
	return userName.toLowerCase().replace(' ','');
};

// =============================================
// Session
// =============================================

/*
 * Accepts two CouchDb session context objects and compares
 * them. If they are equivalent, returns true. Otherwise, false.
 */
function compareSessionContexts(s1, s2){
	// This takes care of same object and both objects null
	if( s1 === s2 ) {
		return true;
	};

	// Check that one of them is null or undefined
	if( !s1 ){
		return false;
	};
	if( !s2 ){
		return false;
	};

	if( s1.name !== s2.name ){
		return false;
	};

	// Compare roles
	var s1Roles = {};
	if( s1.roles ){
		for(var i=0,e=s1.roles.length; i<e; ++i){
			var role = s1.roles[i];
			s1Roles[role] = true;
		};
	};
	var s2Roles = {};
	if( s2.roles ){
		for(var i=0,e=s2.roles.length; i<e; ++i){
			var role = s2.roles[i];
			s2Roles[role] = true;
		};
	};
	for(var role in s1Roles){
		if( !s2Roles[role] ){
			return false;
		};
	};
	for(var role in s2Roles){
		if( !s1Roles[role] ){
			return false;
		};
	};

	return true;
};

var Session = $n2.Class({

	server: null

	,pathToSession: null

	,changedContextListeners: null

	,lastSessionContext: null

	,initialize: function(server_, sessionInfo_){

		this.server = server_;

		this.changedContextListeners = [];
		this.lastSessionContext = null;

		if( sessionInfo_ ){
    		if( sessionInfo_.ok ) {
    			var context = sessionInfo_.userCtx;
    			this.changeContext(context);
    		};
		};
	}

	,getUrl: function() {
		return this.server.getSessionUrl();
	}

	,getContext: function() {
		return this.lastSessionContext;
	}

	,addChangedContextListener: function(listener){
		if( typeof(listener) === 'function' ) {
			this.changedContextListeners.push(listener);

			if( this.lastSessionContext ) {
				listener(this.lastSessionContext);
			};
		};
	}

	,changeContext: function(context) {
		this.lastSessionContext = context;
		if( this.lastSessionContext ) {
			for(var i=0,e=this.changedContextListeners.length; i<e; ++i) {
				var listener = this.changedContextListeners[i];
				try {
					listener(this.lastSessionContext);
				} catch(e) {};
			};
		};
	}

	,refreshContext: function(opts_) {
		var opts = $.extend({
				onSuccess: function(context) {}
				,onError: $n2.reportErrorForced
			}
			,opts_
		);

		var _this = this;
		var sessionUrl = this.getUrl();

		var data = {};

		$.ajax({
			url: sessionUrl
			,type: 'get'
			,async: true
			,dataType: 'json'
			,data: data
			,success: function(res) {
				if( res.ok ) {
					var context = res.userCtx;
					_this.changeContext(context);
					opts.onSuccess(context);
				} else {
					opts.onError('Malformed context reported');
				};
			}
			,error: function(XMLHttpRequest, textStatus, errorThrown) {
				var errStr = httpJsonError(XMLHttpRequest, textStatus);
				opts.onError('Error obtaining context: '+errStr);
			}
		});
	}

	,login: function(opts_) {
		var opts = $.extend({
				name: null
				,password: null
				,onSuccess: function(context) {}
				,onError: $n2.reportErrorForced
			}
			,opts_
		);

		var _this = this;
		var sessionUrl = this.getUrl();

		// Fix name: no spaces, all lowercase
		if( opts.name ) {
			var name = fixUserName(opts.name);
		} else {
			opts.onError('A name must be supplied when logging in');
			return;
		};

		$.ajax({
	    	url: sessionUrl
    		,type: 'post'
    		,async: true
	    	,data: {
	    		name: name
	    		,password: opts.password
	    	}
	    	,contentType: 'application/x-www-form-urlencoded'
    		,dataType: 'json'
    		,success: function(info) {
	    		if( info && info.ok ) {
	    			_this.refreshContext({
	    				onSuccess: opts.onSuccess
	    				,onError: opts.onError
	    			});
	    		} else {
    				opts.onError('Unknown error during log in');
	    		};
	    	}
    		,error: function(XMLHttpRequest, textStatus, errorThrown) {
				var errStr = httpJsonError(XMLHttpRequest, textStatus);
				opts.onError('Error during log in: '+errStr);
	    	}
		});
	}

	,logout: function(opts_) {
		var opts = $.extend({
				onSuccess: function(context) {}
				,onError: $n2.reportErrorForced
			}
			,opts_
		);

		var _this = this;
		var sessionUrl = this.getUrl();

		$.ajax({
	    	url: sessionUrl
    		,type: 'DELETE'
    		,async: true
    		,dataType: 'json'
    		,success: function(info) {
	    		if( info && info.ok ) {
	    			_this.refreshContext({
	    				onSuccess: opts.onSuccess
	    				,onError: opts.onError
	    			});
	    		} else {
    				opts.onError('Unknown error during log out');
	    		};
	    	}
    		,error: function(XMLHttpRequest, textStatus, errorThrown) {
				var errStr = httpJsonError(XMLHttpRequest, textStatus);
				opts.onError('Error during log out: '+errStr);
	    	}
		});
	}
});

// =============================================
// Design Document
// =============================================

var designDoc = $n2.Class({
	ddName: null

	,initialize: function(opts_) {
		var opts = $n2.extend({
			ddName: null
			,db: null
		},opts_);

		this.ddUrl = opts.ddUrl;
		this.ddName = opts.ddName;
		this.db = opts.db;
	}

	,getQueryUrl: function(opts_){
		var opts = $.extend(true, {
				viewName: null
				,listName: null
			}
			,opts_
		);

		throw 'Couchbase.DesignDoc.getQueryUrl() is not implemented';
	}

	,queryView: function(options_) {
		var opts = $.extend(true, {
				viewName: null
				,listName: null
				,viewUrl: null
				,startkey: null
				,endkey: null
				,keys: null
				,group: null
				,include_docs: null
				,limit: null
				,onlyRows: true
				,rawResponse: false
				,reduce: false
				,onSuccess: function(rows){}
				,onError: function(errorMsg){ $n2.reportErrorForced(errorMsg); }
			}
			,options_
		);

		if( opts.viewUrl ){
    		throw 'Couchbase.DesignDoc.queryView() does not support option: viewUrl';
		};
		if( opts.listName ){
    		throw 'Couchbase.DesignDoc.queryView() does not support option: listName';
		};
		if( opts.rawResponse ){
    		throw 'Couchbase.DesignDoc.queryView() does not support option: rawResponse';
		};

		var req = {};

		if( typeof opts.viewName !== 'string' ){
    		throw 'Couchbase.DesignDoc.queryView() option viewName must be a string';
		};
		req.viewName = opts.viewName;

		if( opts.startkey ){
		    req.startkey = opts.startkey;
		};

		if( opts.endkey ){
		    req.endkey = opts.endkey;
		};

		if( opts.keys ){
            if( !$n2.isArray(opts.keys) ){
                throw 'Couchbase.DesignDoc.queryView() if option keys is specified, it must be an array';
            };
            req.keys = opts.keys;
		};

		if( opts.include_docs ){
    		req.include_docs = true;
		};

		if( null !== opts.limit && typeof opts.limit !== 'undefined' ){
		    // Limit is specified
		    if( typeof opts.limit !== 'number' ){
                throw 'Couchbase.DesignDoc.queryView() if option limit is specified, it must be a number';
		    };
		};

		if( opts.reduce ){
    		req.reduce = true;
		};

		$n2.cordovaPlugin.couchbasePerformQuery({
		    designName: this.ddName
		    ,query: req
		    ,onSuccess: function(result) {
		        opts.onSuccess(result.rows);
		    }
		    ,onError: opts.onError
		});
	}
});

// =============================================
// Change Notifier
// =============================================

var ChangeNotifier = $n2.Class({

	options: null

	,db: null

	,listeners: null

	,lastSequence: null

	,currentRequest: null

	,currentWait: null

	,onError: function(err) { $n2.log(err); }

	,initialize: function(db, opts_) {
		this.options = $n2.extend({
			doNotReset: false
			,include_docs: false
			,pollInterval: 5000
			,longPoll: false
			,timeout: 20000
			,style: 'all_docs'
			,listeners: null
			,onSuccess: function(notifier){}
		},opts_);

		this.db = db;
		this.listeners = [];
		if( this.options.listeners ) {
			for(var i=0,e=this.options.listeners.length; i<e; ++i){
				this.listeners.push( this.options.listeners[i] );
			};
			delete this.options.listeners;
		};

		var onSuccessFn = this.options.onSuccess;
		delete this.options.onSuccess;

		var _this = this;

		if( this.options.doNotReset ) {
			finishInitialization();
		} else {
			this.resetLastSequence({
				onSuccess: finishInitialization
				,onError: finishInitialization
			});
		};

		function finishInitialization() {
			_this.reschedule();
			_this.requestChanges();

			onSuccessFn(_this);
		};
	}

	,addListener: function(listener) {
		if( typeof(listener) === 'function' ) {
			this.listeners.push(listener);

			this.requestChanges();
		};
	}

	/*
	 * This function does not report any changes. Instead, it
	 * updates the last sequence number to the current one. This
	 * means that the next request for changes will report only
	 * changes that have happened since 'now'.
	 */
	,resetLastSequence: function(opt_) {

		var opt = $n2.extend({
			onSuccess: function(){}
			,onError: function(err){}
		},opt_);

        this.lastSequence = 0;
        opt.onSuccess();
	}

	,getLastSequence: function(){
		return this.lastSequence;
	}

	,_reportChanges: function(changes) {

		if( changes.last_seq ) {
			this.lastSequence = changes.last_seq;
		};

		if( changes.results && changes.results.length > 0 ) {
			for(var i=0,e=this.listeners.length; i<e; ++i) {
				this.listeners[i](changes);
			};
		};
	}

	/**
		Request the server for changes.
	 */
	,requestChanges: function() {

		if( !this.listeners
		 || this.listeners.length < 1 ) {
			// Nothing to do
			return;
		};
		if( this.currentRequest ) {
			// A request already in progress
			return;
		};
		if( this.currentWait ) {
			// Already scheduled
			return;
		};

		// In cordova, do not do anything
		return;
	}

	/**
		Reschedule the next request for changes
	 */
	,reschedule: function() {
	}
});

//=============================================
// Database Callbacks
//=============================================

var DatabaseCallbacks = $n2.Class({

	onCreatedCallbacks: null

	,onUpdatedCallbacks: null

	,onDeletedCallbacks: null

	,initialize: function(){
		this.onCreatedCallbacks = [];
		this.onUpdatedCallbacks = [];
		this.onDeletedCallbacks = [];
	}

	,addOnCreatedCallback: function(f){
		if( typeof(f) === 'function' ) {
			this.onCreatedCallbacks.push(f);
		}
	}

	,addOnUpdatedCallback: function(f){
		if( typeof(f) === 'function' ) {
			this.onUpdatedCallbacks.push(f);
		}
	}

	,addOnDeletedCallback: function(f){
		if( typeof(f) === 'function' ) {
			this.onDeletedCallbacks.push(f);
		}
	}

	,_reportOnCreated: function(docInfo){
		for(var i=0,e=this.onCreatedCallbacks.length; i<e; ++i){
			var f = this.onCreatedCallbacks[i];
			f(docInfo);
		};
	}

	,_reportOnUpdated: function(docInfo){
		for(var i=0,e=this.onCreatedCallbacks.length; i<e; ++i){
			var f = this.onUpdatedCallbacks[i];
			f(docInfo);
		};
	}

	,_reportOnDeleted: function(docInfo){
		for(var i=0,e=this.onDeletedCallbacks.length; i<e; ++i){
			var f = this.onUpdatedCallbacks[i];
			f(docInfo);
		};
	}
});

// =============================================
// Database
// =============================================

var Database = $n2.Class({

	dbName: null

	,server: null

	,callbacks: null

	,initialize: function(opts_, server_) {
		var opts = $n2.extend({
			dbName: null
		},opts_);

		this.server = server_;

		this.dbName = opts.dbName;

		this.callbacks = new DatabaseCallbacks();
	}

	,getUrl: function(){
		return null;
	}

	,getDesignDoc: function(opts_) {
		var ddOpts = $.extend({
				ddUrl: null
				,ddName: null
			}
			,opts_
		);

		if( ddOpts.ddUrl ) {
			throw 'Couchbase.Database.getDesignDoc() option ddUrl is not supported';
		};

		ddOpts.db = this;

		return new designDoc(ddOpts);
	}

	,getChangeNotifier: function(opt_) {
		var opt = $n2.extend({
			onSuccess: function(notifier){}
		},opt_);

		var changeNotifier = new ChangeNotifier(
			this
			,{
				onSuccess: opt.onSuccess
			}
		);

		return changeNotifier;
	}

	,getChanges: function(opt_) {
		var opt = $n2.extend({
			since: null
			,limit: null
			,descending: false
			,include_docs: false
			,onSuccess: function(changes){}
			,onError: function(msg){ $n2.reportErrorForced(msg); }
		},opt_);


		throw 'Couchbase.Database.getChanges() not implemented';

		var req = {
			feed: 'normal'
		};

		if( opt.since ) {
			req.since = opt.since;
		};

		if( opt.limit ) {
			req.limit = opt.limit;
		};

		if( opt.descending ) {
			req.descending = opt.descending;
		};

		if( opt.include_docs ) {
			req.include_docs = opt.include_docs;
		};

		var changeUrl = this.dbUrl + '_changes';

		$.ajax({
	    	url: changeUrl
	    	,type: 'GET'
	    	,async: true
	    	,data: req
	    	,dataType: 'json'
	    	,success: opt.onSuccess
	    	,error: function(XMLHttpRequest, textStatus, errorThrown) {
				var errStr = httpJsonError(XMLHttpRequest, textStatus);
				opts.onError('Error obtaining database changes: '+errStr);
	    	}
		});
	}

	,getDocumentUrl: function(doc) {

		throw 'Couchbase.Database.getDocumentUrl() not implemented';

		if( typeof(doc) === 'string' ) {
			var docId = doc;
		} else {
			docId = doc._id;
		};

		return this.dbUrl + docId;
	}

	,getAttachmentUrl: function(doc,attName) {

		throw 'Couchbase.Database.getAttachmentUrl() not implemented';

		var docUrl = this.getDocumentUrl(doc);
		var url = docUrl + '/' + encodeURIComponent(attName);

		return url;
	}

	,getDocumentRevision: function(opts_) {
		var opts = $.extend({
				docId: null
				,onSuccess: function(rev){}
				,onError: function(msg){ $n2.reportErrorForced(msg); }
			}
			,opts_
		);

		if( !opts.docId ) {
			opts.onError('No docId set. Can not retrieve document information');
			return;
		};

		$n2.cordovaPlugin.couchbaseGetDocumentRevision({
		    docId: opts.docId
		    ,onSuccess: function(result) {
		        opts.onSuccess(result.rev);
		    }
		    ,onError: opts.onError
		});
	}

	,getDocumentRevisions: function(opts_) {
		var opts = $.extend({
				docIds: null
				,onSuccess: function(info){}
				,onError: function(msg){ $n2.reportErrorForced(msg); }
			}
			,opts_
		);

		if( !opts.docIds ) {
			opts.onError('No docIds set. Can not retrieve document revisions');
			return;
		};
		if( !$n2.isArray(opts.docIds) ) {
			opts.onError('docIds must ba an array. Can not retrieve document revisions');
			return;
		};

		throw 'Couchbase.Database.getDocumentRevisions() not implemented';

		var data = {
			keys: opts.docIds
		};

	    $.ajax({
	    	url: this.dbUrl + '_all_docs?include_docs=false'
	    	,type: 'POST'
	    	,async: true
	    	,data: JSON.stringify(data)
	    	,contentType: 'application/json'
	    	,dataType: 'json'
	    	,success: function(res) {
	    		if( res.rows ) {
	    			var info = {};
    				for(var i=0,e=res.rows.length; i<e; ++i){
    					var row = res.rows[i];
    					if( row.id && row.value && row.value.rev ){
    						if( !row.value.deleted ) {
    							info[row.id] = row.value.rev;
    						};
    					};
    				};
	    			opts.onSuccess(info);
	    		} else {
					opts.onError('Malformed document revisions');
	    		};
	    	}
	    	,error: function(XMLHttpRequest, textStatus, errorThrown) {
				var errStr = httpJsonError(XMLHttpRequest, textStatus);
				opts.onError('Error obtaining document revision for '+opts.docId+': '+errStr);
	    	}
	    });
	}

	,createDocument: function(options_) {
		var opts = $.extend(true, {
				data: {}
				,onSuccess: function(docInfo){}
				,onError: function(errorMsg){ $n2.reportErrorForced(errorMsg); }
			}
			,options_
		);

		var _s = this;

		if( typeof opts.data !== 'object' ){
    		throw 'Couchbase.Database.createDocument(): data must be an object';
		};

		$n2.cordovaPlugin.couchbaseCreateDocument({
		    doc: opts.data
		    ,onSuccess: function(docInfo){
		    	_s.callbacks._reportOnCreated(docInfo);
		        opts.onSuccess(docInfo);
		    }
		    ,onError: opts.onError
		});
	}

	,updateDocument: function(options_) {
		var opts = $.extend(true, {
				data: null
				,onSuccess: function(docInfo){}
				,onError: function(errorMsg){ $n2.reportErrorForced(errorMsg); }
			}
			,options_
		);

		var _s = this;

		if( typeof opts.data !== 'object' ){
    		throw 'Couchbase.Database.updateDocument(): data must be an object';
		};

		$n2.cordovaPlugin.couchbaseUpdateDocument({
		    doc: opts.data
		    ,onSuccess: function(docInfo){
	    		_s.callbacks._reportOnUpdated(docInfo);
	    		opts.onSuccess(docInfo);
		    }
		    ,onError: opts.onError
		});
	}

	,deleteDocument: function(options_) {
		var opts = $.extend(true, {
				data: null
				,onSuccess: function(docInfo){}
				,onError: function(errorMsg){ $n2.reportErrorForced(errorMsg); }
			}
			,options_
		);

		var _s = this;

		if( typeof opts.data !== 'object' ){
    		throw 'Couchbase.Database.deleteDocument(): data must be an object';
		};

		$n2.cordovaPlugin.couchbaseDeleteDocument({
		    doc: opts.data
		    ,onSuccess: function(docInfo){
	    		_s.callbacks._reportOnDeleted(docInfo);
	    		opts.onSuccess(docInfo);
		    }
		    ,onError: opts.onError
		});
	}

	/**
		Inserts and/or updates a number of documents
	 	@name bulkDocuments
	 	@function
	 	@memberOf nunaliit2.couch.Database
	 	@param {Array} documents Array of documents
	 	@param {Object} options_ Options associated with operations

	 */
	,bulkDocuments: function(documents, options_) {
		var opts = $.extend(true, {
				onSuccess: function(docInfos){}
				,onError: function(errorMsg){ $n2.reportErrorForced(errorMsg); }
			}
			,options_
		);

		throw 'Couchbase.Database.bulkDocuments() not implemented';
	}

	,getDocument: function(options_) {
		var opts = $.extend(true, {
				docId: null
				,rev: null
				,revs_info: false
				,revisions: false
				,conflicts: false
				,deleted_conflicts: false
				,onSuccess: function(doc){}
				,onError: function(errorMsg){ $n2.reportErrorForced(errorMsg); }
			}
			,options_
		);

		var _s = this;

		if( typeof opts.docId !== 'string' ){
    		throw 'Couchbase.Database.getDocument(): docId must be a string';
		};

		var data = {};

		if( opts.rev ) {
			data.rev = opts.rev;
    		throw 'Couchbase.Database.getDocument() option rev not supported';
		};

		if( opts.revs_info ) {
			data.revs_info = 'true';
    		throw 'Couchbase.Database.getDocument() option rev_info not supported';
		};

		if( opts.revisions ) {
			data.revs = 'true';
    		throw 'Couchbase.Database.getDocument() option revisions not supported';
		};

		if( opts.conflicts ) {
			data.conflicts = 'true';
    		throw 'Couchbase.Database.getDocument() option conflicts not supported';
		};

		if( opts.deleted_conflicts ) {
			data.deleted_conflicts = 'true';
    		throw 'Couchbase.Database.getDocument() option deleted_conflicts not supported';
		};

		$n2.cordovaPlugin.couchbaseGetDocument({
		    docId: opts.docId
		    ,onSuccess: function(result){
	    		opts.onSuccess(result.doc);
		    }
		    ,onError: opts.onError
		});
	}

	,getDocuments: function(opts_) {
		var opts = $.extend(true, {
				docIds: null
				,onSuccess: function(docs){}
				,onError: function(errorMsg){ $n2.log(errorMsg); }
			}
			,opts_
		);

		var _s = this;

		if( !$n2.isArray(opts.docIds) ){
    		throw 'Couchbase.Database.getDocuments(): docIds must be an array';
		};

		$n2.cordovaPlugin.couchbaseGetDocuments({
		    docIds: opts.docIds
		    ,onSuccess: function(result){
	    		opts.onSuccess(result.docs);
		    }
		    ,onError: opts.onError
		});
	}

	,listAllDocuments: function(opts_) {
		var opts = $.extend(true, {
				onSuccess: function(docIds){}
				,onError: function(errorMsg){ $n2.reportErrorForced(errorMsg); }
			}
			,opts_
		);

		$n2.cordovaPlugin.couchbaseGetAllDocumentIds({
		    onSuccess: function(result){
	    		opts.onSuccess(result.ids);
		    }
		    ,onError: opts.onError
		});
	}

	,getAllDocuments: function(opts_) {
		var opts = $.extend(true, {
				onSuccess: function(docs){}
				,onError: function(errorMsg){ $n2.reportErrorForced(errorMsg); }
			}
			,opts_
		);

		$n2.cordovaPlugin.couchbaseGetAllDocuments({
		    onSuccess: function(result){
	    		opts.onSuccess(result.docs);
		    }
		    ,onError: opts.onError
		});
	}

	,getInfo: function(opts_) {
		var opts = $.extend(true, {
				onSuccess: function(dbInfo){}
				,onError: function(errorMsg){ $n2.reportErrorForced(errorMsg); }
			}
			,opts_
		);

		$n2.cordovaPlugin.couchbaseGetDatabaseInfo({
		    onSuccess: function(info){
	    		opts.onSuccess(info);
		    }
		    ,onError: opts.onError
		});

	}

	,queryTemporaryView: function(opts_){
		var opts = $n2.extend({
			map: null
			,reduce: null
			,onSuccess: function(rows){}
			,onError: $n2.reportErrorForced
		},opts_);

		throw 'Couchbase.Database.queryTemporaryView() not implemented';
	}
});

// =============================================
// Server
// =============================================

var Server = $n2.Class({

	isInitialized: false

	,initListeners: null

	,initialize: function(options_, initListeners_){
		var opts = $n2.extend({
				onSuccess: function(server){}
				,onError: function(err){}
			}
			,options_
		);

		this.isInitialized = true;
		this.initListeners = initListeners_;
		if( !this.initListeners ) {
			this.initListeners = [];
		};

		// Call back all listeners
		for(var i=0,e=this.initListeners; i<e; ++i) {
			var listener = this.initListeners[i];
			try { listener(); } catch(e){}
		};
		this.initListeners = [];

		opts.onSuccess(this);
	}

	,getPathToServer: function() {
		return null;
	}

	,getVersion: function() { return '1.0'; }

	,getReplicateUrl: function() {
		return null;
	}

	,getActiveTasksUrl: function() {
		return null;
	}

	,getSessionUrl: function() {
		return null;
	}

	,getUniqueId: function(options_) {
		var opts = $.extend({
				onSuccess: function(uuid){}
				,onError: function(errorMsg){ $n2.reportErrorForced(errorMsg); }
			}
			,options_
		);

		throw 'Couchbase.Server.getUniqueId() not implemented';
	}

	,listDatabases: function(options_) {
		var opts = $.extend({
				onSuccess: function(dbNameArray){}
				,onError: function(errorMsg){ $n2.reportErrorForced(errorMsg); }
			}
			,options_
		);

		throw 'Couchbase.Server.listDatabases() not implemented';
	}

	,getUserDb: function() {
		throw 'Couchbase.Server.getUserDb() not implemented';
	}

	,getSession: function(sessionInfo) {
		throw 'Couchbase.Server.getSession() not implemented';
	}

	,getDb: function(opts) {
		return new Database(opts, this);
	}

	,createDb: function(opts_) {
		var opts = $n2.extend({
			dbName: null
			,onSuccess: function(db){}
			,onError: $n2.reportErrorForced
		},opts_);

		throw 'Couchbase.Server.createDb() not implemented';
	}

	,deleteDb: function(opts_) {
		var opts = $n2.extend({
			dbName: null
			,onSuccess: function(){}
			,onError: $n2.reportErrorForced
		},opts_);

		throw 'Couchbase.Server.deleteDb() not implemented';
	}

	,replicate: function(opts_){
		var opts = $n2.extend({
			source: null
			,target: null
			,filter: null
			,docIds: null
			,continuous: false
			,onSuccess: function(db){}
			,onError: $n2.reportErrorForced
		},opts_);

		throw 'Couchbase.Server.replicate() not implemented';
	}

	,addInitializedListener: function(listener) {
		if( typeof(listener) === 'function' ) {
			if( this.isInitialized ) {
				try { listener(); } catch(e) {}
			} else {
				this.initListeners.push(listener);
			};
		};
	}

});

//=============================================
// Utilities
//=============================================

function addAttachmentToDocument(opts_){
	var opts = $n2.extend({
		doc: null // Document to add attachment
		,data: null // The binary data in the attachment
		,attachmentName: null // name of attachment
		,contentType: 'application/binary'
	},opts_);

	if( !opts.doc || !opts.data || !opts.attachmentName ) {
		return 'Invalid parameters';
	};

	if( typeof($n2.Base64) == 'undefined' ) {
		return 'Base64 not included';
	};

	if( !opts.doc._attachments ) {
		opts.doc._attachments = {};
	};

	opts.doc._attachments[opts.attachmentName] = {};
	opts.doc._attachments[opts.attachmentName].content_type = opts.contentType;
	opts.doc._attachments[opts.attachmentName].data = $n2.Base64.encode(opts.data);

	return null;
};

//=============================================
// Couch Default
//=============================================

$n2.cordovaCouchbase = $.extend({},{

	getServer: function(opt_) {
		return new Server(opt_);
	}

	/* Following deals with "default" server */
	,DefaultServer: null

	,defaultInitializeListeners: []

	,getPathToServer: function() {
		return $n2.couch.DefaultServer.getPathToServer();
	}

	,addInitializedListener: function(listener) {
		if( $n2.couch.DefaultServer ) {
			$n2.couch.DefaultServer.addInitializedListener(listener);
		} else {
			if( typeof(listener) === 'function' ) {
				$n2.couch.defaultInitializeListeners.push(listener);
			};
		};
	}

	,initialize: function(opts_) {
		$n2.couch.DefaultServer = new Server(opts_, $n2.couch.defaultInitializeListeners);
	}

	,getVersion: function() {
		return $n2.couch.DefaultServer.getVersion();
	}

	,getSession: function() {
		return $n2.couch.DefaultServer.getSession();
	}

	,getUserDb: function() {
		return $n2.couch.DefaultServer.getUserDb();
	}

	,getReplicateUrl: function() {
		return $n2.couch.DefaultServer.getReplicateUrl();
	}

	,getActiveTasksUrl: function() {
		return $n2.couch.DefaultServer.getActiveTasksUrl();
	}

	,getDb: function(opts) {
		return $n2.couch.DefaultServer.getDb(opts);
	}

	,getUniqueId: function(options_) {
		$n2.couch.DefaultServer.getUniqueId();
	}

	,addAttachmentToDocument: addAttachmentToDocument

	,compareSessionContexts: compareSessionContexts
});


})(jQuery,nunaliit2);