class Kulu.Utils
  clickableRows: (invoiceRows) ->
    _.each invoiceRows, (invoiceRow) ->
      $(invoiceRow).click ->
        Turbolinks.visit($(this).data('invoice-url'))
