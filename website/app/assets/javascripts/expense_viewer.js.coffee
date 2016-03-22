class Kulu.ExpenseViewer
  constructor: (@container) ->
    @container.find(".expense-image-container").css('display', 'block').zoom({
      callback: ->
        $(this).colorbox({
          href: this.src,
          onLoad: ->
            $('#cboxClose').remove()
        })
    })
