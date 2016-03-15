;(function(cordova){
    "use strict";

    var SERVICE = 'Nunaliit';

    // Nunaliit
    if( typeof window.nunaliit2 === 'undefined' ){
        window.nunaliit2 = {};
    };
    var $n2 = window.nunaliit2;

    // ==================================================================
    function echo(msg, onSuccess, onError){
        cordova.exec(
            // success
            function(msg){
                if( typeof onSuccess === 'function' ){
                    onSuccess(msg);
                };
            },
            // error
            function(err){
                if( typeof onError === 'function' ){
                    onError(err);
                };
            },
            // service
            SERVICE,
            // action
            'echo',
            // Arguments
            [msg]
        );
    };

    // ==================================================================
    function getConnectionInfo(onSuccess, onError){
        cordova.exec(
            // success
            function(result){
                if( typeof onSuccess === 'function' ){
                    onSuccess(result);
                };
            },
            // error
            function(err){
                if( typeof onError === 'function' ){
                    onError(err);
                };
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

})(cordova);