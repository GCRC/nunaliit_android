;(function(cordova,$n2){
    "use strict";

    var SERVICE = 'Nunaliit';

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
                opts.onSuccess(result);
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
    $n2.cordovaPlugin = {
        echo: echo
        ,getConnectionInfo: getConnectionInfo
        ,couchbaseGetDocumentRevision: couchbaseGetDocumentRevision
        ,couchbaseCreateDocument: couchbaseCreateDocument
        ,couchbaseUpdateDocument: couchbaseUpdateDocument
        ,couchbaseDeleteDocument: couchbaseDeleteDocument
        ,couchbaseGetDocument: couchbaseGetDocument
    };

})(cordova,nunaliit2);