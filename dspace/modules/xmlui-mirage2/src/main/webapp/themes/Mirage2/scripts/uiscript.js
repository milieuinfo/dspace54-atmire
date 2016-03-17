$(window).scroll(function(){
    var $win = $(window);
    $('#subheadercontent').css('top', 44 -$win.scrollTop());
});