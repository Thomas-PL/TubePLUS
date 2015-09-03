// This code loads the IFrame Player API code asynchronously.
var tag = document.createElement('script');
tag.src = "https://www.youtube.com/iframe_api";
var firstScriptTag = document.getElementsByTagName('script')[0];
firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);


// This function creates an <iframe> (and YouTube player)
//    after the API code downloads.
var player;
var storedVolume;
var width, height;
var counter = 1;
var url;
function onYouTubeIframeAPIReady() {
    
    url = android_player.getUrl();
    width = android_player.getwidth(); 
    height = android_player.getheight();
    player = new YT.Player('youtube-player', {
        height: height,
        width: width,
        playerVars: {'color': 'red', 'controls': 2, 'enablejsapi': 1, 'modestbranding': 1, 'rel': 0, 'theme': 'light'},
        videoId: url,
        events: {
            'onReady': onPlayerReady,
            'onStateChange': onPlayerStateChange,
            'onPlaybackQualityChange': onQualityChange,
            'onError': onError
        }
    });
}

function onError(event) {
    android_player.log("ERROR IN PLAYER");
}
var ready = 0;
// The API will call this function when the video player is ready.
function onPlayerReady(event) {
    android_player.stopSpinner();
    ready = 1;
    $("#youtube-player").css("width",width+"px");
    $("#youtube-player").css("height",height+"px");
}

function setBufferedPercentage() {

    if (ready === 1) {
        android_player.log(player.getVideoLoadedFraction() * 100.0);
        player_controller.setBufferedPercentage(player.getVideoLoadedFraction());
    }
}

function onQualityChange(event) {
}

var seeking = 0;
var time = 0;
function onPlayerStateChange(event) {

    if (event.data === YT.PlayerState.PLAYING) {
        android_player.resetTPThread(Math.round(player.getCurrentTime()));
    }

    if (seeking === 1 && event.data === YT.PlayerState.PLAYING)
    {
        if (player.getPlaybackQuality() === player_controller.getPlayQuality())
        {
            player_controller.startTimeOut();
            android_player.log("seeking");

            seeking = 0;
        }
    }

    if (event.data === YT.PlayerState.PLAYING && counter === 1) {
        createJSONQualities(event);
        getDuration();
        setQuality(player_controller.getPlayQuality());
        player_controller.setStarted(true);
        counter++;
    }
}

function getDuration() {
    var qu = android_player.getUrl();
    var requestOptions = {
        id: qu,
        part: 'contentDetails'
    };
    var request = gapi.client.youtube.videos.list(requestOptions);

    request.execute(function(data) {
        var duration = data.items[0].contentDetails.duration;
        player_controller.setDuration(duration.substring(2));
    });

}


function createJSONQualities(event) {
    event.target.getAvailableQualityLevels().forEach(function(entry) {
        player_controller.addQuality(entry);
    });
}

function setQuality(quality)
{
    player.setPlaybackQuality(quality);
}

function changeQuality() {
    time = player.getCurrentTime();
    seeking = 1;
    setQuality(player_controller.getPlayQuality());
    android_player.setToast("Quality set to " + player_controller.getPlayQuality());
}

function setCurrentPosition() {
    var current = Math.round(player.getCurrentTime());
    player_controller.setCurrentPosition(current);
}

function stopVideo() {
    player.stopVideo();
}

function seekTo() {
    //als het toch zou gelukt zijn,moet er niet nog eens versprongen worden
    if (player.getCurrentTime() <= time)
        player.seekTo(time);
}

function pauseVideo() {
    player.pauseVideo();
}

function mute() {
    player.mute();
}

function unMute() {
    player.unMute();
}

function restoreVolume() {
    player.setVolume(storedVolume);
}

function storeVolume() {
    storedVolume = player.getVolume();
}

function setVolume(v) {
    player.setVolume(v);
}

function destroyBecauseOfTimeout() {
    android_player.destroy();
}