document.addEventListener("deviceready", function() {
    navigator.camera.getPicture(function(success) {
        console.log('success', success);
    }, function(error) {
        console.log('error', error);
    }, {});
}, false);



