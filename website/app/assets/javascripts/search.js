Kulu.search = function (searchParams) {
  var facets = [
    {label: 'query'},
    {label: 'merchant name', category: 'general'},
    {label: 'type', category: 'general'},
    {label: 'status', category: 'general'},
    {label: 'conflict', category: 'general'},
    {label: 'spender', category: 'general'},
    {label: 'category', category: 'general'},
    {label: 'expense date (from)', category: 'expense date'},
    {label: 'expense date (to)', category: 'expense date'},
    {label: 'submission date (from)', category: 'submission date'},
    {label: 'submission date (to)', category: 'submission date'},
    {label: 'currency', category: 'amount'},
    {label: 'amount (>)', category: 'amount'},
    {label: 'amount (<)', category: 'amount'},
    {label: 'amount (=)', category: 'amount'}
  ];

  var visualSearchToApiMappings = {
    "merchant name": "name",
    "expense date (from)": "from_date",
    "expense date (to)": "to_date",
    "submission date (from)": "from_submission_date",
    "submission date (to)": "to_submission_date",
    "amount (>)": "min_amount",
    "amount (<)": "max_amount",
    "amount (=)": "amount",
    "type": "expense_type",
    "category": "category_name",
    "spender": "user_name",
    "currency": "currency",
    "query": "q",
    "conflict": "conflict",
    "status": "status"
  };

  var displayDatepicker = function (callback) {
    var input = $('.search_facet.is_editing input.search_facet_input');

    var removeDatepicker = function () {
      input.datepicker("destroy");
    };

    var setVisualSearch = function (date) {
      removeDatepicker();
      callback([date]);
      $("ul.VS-interface:visible li.ui-menu-item a:first").click()
    };

    input.datepicker({
      dateFormat: 'yy-mm-dd',
      onSelect: setVisualSearch,
      onClose: removeDatepicker
    });

    input.datepicker('show');
  };

  var visualSearch = VS.init({
    container: $('.visual_search'),
    query: '',
    minLength  : 1,
    remainder   : 'query',
    placeholder : "Search for your expenses...",
    unquotable : [
      'query',
      'conflict',
      'type',
      'status',
      'currency',
      'submission date (to)',
      'submission date (from)',
      'expense date (to)',
      'expense date (from)'
    ],
    callbacks: {
      search: function (query, searchCollection) {
        var $query = $('.search_query');
        var count  = searchCollection.size();
        $query.stop().animate({opacity : 1}, {duration: 300, queue: false});
        $query.html('<span class="raquo">&raquo;</span>' + ' (' + count + ' filter' + (count == 1 ? '' : 's') + ' applied)');
      },

      facetMatches: function (callback) {
        var currentFacets = visualSearch.searchQuery.pluck("category");
        var newFacets = facets.filter(function(el) {
          var e = el['label'] || el;
          return currentFacets.indexOf(e) < 0;
        });

        callback(newFacets, {preserveOrder: true});
      },

      valueMatches: function (facet, searchTerm, callback) {
        switch (facet) {
          case 'submission date (to)':
            setTimeout(function () {
              displayDatepicker(callback);
            }, 0);
            break;
          case 'submission date (from)':
            setTimeout(function () {
              displayDatepicker(callback);
            }, 0);
            break;
          case 'expense date (to)':
            setTimeout(function () {
              displayDatepicker(callback);
            }, 0);
            break;
          case 'expense date (from)':
            setTimeout(function () {
              displayDatepicker(callback);
            }, 0);
            break;
          case 'conflict':
            callback(['false', 'true'], {preserveOrder: true});
            break;
          case 'type':
            callback(['Company', 'Reimbursement']);
            break;
          case 'status':
            callback(['Submitted', 'Extracted', 'Recorded', 'Reimbursed/Deducted', 'Reviewed'], {preserveOrder: true});
            break;
          case 'currency':
            callback(['INR', 'USD', 'EUR', 'SGD', 'IDR', 'GBP', 'CAD'], {preserveOrder: true});
            break;
        }
      }
    }
  });

  var paramsToVisualSearch = function (params) {
    var invertedMappings = _.invert(visualSearchToApiMappings);

    _.each(params, function (v, k) {
      if (_.has(invertedMappings, k)) {
        visualSearch.searchBox.addFacet(invertedMappings[k], v + "");
      }
    });
  };

  paramsToVisualSearch(searchParams);

  var formContainer = $('#search-form');
  var searchButton  = $("#expenses-search");

  searchButton.click(function (e) {
    e.preventDefault();
    return formSubmit();
  });

  var formSubmit = function () {
    _.each(visualSearch.searchQuery.facets(), function (o) {
      var key = _.keys(o)[0];
      var value = _.values(o)[0];
      var formKeyName = visualSearchToApiMappings[key];
      input = 'input[name="' + (formKeyName ? formKeyName : key) + '"]';
      formContainer.find(input).val(value).prop("disabled", false);
    });

    return formContainer.submit();
  };
};
