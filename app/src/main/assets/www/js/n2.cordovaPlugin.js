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
    $n2.cordovaPlugin = {
        echo: echo
        ,getConnectionInfo: getConnectionInfo
    };

})(cordova,nunaliit2);