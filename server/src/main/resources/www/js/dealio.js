(function($, w) {
  $(function() {
    w.error = function(msg) {
      var e = $("#error");
      if(e.length === 0) {
         e = $('<div id="error"/>');
        $("#container").prepend(e);
      }
      e.append(['<div>',msg,'</div>'].join(''));
    };
  });
})(jQuery, window);