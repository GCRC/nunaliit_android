;(function($n2){
    "use strict";

    var SERVICE = 'Nunaliit';

    if( typeof window.cordova === 'undefined' ) return;
    var cordova = window.cordova;

    // ==================================================================
    function echo(opts_){
        var opts = $n2.extend({
            msg: null
            ,onSuccess: function(msg){}
            ,onError: function(err){}
        },opts_);

        cordova.exec(
            // success
            function(msg){
                opts.onSuccess(msg);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'echo',
            // Arguments
            [opts.msg]
        );
    };

    // ==================================================================
    function getConnectionInfo(opts_){
        var opts = $n2.extend({
            onSuccess: function(info){}
            ,onError: function(err){}
        },opts_);
        cordova.exec(
            // success
            function(result){
                opts.onSuccess(result);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'getConnectionInfo',
            // Arguments
            []
        );
    };

    // ==================================================================
    function couchbaseGetDatabaseInfo(opts_){
        var opts = $n2.extend({
            onSuccess: function(info){}
            ,onError: function(err){}
        },opts_);

        cordova.exec(
            // success
            function(result){
                opts.onSuccess(result);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'couchbaseGetDatabaseInfo',
            // Arguments
            [
            ]
        );
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

        cordova.exec(
            // success
            function(result){
                opts.onSuccess(result);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'couchbaseGetDocumentRevision',
            // Arguments
            [
                opts.docId
            ]
        );
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

        cordova.exec(
            // success
            function(result){
                opts.onSuccess(result);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'couchbaseCreateDocument',
            // Arguments
            [
                opts.doc
            ]
        );
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

        cordova.exec(
            // success
            function(result){
                opts.onSuccess(result);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'couchbaseUpdateDocument',
            // Arguments
            [
                opts.doc
            ]
        );
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

        cordova.exec(
            // success
            function(result){
                opts.onSuccess(result);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'couchbaseDeleteDocument',
            // Arguments
            [
                opts.doc
            ]
        );
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

        cordova.exec(
            // success
            function(result){
                opts.onSuccess(result.doc);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'couchbaseGetDocument',
            // Arguments
            [
                opts.docId
            ]
        );
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

        cordova.exec(
            // success
            function(result){
                opts.onSuccess(result.docs);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'couchbaseGetDocuments',
            // Arguments
            [
                opts.docIds
            ]
        );
    };

    // ==================================================================
    function couchbaseGetAllDocumentIds(opts_){
        var opts = $n2.extend({
            onSuccess: function(result){}
            ,onError: function(err){}
        },opts_);

        cordova.exec(
            // success
            function(result){
                opts.onSuccess(result.ids);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'couchbaseGetAllDocumentIds',
            // Arguments
            [
            ]
        );
    };

    // ==================================================================
    function couchbaseGetAllDocuments(opts_){
        var opts = $n2.extend({
            onSuccess: function(result){}
            ,onError: function(err){}
        },opts_);

        cordova.exec(
            // success
            function(result){
                opts.onSuccess(result.docs);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'couchbaseGetAllDocuments',
            // Arguments
            [
            ]
        );
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

        cordova.exec(
            // success
            function(result){
                opts.onSuccess(result.rows);
            },
            // error
            function(err){
                opts.onError(err);
            },
            // service
            SERVICE,
            // action
            'couchbasePerformQuery',
            // Arguments
            [
                opts.designName
                ,opts.query
            ]
        );
    };

    function registerCallback(ecb, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            SERVICE,
            'registerCallback',
            [ecb]);
    }

    // ==================================================================
    $n2.cordovaPlugin = {
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
        ,registerCallback: registerCallback
    };

})(nunaliit2);