var query, nextPageToken, prevPageToken;
var quality = 0;

$(document).ready(function(){
    $('#prev-button').css('visibility', "hidden");
    $('#next-button').css('visibility', "hidden");
});
// After the API loads, call a function to enable the search box.
function handleAPILoaded() {  
    //fill homepage with recommended videos
    var request = gapi.client.youtube.search.list({
        part: 'snippet',
        home: 'true',
        type: 'video',
        videoEmbeddable: 'true',
        videoSyndicated: 'true'
    });
    execute(request);
}

function zoek(qu, pageToken) {
    query = qu;
    var requestOptions = {
        q: qu,
        part: 'snippet',
        type: 'video',
        videoEmbeddable: 'true',
        videoSyndicated: 'true'
    };
    if (pageToken) {
        requestOptions.pageToken = pageToken;
    }
    var request = gapi.client.youtube.search.list(requestOptions);

    execute(request);
}

var url;

//execute the request and get the videos from youtube
function execute(request)
{
    getQuality();
    request.execute(function(data) {
        $('#search-container').empty();
        $("#search-container").removeAttr("class");

        //$('#search-container').html(JSON.stringify(data));
        //var data = JSON.stringify(response.result);
        var aantal = data.pageInfo.resultsPerPage;
        var totaal = data.pageInfo.totalResults;
        if (totaal === 0) {
            $("#empty_search").removeAttr("hidden");
        } else {
            $("#empty_search").attr("hidden", true);
        }
        for (var i = 0; i < aantal; i++)
        {
            switch (quality) {
                case 0:
                    //lQ
                    $('#search-container').append(
                            $("<tr>").append(
                            $("<td>").append(
                            $("<table>").attr("class", "filmpje").append(
                            $("<tr>").append(
                            $("<td>").attr("class", "title").html(data.items[i].snippet.title)
                            )
                            ).append(
                            $("<tr>").append(
                            $("<td>").append(
                            $("<img>").attr("src", data.items[i].snippet.thumbnails.default.url)
                            .attr("alt", data.items[i].snippet.title)
                            .attr("id", data.items[i].id.videoId)
                            .attr("class", "thumbnail")
                            )
                            )
                            )
                            )
                            )
                            );
                    break;

                case 1:
                    //MQ
                    $('#search-container').append(
                            $("<tr>").append(
                            $("<td>").append(
                            $("<table>").attr("class", "filmpje").append(
                            $("<tr>").append(
                            $("<td>").attr("class", "title").html(data.items[i].snippet.title)
                            )
                            ).append(
                            $("<tr>").append(
                            $("<td>").append(
                            $("<img>").attr("src", data.items[i].snippet.thumbnails.medium.url)
                            .attr("alt", data.items[i].snippet.title)
                            .attr("id", data.items[i].id.videoId)
                            .attr("class", "thumbnail")
                            )
                            )
                            )
                            )
                            )
                            );
                    break;

                case 2:
                    //HQ
                    $('#search-container').append(
                            $("<tr>").append(
                            $("<td>").append(
                            $("<table>").attr("class", "filmpje").append(
                            $("<tr>").append(
                            $("<td>").attr("class", "title").html(data.items[i].snippet.title)
                            )
                            ).append(
                            $("<tr>").append(
                            $("<td>").append(
                            $("<img>").attr("src", data.items[i].snippet.thumbnails.high.url)
                            .attr("alt", data.items[i].snippet.title)
                            .attr("id", data.items[i].id.videoId)
                            .attr("class", "thumbnail")
                            )
                            )
                            )
                            )
                            )
                            );
                    break;
            }
        }
        

        nextPageToken = data.nextPageToken;
        var nextVis = nextPageToken ? 'visible' : 'hidden';
        $('#next-button').css('visibility', nextVis);
        if (nextPageToken) {
            Android.setHasNextPage(true);
        }
        else {
            Android.setHasNextPage(false);
        }

        prevPageToken = data.prevPageToken;
        var prevVis = prevPageToken ? 'visible' : 'hidden';
        $('#prev-button').css('visibility', prevVis);
        if (prevPageToken) {
            Android.setHasPreviousPage(true);
        }
        else {
            Android.setHasPreviousPage(false);
        }
        $("#reload").attr("hidden", true);
        $("#connection_error").attr("hidden", true);
        Android.stopSpinner();
    });
}

function showReload() {
    $("#reload").removeAttr("hidden");
}

function showConnectionError() {
    $("#connection_error").removeAttr("hidden");
}

$(document).on("click", "img", function() {
    //start webview met video op android
    var title = $("#" + this.id).attr("alt");
    Android.triggerActivity(this.id, title);
});

// Retrieve the next page of videos in the playlist.
function nextPage() {
    $("#search-container").attr("class", "hide-left");
    zoek(query, nextPageToken);
}

// Retrieve the previous page of videos in the playlist.
function previousPage() {
    zoek(query, prevPageToken);
}

function getQuality() {
    quality = network_controller.getNetworkQuality();
}