class Kulu.InvoiceEdit
  constructor: (@formContainer) ->
    @updateButton = @formContainer.find(".invoice-edit-submit")
    @invoiceID = @formContainer.find('input[name="invoice[:id]"]').val()
    @dateGroup = @formContainer.find('.invoice-details-date-group')
    @deleteButton = @formContainer.find('.invoice-edit-delete')

    @updateButton.click (e) =>
      e.preventDefault()
      @formSubmit()

    @deleteButton.click (e) =>
      unless window.confirm("Are you sure?")
        e.stopPropagation()

  isDateValid: () =>
    dateInput = @dateGroup.find(".invoice-details-date")
    moment($(dateInput).val(), "DD-MM-YYYY").isValid()

  formSubmit: () =>
    unless @isDateValid()
      @dateGroup.addClass('has-error')
      return false

    data = {
      id: @formContainer.find('input[name="invoice[:id]"]').val()
      name: @formContainer.find('input[name="invoice[:name]"]').val()
      currency: @formContainer.find('select[name="invoice[:currency]"]').val()
      amount: Number(@formContainer.find('input[name="invoice[:amount]"]').val())
      date: @formContainer.find('input[name="invoice[:date]"]').val()
      expense_type: @formContainer.find('input[name="invoice[:expense_type]"]:checked').val()
      status: @formContainer.find('select[name="invoice[:status]"]').val()
      conflict: @formContainer.find('input[name="invoice[:conflict]"]:checked').val()
      remarks: @formContainer.find('input[name="invoice[:remarks]"]').val()
      category_id: @formContainer.find('select[name="invoice[:category_id]"]').val()
    }

    $.ajax(
      type: "PUT"
      url: Routes.invoice_path({id: @invoiceID})
      data: JSON.stringify({invoice: data})
      contentType: "application/json"
    ).success =>
      Turbolinks.visit(@formContainer.data('invoices-url'));
      Kulu.flash()
