NProgress.configure
  speed: 700
  minimum: 0.7
  ease: 'ease'
  showSpinner: false

startSpinner = -> NProgress.inc()
stopSpinner = -> NProgress.done()
removeSpinner = -> NProgress.remove()

$(document).on "page:fetch", startSpinner
$(document).on "page:change", stopSpinner
$(document).on "page:restore", removeSpinner
$(document).on "page:receive", stopSpinner
$(document).on "ajax:before", startSpinner
$(document).on "ajax:complete", stopSpinner

$ ->
  Kulu.flash = ->
    flashCallback = =>
      $(".flash-message").fadeOut 700, =>
        $("#nav").removeClass('flash-background notice error')

    $(".flash-background").bind 'click', flashCallback

    setTimeout flashCallback, 2500

  Kulu.flash()
