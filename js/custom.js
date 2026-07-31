// Scrolls to the selected menu item on the page
document.addEventListener('DOMContentLoaded', function() {
  var links = document.querySelectorAll('a[href*="#"]:not([href="#"])');
  links.forEach(function(link) {
    link.addEventListener('click', function(event) {
      if (link.pathname.replace(/^\//, '') === location.pathname.replace(/^\//, '') &&
          link.hostname === location.hostname) {
        var target = document.getElementById(link.hash.slice(1)) ||
                     document.getElementsByName(link.hash.slice(1))[0];
        if (target) {
          event.preventDefault();
          target.scrollIntoView({ behavior: 'smooth' });
        }
      }
    });
  });
});
