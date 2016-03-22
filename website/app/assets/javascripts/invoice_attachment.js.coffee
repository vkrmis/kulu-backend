class Kulu.InvoiceAttachment
  constructor: (@container, @url) ->

  fetch: () ->
    $.ajax(
      type: "POST"
      url: Routes.fetch_attachment_invoices_path()
      data: JSON.stringify({attachment_url: @url})
      contentType: "application/json"
    ).success (data) =>
      @container.attr('src', "data:application/pdf;base64," + data);
