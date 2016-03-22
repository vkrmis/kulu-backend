class Kulu.Paginator
  constructor: (form) ->
    @form = form

  setupPerPageForm: ->
    $(@form.find('select#per-page')).change -> @form.submit()
