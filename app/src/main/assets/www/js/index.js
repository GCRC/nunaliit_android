;(function($n2){
    var app = {
        // Application Constructor
        initialize: function() {
            this.bindEvents();
        },
        // Bind Event Listeners
        //
        // Bind any events that are required on startup. Common events are:
        // 'load', 'deviceready', 'offline', and 'online'.
        bindEvents: function() {
            document.addEventListener('deviceready', this.onDeviceReady, false);
        },
        // deviceready Event Handler
        //
        // The scope of 'this' is the event. In order to call the 'receivedEvent'
        // function, we must explicitly call 'app.receivedEvent(...);'
        onDeviceReady: function() {
            app.receivedEvent('deviceready');

            $n2.cordovaPlugin.echo({
                msg: '12345'
                ,onSuccess: function(msg){
                    if( '12345' === msg ){
                        console.log('echo success: '+msg);
                    } else {
                        console.log('echo error: Unexpected message ('+msg+')');
                    };
                }
                ,onError: function(err){
                    console.log('echo error: '+err);
                }
            });

            $n2.cordovaPlugin.getConnectionInfo({
                onSuccess: function(info){
                    console.log('connection name: '+info.name);
                }
            });
        },
        // Update DOM on a Received Event
        receivedEvent: function(id) {
            var parentElement = document.getElementById(id);
            var listeningElement = parentElement.querySelector('.listening');
            var receivedElement = parentElement.querySelector('.received');

            listeningElement.setAttribute('style', 'display:none;');
            receivedElement.setAttribute('style', 'display:block;');

            console.log('Received Event: ' + id);
        }
    };

    app.initialize();

})(nunaliit2);
