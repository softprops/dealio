(function($, n){
  $(function(){
    var renderVenue = function(v) {
      return [
        '<li><a href="/checkins/', v.name, '/', v.id, '">', v.name, '</a></li>'
      ].join('');
    }
    if(n.geolocation) {
      n.geolocation.getCurrentPosition(function(p){
        var lat = p.coords.latitude, lon = p.coords.longitude;
        $.get('/fs/venues', { ll: [lat, lon].join(',') }, function(r) {
          var v = $("#venues");
          if(r.meta.code === 400) {
            error(
              "Are you sure you are you say you are? Foursquare doen't thinks so"
            );
          } else {
            var vens = r.response.venues,
                markup = $.map(vens, renderVenue).join('');
            if(markup.length === 0) { markup = "<li/>You don't appear to manage any<li>"; }
            v.append(markup);
          }
        })
      }, function(e) {
        switch(e.code) {
        case e.PERMISSION_DENIED: alert("maybe next time");
        case e.POSITION_UNAVILABLE: alert("the the h*** are you?")
        case e.TIMEOUT: alert("timed out getting your loc")
        default: alert("unknown error"); break;
        }
      });
    } else {
      alert("Use a better browser. This one doesn't know where you are at.")
    }
  });
})(jQuery, navigator);