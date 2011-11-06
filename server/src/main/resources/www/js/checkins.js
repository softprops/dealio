// todo
(function($, w){
  $(function(){
      var s = window.location.href.split('/'), v = s.pop();
      while(v.length === 0) { v = s.pop(); } // '' if there was a trailing slash
      $.get('/api/venues/' + v, function(cins){
          console.log(cins);
      });
  });
})(jQuery, window);