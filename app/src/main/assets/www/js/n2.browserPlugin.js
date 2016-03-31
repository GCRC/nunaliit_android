;(function($n2){
    "use strict";

    var atlasDb = null;
    var atlasDesign = null;
    var couchServer = new $n2.couch.getServer({
        pathToServer: '/atlas/server/'
        ,onSuccess: function(server){
            atlasDb = server.getDb({
                dbUrl: '/atlas/db/'
            });
            atlasDesign = atlasDb.getDesignDoc({
                ddName: 'atlas'
            });

            // Do not load nunaliit until we are connected to server
            $n2.cordovaPlugin = $n2.browserPlugin;
        }
        ,onError: function(err){
            $n2.log('Unable to connect to CouchDb server: '+err );
        }
    });

    // ==================================================================
    function echo(opts_){
        var opts = $n2.extend({
            msg: null
            ,onSuccess: function(msg){}
            ,onError: function(err){}
        },opts_);

        window.setTimeout(function(){
            opts.onSuccess(opts.msg);
        },0);
    };

    // ==================================================================
    function getConnectionInfo(opts_){
        var opts = $n2.extend({
            onSuccess: function(info){}
            ,onError: function(err){}
        },opts_);

        window.setTimeout(function(){
            opts.onSuccess({
                name: 'demo'
                ,id: 'abcdef'
                ,url: 'http://localhost:8080'
                ,user: 'admin'
            });
        },0);
    };

    // ==================================================================
    function couchbaseGetDatabaseInfo(opts_){
        var opts = $n2.extend({
            onSuccess: function(info){}
            ,onError: function(err){}
        },opts_);

        window.setTimeout(function(){
            opts.onSuccess({
                'db_name': 'demo'
                ,'doc_count': 32
                ,'committed_update_seq': 233
            });
        },0);
    };

    // ==================================================================
    function couchbaseGetDocumentRevision(opts_){
        var opts = $n2.extend({
            docId: null
            ,onSuccess: function(info){}
            ,onError: function(err){}
        },opts_);

        if( typeof opts.docId !== 'string' ){
            throw 'cordovaPlugin.getCouchbaseDocumentRevision(): docId must be a string';
        };

        atlasDb.getDocumentRevision(opts);
    };

    // ==================================================================
    function couchbaseCreateDocument(opts_){
        var opts = $n2.extend({
            doc: null
            ,onSuccess: function(info){}
            ,onError: function(err){}
        },opts_);

        if( typeof opts.doc !== 'object' ){
            throw 'cordovaPlugin.couchbaseCreateDocument(): doc must be an object';
        };

        atlasDb.createDocument({
            data: opts.doc
            ,onSuccess: opts.onSuccess
            ,onError: opts.onError
        });
    };

    // ==================================================================
    function couchbaseUpdateDocument(opts_){
        var opts = $n2.extend({
            doc: null
            ,onSuccess: function(info){}
            ,onError: function(err){}
        },opts_);

        if( typeof opts.doc !== 'object' ){
            throw 'cordovaPlugin.couchbaseUpdateDocument(): doc must be an object';
        };

        atlasDb.updateDocument({
            data: opts.doc
            ,onSuccess: opts.onSuccess
            ,onError: opts.onError
        });
    };

    // ==================================================================
    function couchbaseDeleteDocument(opts_){
        var opts = $n2.extend({
            doc: null
            ,onSuccess: function(info){}
            ,onError: function(err){}
        },opts_);

        if( typeof opts.doc !== 'object' ){
            throw 'cordovaPlugin.couchbaseDeleteDocument(): doc must be an object';
        };

        atlasDb.deleteDocument({
            data: opts.doc
            ,onSuccess: opts.onSuccess
            ,onError: opts.onError
        });
    };

    // ==================================================================
    function couchbaseGetDocument(opts_){
        var opts = $n2.extend({
            docId: null
            ,onSuccess: function(doc){}
            ,onError: function(err){}
        },opts_);

        if( typeof opts.docId !== 'string' ){
            throw 'cordovaPlugin.couchbaseGetDocument(): docId must be a string';
        };

        atlasDb.getDocument({
            docId: opts.docId
            ,onSuccess: opts.onSuccess
            ,onError: opts.onError
        });
    };

    // ==================================================================
    function couchbaseGetDocuments(opts_){
        var opts = $n2.extend({
            docIds: null
            ,onSuccess: function(doc){}
            ,onError: function(err){}
        },opts_);

        if( !$n2.isArray(opts.docIds) ){
            throw 'cordovaPlugin.couchbaseGetDocuments(): docIds must be an array';
        };

        atlasDb.getDocuments(opts);
    };

    // ==================================================================
    function couchbaseGetAllDocumentIds(opts_){
        var opts = $n2.extend({
            onSuccess: function(result){}
            ,onError: function(err){}
        },opts_);

        atlasDb.listAllDocuments(opts);
    };

    // ==================================================================
    function couchbaseGetAllDocuments(opts_){
        var opts = $n2.extend({
            onSuccess: function(result){}
            ,onError: function(err){}
        },opts_);

        atlasDb.getAllDocuments(opts);
    };

    // ==================================================================
    function couchbasePerformQuery(opts_){
        var opts = $n2.extend({
		    designName: null
		    ,query: null
            ,onSuccess: function(result){}
            ,onError: function(err){}
        },opts_);

        if( typeof opts.designName !== 'string' ){
            throw 'cordovaPlugin.couchbasePerformQuery(): designName must be a string';
        };

        if( typeof opts.query !== 'object' ){
            throw 'cordovaPlugin.couchbasePerformQuery(): query must be an object';
        };

        atlasDesign.queryView({
            viewName: opts.query.viewName
            ,startkey: opts.query.startkey
            ,endkey: opts.query.endkey
            ,keys: opts.query.keys
            ,group: opts.query.group
            ,include_docs: opts.query.include_docs
            ,limit: opts.query.limit
            ,onlyRows: true
            ,rawResponse: false
            ,reduce: opts.query.reduce
            ,onSuccess: opts.onSuccess
            ,onError: opts.onError
        });
    };

    // ==================================================================
    $n2.browserPlugin = {
        echo: echo
        ,getConnectionInfo: getConnectionInfo
        ,couchbaseGetDatabaseInfo: couchbaseGetDatabaseInfo
        ,couchbaseGetDocumentRevision: couchbaseGetDocumentRevision
        ,couchbaseCreateDocument: couchbaseCreateDocument
        ,couchbaseUpdateDocument: couchbaseUpdateDocument
        ,couchbaseDeleteDocument: couchbaseDeleteDocument
        ,couchbaseGetDocument: couchbaseGetDocument
        ,couchbaseGetDocuments: couchbaseGetDocuments
        ,couchbaseGetAllDocumentIds: couchbaseGetAllDocumentIds
        ,couchbaseGetAllDocuments: couchbaseGetAllDocuments
        ,couchbasePerformQuery: couchbasePerformQuery
    };

})(nunaliit2);