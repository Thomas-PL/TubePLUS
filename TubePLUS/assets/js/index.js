//var app = {
//    // Application Constructor
//    initialize: function() {
//        this.bindEvents();
//    },
//    // Bind Event Listeners
//    //
//    // Bind any events that are required on startup. Common events are:
//    // 'load', 'deviceready', 'offline', and 'online'.
//    bindEvents: function() {
//        document.addEventListener('deviceready', this.onDeviceReady, false);
//    },
//    // deviceready Event Handler
//    //
//    // The scope of 'this' is the event. In order to call the 'receivedEvent'
//    // function, we must explicity call 'app.receivedEvent(...);'
//    onDeviceReady: function() {
//        document.addEventListener("pause", app.onPause, false);
//        document.addEventListener("resume", app.onResume, false);
//        
//        //app.receivedEvent('deviceready');        
//
//    },
//    onResume: function() {
//        //app.receivedEvent("resume");
//    },
//    onPause: function() {
////        youtubePlugin.setTime();
//        //app.receivedEvent('pause');
//        //
//    }
////    // Update DOM on a Received Event
////    receivedEvent: function(id) {
////        var parentElement = document.getElementById(id);
////        var listeningElement = parentElement.querySelector('.listening');
////        var receivedElement = parentElement.querySelector('.received');
////        listeningElement.setAttribute('style', 'display:none;');
////        receivedElement.setAttribute('style', 'display:block;');
////
////        console.log('Received Event: ' + id);
////    }
//};
function tryReload() {
    Android.tryReload();
};

function checkConnection() {
    Android.checkNetworkConnection();
}
